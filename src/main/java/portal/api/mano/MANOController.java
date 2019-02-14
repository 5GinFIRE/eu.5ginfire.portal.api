/**
 * Copyright 2017 University of Patras 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and limitations under the License.
 */

package portal.api.mano;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import OSM4NBIClient.OSM4Client;
import portal.api.bus.BusController;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.DeploymentDescriptorStatus;
import portal.api.model.ExperimentMetadata;
import portal.api.model.ExperimentOnBoardDescriptor;
import portal.api.model.MANOprovider;
import portal.api.model.NSCreateInstanceRequestPayload;
import portal.api.model.NSInstantiateInstanceRequestPayload;
import portal.api.model.OnBoardingStatus;
import portal.api.model.VxFMetadata;
import portal.api.model.VxFOnBoardedDescriptor;
import portal.api.osm.client.OSMClient;
import portal.api.repo.PortalRepository;
import urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.Nsd;
import urn.ietf.params.xml.ns.yang.nfvo.vnfd.rev150910.vnfd.catalog.Vnfd;

/**
 * @author ctranoris
 *
 */
public class MANOController {

	/** This is also binded by Bean */
	private static PortalRepository portalRepositoryRef;

	/** */
	private static final transient Log logger = LogFactory.getLog(MANOController.class.getName());

	public MANOController() {

	}

	private static String HTTP_SCHEME = "https:";

	public static void setHTTPSCHEME(String url) {
		logger.info("setHTTPSCHEME url = " + url);
		if (url.contains("localhost")) {
			HTTP_SCHEME = "http:";
		}
		// HTTP_SCHEME = url + ":";
	}

	/**
	 * onBoard a VNF to MANO Provider, as described by this descriptor
	 * 
	 * @param vxfobds
	 * @throws Exception
	 */
	public void onBoardVxFToMANOProvider(VxFOnBoardedDescriptor vxfobd) throws Exception {

		// PortalRepository portalRepositoryRef = new PortalRepository();

		vxfobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		// This is the Deployment ID for the portal
		vxfobd.setDeployId(UUID.randomUUID().toString());
		VxFMetadata vxf = vxfobd.getVxf();
		if (vxf == null) {
			vxf = (VxFMetadata) portalRepositoryRef.getProductByID(vxfobd.getVxfid());
		}
		// Set MANO Provider VxF ID
		vxfobd.setVxfMANOProviderID(vxf.getName());
		// Set onBoarding Date
		vxfobd.setLastOnboarding(new Date());

		VxFOnBoardedDescriptor vxfobds = portalRepositoryRef.updateVxFOnBoardedDescriptor(vxfobd);
		if (vxfobds == null) {
			throw new Exception("Cannot load VxFOnBoardedDescriptor");
		}

		logger.info(portalRepositoryRef.toString());

		String pLocation = vxf.getPackageLocation();
		logger.info("VxF Package Location: " + pLocation);

		if (!pLocation.contains("http")) {
			pLocation = HTTP_SCHEME + pLocation;
		}
//		if (!pLocation.contains("http")) {
//			pLocation = "http:" + pLocation;
//			pLocation = pLocation.replace("\\", "/");
//		}					
		logger.info("PROPER VxF Package Location: " + pLocation);

		if (vxfobds.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM TWO")) {
			OSMClient.getInstance(vxfobds.getObMANOprovider()).createOnBoardVNFDPackage(pLocation,
					vxfobds.getDeployId());
			// run in a thread the GET polling for a VNF onboarding status
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(() -> {
				try {
					checkVxFStatus(vxfobds);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
		if (vxfobds.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FOUR")) {
			OSM4Client osm4Client = new OSM4Client(vxfobds.getObMANOprovider().getApiEndpoint(),
					vxfobds.getObMANOprovider().getUsername(), vxfobds.getObMANOprovider().getPassword(), "admin");
			ResponseEntity<String> response = null;
			response = osm4Client.createVNFDPackage();
			if (response == null || response.getStatusCode().is4xxClientError()
					|| response.getStatusCode().is5xxServerError()) {
				logger.error("VNFD Package Creation failed.");
				// Set status
				vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				// Set the reason of the failure
				vxfobds.setFeedbackMessage(response.getBody().toString());
				// Uncertify if it failed OnBoarding.
				vxfobds.getVxf().setCertified(false);
				VxFOnBoardedDescriptor vxfobds_final = portalRepositoryRef.updateVxFOnBoardedDescriptor(vxfobds);
				BusController.getInstance().onBoardVxFFailed(vxfobds);
				return;				
			}
			else
			{
				JSONObject obj = new JSONObject(response.getBody());
				String vnfd_id = obj.getString("id");
				logger.info(response.getStatusCode()+" replied. The new VNFD Package id is :" + vnfd_id);
				
				response = osm4Client.uploadVNFDPackageContent(vnfd_id, pLocation);
				if (response == null || response.getStatusCode().is4xxClientError()
						|| response.getStatusCode().is5xxServerError()) {
					logger.error("Upload of VNFD Package Content failed. Deleting VNFD Package.");
					// Delete the package from the OSM
					osm4Client.deleteVNFDPackage(vnfd_id);
					// Set status
					vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
					// Set the reason of the failure
					vxfobds.setFeedbackMessage(response.getBody().toString());
					// Uncertify if it failed OnBoarding.
					vxfobds.getVxf().setCertified(false);
					VxFOnBoardedDescriptor vxfobds_final = portalRepositoryRef.updateVxFOnBoardedDescriptor(vxfobds);
					BusController.getInstance().onBoardVxFFailed(vxfobds);
					return;
				}

				vxfobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
				vxfobds.setFeedbackMessage("OnBoarding Succeeded");
				// We select by design not to Certify upon OnBoarding but only on final version
				// is determined.
				// vxfobds.getVxf().setCertified(true);
				// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
				vxfobds.setDeployId(vnfd_id);
				// What should be the vxf Name. Something like cirros_vnfd.
				vxfobds.setVxfMANOProviderID(vxf.getName());
				// Set Onboarding date
				vxfobds.setLastOnboarding(new Date());
				// Save the changes to vxfobds
				VxFOnBoardedDescriptor vxfobds_final = portalRepositoryRef.updateVxFOnBoardedDescriptor(vxfobds);
				BusController.getInstance().onBoardVxFSucceded(vxfobds);
				
			}
			
		}

	}

	// Catch an VxF Onboarding Message
	// NOT USED
//	private void onCatchBoardVxFToMANOProvider(VxFOnBoardedDescriptor vxfobds) throws Exception {
//
//		CamelContext tempcontext = new DefaultCamelContext();
//		MANOController mcontroller = this;
//		try {
//			RouteBuilder rb = new RouteBuilder() {
//				@Override
//				public void configure() throws Exception {
//					from("seda:vxf.create?multipleConsumers=true")
//							.log("Will OnBoard VNF package")
//							.setBody().constant(vxfobds)
//							.bean(mcontroller, "onBoardVxFToMANOProvider");
//				}
//			};
//			tempcontext.addRoutes(rb);
//			tempcontext.start();
//			Thread.sleep(30000);
//		} finally {
//			tempcontext.stop();
//		}
//
//	}

	public static void checkAndDeployExperimentToMANOProvider() {
		logger.info("This will trigger the check and Deploy Experiments");
		// Check the database for a new deployment in the next minutes
		// If there is a deployment to be made and the status is Scheduled
		List<DeploymentDescriptor> DeploymentDescriptorsToRun = portalRepositoryRef.getDeploymentsToInstantiate();
		// Foreach deployment
		for (DeploymentDescriptor d : DeploymentDescriptorsToRun) {
			// Launch the deployment
			BusController.getInstance().deployExperiment(d);
		}
	}

	public static void checkAndTerminateExperimentToMANOProvider() {
		logger.info("This will trigger the check and Terminate Deployments");
		// Check the database for a deployment to be completed in the next minutes
		// If there is a deployment to be made and the status is Scheduled
		List<DeploymentDescriptor> DeploymentDescriptorsToComplete = portalRepositoryRef.getDeploymentsToBeCompleted();
		// Foreach deployment
		for (DeploymentDescriptor deployment_descriptor_tmp : DeploymentDescriptorsToComplete) {
			// Terminate the deployment
			deployment_descriptor_tmp.setStatus(DeploymentDescriptorStatus.TERMINATING);
			deployment_descriptor_tmp.setConstituentVnfrIps("N/A");
			DeploymentDescriptor changed_deployment_descriptor = portalRepositoryRef
					.updateDeploymentDescriptor(deployment_descriptor_tmp);
			BusController.getInstance().completeExperiment(changed_deployment_descriptor);
		}
	}

	public static void checkAndUpdateRunningDeploymentDescriptors() {
		logger.info("Update Deployment Descriptors");
		List<DeploymentDescriptor> runningDeploymentDescriptors = portalRepositoryRef
				.getRunningAndCompletedDeployments();
		OSM4Client osm4Client = null;
		// For each deployment get the status info and the IPs
		for (int i = 0; i < runningDeploymentDescriptors.size(); i++) {
			DeploymentDescriptor deployment_tmp = portalRepositoryRef
					.getDeploymentByID(runningDeploymentDescriptors.get(i).getId());
			try {
				// Get the MANO Provider for each deployment
				MANOprovider sm = portalRepositoryRef.getMANOproviderByID(deployment_tmp.getExperimentFullDetails()
						.getExperimentOnBoardDescriptors().get(0).getObMANOprovider().getId());
				if (sm.getSupportedMANOplatform().getName().equals("OSM FOUR")) {
					if (osm4Client == null || !osm4Client.getMANOApiEndpoint().equals(sm.getApiEndpoint())) {
						osm4Client = new OSM4Client(sm.getApiEndpoint(), sm.getUsername(), sm.getPassword(), "admin");
					}
					JSONObject ns_instance_info = osm4Client
							.getNSInstanceInfo(runningDeploymentDescriptors.get(i).getInstanceId());
					if (ns_instance_info != null) {
						try {
							logger.info(ns_instance_info.toString());
							deployment_tmp.setOperationalStatus(ns_instance_info.getString("operational-status"));
							deployment_tmp.setConfigStatus(ns_instance_info.getString("config-status"));
							deployment_tmp.setDetailedStatus(ns_instance_info.getString("detailed-status")
									.replaceAll("\\n", " ").replaceAll("\'", "'").replaceAll("\\\\", ""));
							// Depending on the current OSM status, change the portal status.
							if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.INSTANTIATING
									&& deployment_tmp.getOperationalStatus().toLowerCase().equals("running")
									&& deployment_tmp.getConfigStatus().toLowerCase().equals("configured")
									&& deployment_tmp.getDetailedStatus().toLowerCase().equals("done")) {
								deployment_tmp.setStatus(DeploymentDescriptorStatus.RUNNING);
								BusController.getInstance().deploymentInstantiationSucceded(deployment_tmp);
								deployment_tmp.setConstituentVnfrIps("");
								for (int j = 0; j < ns_instance_info.getJSONArray("constituent-vnfr-ref")
										.length(); j++) {
									if (j > 0) {
										deployment_tmp
												.setConstituentVnfrIps(deployment_tmp.getConstituentVnfrIps() + ", ");
									}
									JSONObject vnf_instance_info = osm4Client.getVNFInstanceInfo(
											ns_instance_info.getJSONArray("constituent-vnfr-ref").get(j).toString());
									if (vnf_instance_info != null) {
										try {
											deployment_tmp.setConstituentVnfrIps(deployment_tmp.getConstituentVnfrIps()
													+ vnf_instance_info.getString("ip-address"));
										} catch (JSONException e) {
											logger.error(e.getMessage());
										}
									}
								}
							}
							// deployment_tmp.getStatus() == DeploymentDescriptorStatus.TERMINATING &&
							if (deployment_tmp.getOperationalStatus().toLowerCase().equals("terminated")
									&& deployment_tmp.getConfigStatus().toLowerCase().equals("terminating")
									&& deployment_tmp.getDetailedStatus().toLowerCase().equals("done")) {
								deployment_tmp.setStatus(DeploymentDescriptorStatus.COMPLETED);
								deployment_tmp.setConstituentVnfrIps("N/A");
								BusController.getInstance().deploymentTerminationSucceded(deployment_tmp);
							}
							// if(deployment_tmp.getStatus() != DeploymentDescriptorStatus.FAILED &&
							// deployment_tmp.getOperationalStatus().equals("failed"))
							if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.INSTANTIATING
									&& deployment_tmp.getOperationalStatus().equals("failed")) {
								deployment_tmp.setStatus(DeploymentDescriptorStatus.FAILED);
								deployment_tmp.setConstituentVnfrIps("N/A");
								BusController.getInstance().deploymentInstantiationFailed(deployment_tmp);
							}
							if (deployment_tmp.getStatus() == DeploymentDescriptorStatus.TERMINATING
									&& deployment_tmp.getOperationalStatus().equals("failed")) {
								deployment_tmp.setStatus(DeploymentDescriptorStatus.TERMINATION_FAILED);
								BusController.getInstance().deploymentTerminationFailed(deployment_tmp);
							}
							deployment_tmp = portalRepositoryRef.updateDeploymentDescriptor(deployment_tmp);
						} catch (JSONException e) {
							logger.error(e.getMessage());
						}
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		checkAndDeployExperimentToMANOProvider();
		checkAndTerminateExperimentToMANOProvider();
	}

	public void onBoardNSDToMANOProvider(ExperimentOnBoardDescriptor uexpobd) throws Exception {

		uexpobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		// This is the Deployment ID for the portal
		uexpobd.setDeployId(UUID.randomUUID().toString());
		ExperimentMetadata em = uexpobd.getExperiment();
		if (em == null) {
			em = (ExperimentMetadata) portalRepositoryRef.getProductByID(uexpobd.getExperimentid());
		}

		/**
		 * The following is not OK. When we submit to OSMClient the createOnBoardPackage
		 * we just get a response something like response = {"output":
		 * {"transaction-id": "b2718ef9-4391-4a9e-97ad-826593d5d332"}} which does not
		 * provide any information. The OSM RIFTIO API says that we could get
		 * information about onboarding (create or update) jobs see
		 * https://open.riftio.com/documentation/riftware/4.4/a/api/orchestration/pkt-mgmt/rw-pkg-mgmt-download-jobs.htm
		 * with /api/operational/download-jobs, but this does not return pending jobs.
		 * So the only solution is to ask again OSM if something is installed or not, so
		 * for now the client (the portal ) must check via the
		 * getVxFOnBoardedDescriptorByIdCheckMANOProvider giving the VNF ID in OSM. OSM
		 * uses the ID of the yaml description Thus we asume that the vxf name can be
		 * equal to the VNF ID in the portal, and we use it for now as the OSM ID. Later
		 * in future, either OSM API provide more usefull response or we extract info
		 * from the VNFD package
		 * 
		 */

		uexpobd.setVxfMANOProviderID(em.getName()); // Possible Error. This probably needs to be
													// setExperimentMANOProviderID(em.getName())

		uexpobd.setLastOnboarding(new Date());

		ExperimentOnBoardDescriptor uexpobds = portalRepositoryRef.updateExperimentOnBoardDescriptor(uexpobd);
		if (uexpobds == null) {
			throw new Exception("Cannot load NSDOnBoardedDescriptor");
		}

		String pLocation = em.getPackageLocation();
		logger.info("NSD Package Location: " + pLocation);
		if (!pLocation.contains("http")) {
			pLocation = HTTP_SCHEME + pLocation;
		}
//		if (!pLocation.contains("http")) {
//			pLocation = "http:" + pLocation;
//			pLocation = pLocation.replace("\\", "/");
//		}				

		// Here we need to get a better solution for the OSM version names.
		if (uexpobds.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM TWO")) {
			OSMClient.getInstance(uexpobds.getObMANOprovider()).createOnBoardNSDPackage(pLocation,
					uexpobds.getDeployId());
			// run in a thread the GET polling for a NSD onboarding status
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(() -> {
				try {
					checkNSDStatus(uexpobds);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
		if (uexpobds.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FOUR")) {
			OSM4Client osm4Client = new OSM4Client(uexpobds.getObMANOprovider().getApiEndpoint(),
					uexpobds.getObMANOprovider().getUsername(), uexpobds.getObMANOprovider().getPassword(), "admin");
			ResponseEntity<String> response = null;
			response = osm4Client.createNSDPackage();
			if (response == null || response.getStatusCode().is4xxClientError()
					|| response.getStatusCode().is5xxServerError()) {
				logger.error("Upload of NSD Package Content failed. Deleting NSD Package.");
				uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				// Set the reason of the failure
				uexpobds.setFeedbackMessage(response.getBody().toString());
				// Set Valid to false if it fails OnBoarding
				uexpobds.getExperiment().setValid(false);
				ExperimentOnBoardDescriptor uexpobd_final = portalRepositoryRef
						.updateExperimentOnBoardDescriptor(uexpobds);
				BusController.getInstance().onBoardNSDFailed(uexpobds);
				return;				
			}
			else
			{
				JSONObject obj = new JSONObject(response.getBody());
				String nsd_id = obj.getString("id");
				logger.info(response.getStatusCode()+" replied. The new NSD Package id is :" + nsd_id);
				logger.error("Uploading NSD Archive from URL " + pLocation);
				logger.error("************************");
				response = osm4Client.uploadNSDPackageContent(nsd_id, pLocation);
				if (response == null || response.getStatusCode().is4xxClientError()
						|| response.getStatusCode().is5xxServerError()) {
					logger.error("Upload of NSD Package Content failed. Deleting NSD Package.");
					osm4Client.deleteNSDPackage(nsd_id);
					uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
					uexpobds.setFeedbackMessage(response.getBody().toString());
					// Set Valid to false if it fails OnBoarding
					uexpobds.getExperiment().setValid(false);
					ExperimentOnBoardDescriptor uexpobd_final = portalRepositoryRef.updateExperimentOnBoardDescriptor(uexpobds);
					BusController.getInstance().onBoardNSDFailed(uexpobds);
					return;
				}
				else
				{
					uexpobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
					uexpobds.setFeedbackMessage("NSD Onboarded Successfully");
					// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
					uexpobds.setDeployId(nsd_id);
					// What should be the NSD Name. Something like cirros_nsd.
					uexpobds.setExperimentMANOProviderID(em.getName());
					// Set Onboarding date
					uexpobds.setLastOnboarding(new Date());
					// We decide to set valid when we have the final version. Thus we comment this.
					// uexpobds.getExperiment().setValid(true);
					// Save the changes to vxfobds
					ExperimentOnBoardDescriptor uexpobd_final = portalRepositoryRef.updateExperimentOnBoardDescriptor(uexpobds);
					BusController.getInstance().onBoardNSDSucceded(uexpobds);
				}
			}
		}
	}

	/**
	 * offBoard a VNF to MANO Provider, as described by this descriptor
	 * 
	 * @param c
	 */
	public ResponseEntity<String> offBoardVxFFromMANOProvider(VxFOnBoardedDescriptor obd)
			throws HttpClientErrorException {
		// TODO Auto-generated method stub
		ResponseEntity<String> response = null;
		if (obd.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FOUR")) {
			String vnfd_id = obd.getDeployId();
			OSM4Client osm4Client = new OSM4Client(obd.getObMANOprovider().getApiEndpoint(),
					obd.getObMANOprovider().getUsername(), obd.getObMANOprovider().getPassword(), "admin");
			response = osm4Client.deleteVNFDPackage(vnfd_id);
		}
		if (obd.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM TWO")) {
			response = new ResponseEntity<>("Not implemented for OSMvTWO", HttpStatus.CREATED);
		}
		return response;
	}

	private void checkVxFStatus(VxFOnBoardedDescriptor obd) throws Exception {

		CamelContext tempcontext = new DefaultCamelContext();
		MANOController mcontroller = this;
		try {
			RouteBuilder rb = new RouteBuilder() {
				@Override
				public void configure() throws Exception {
					from("timer://getVNFRepoTimer?delay=2000&period=3000&repeatCount=6&daemon=true")
							.log("Will check VNF repo").setBody().constant(obd)
							.bean(mcontroller, "getVxFStatusFromOSM2Client");
				}
			};
			tempcontext.addRoutes(rb);
			tempcontext.start();
			Thread.sleep(30000);
		} finally {
			tempcontext.stop();
		}

	}

	private void checkOSM4VxFStatus(VxFOnBoardedDescriptor obd) throws Exception {

		CamelContext tempcontext = new DefaultCamelContext();
		MANOController mcontroller = this;
		try {
			RouteBuilder rb = new RouteBuilder() {
				@Override
				public void configure() throws Exception {
					from("timer://getVNFRepoTimer?delay=2000&period=4000&repeatCount=6&daemon=true")
							.log("Will check OSM version FOUR VNF repo").setBody().constant(obd)
							.bean(mcontroller, "getVxFStatusFromOSM4Client");
				}
			};
			tempcontext.addRoutes(rb);
			tempcontext.start();
			Thread.sleep(30000);
		} finally {
			tempcontext.stop();
		}

	}

	private void checkOSM4NSOperationalStatus(String nsd_id) throws Exception {
	}

	public VxFOnBoardedDescriptor getVxFStatusFromOSM2Client(VxFOnBoardedDescriptor obds) {

		Vnfd vnfd = null;
		List<Vnfd> vnfds = OSMClient.getInstance(obds.getObMANOprovider()).getVNFDs();
		if (vnfds != null) {
			for (Vnfd v : vnfds) {
				if (v.getId().equalsIgnoreCase(obds.getVxfMANOProviderID())
						|| v.getName().equalsIgnoreCase(obds.getVxfMANOProviderID())) {
					vnfd = v;
					break;
				}
			}
		}

		if (vnfd == null) {
			obds.setOnBoardingStatus(OnBoardingStatus.UNKNOWN);
		} else {
			obds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
		}

		obds = this.getPortalRepositoryRef().updateVxFOnBoardedDescriptor(obds);

		return obds;
	}

	public VxFOnBoardedDescriptor getVxFStatusFromOSM4Client(VxFOnBoardedDescriptor obds) {
		ns.yang.nfvo.vnfd.rev170228.vnfd.catalog.Vnfd vnfd = null;
		try {
			OSM4Client osm4Client = new OSM4Client(obds.getObMANOprovider().getApiEndpoint(),
					obds.getObMANOprovider().getUsername(), obds.getObMANOprovider().getPassword(), "admin");
			ns.yang.nfvo.vnfd.rev170228.vnfd.catalog.Vnfd[] vnfds = osm4Client.getVNFDs();
			if (vnfds != null) {
				for (ns.yang.nfvo.vnfd.rev170228.vnfd.catalog.Vnfd v : vnfds) {
					System.out.println(v.getId() + " vs " + obds.getDeployId());
					if (v.getId().equalsIgnoreCase(obds.getDeployId())
							|| v.getName().equalsIgnoreCase(obds.getVxfMANOProviderID())) {
						vnfd = v;
						break;
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			System.out.println(e.getStackTrace());
			logger.error(e.getStackTrace());
		}

		// This is not necessary. We just want to be notified through logging that our
		// Object Model did not parse successfully the VNFD.
		// The Onboarding is verified by OSM4 NBI during Onboarding
		// if (vnfd == null) {
		// obds.setOnBoardingStatus(OnBoardingStatus.UNKNOWN);
		// } else {
		// obds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
		// }
		// obds = this.getPortalRepositoryRef().updateVxFOnBoardedDescriptor(obds);

		return obds;
	}

	private void checkNSDStatus(ExperimentOnBoardDescriptor obd) throws Exception {

		CamelContext tempcontext = new DefaultCamelContext();
		MANOController mcontroller = this;
		try {
			RouteBuilder rb = new RouteBuilder() {
				@Override
				public void configure() throws Exception {
					from("timer://getVNFRepoTimer?delay=2000&period=2000&repeatCount=3&daemon=true")
							.log("Will check NSD repo").setBody().constant(obd)
							.bean(mcontroller, "getNSDStatusFromOSM2Client");
				}
			};
			tempcontext.addRoutes(rb);
			tempcontext.start();
			Thread.sleep(30000);
		} finally {
			tempcontext.stop();
		}

	}

	private void checkOSM4NSDStatus(ExperimentOnBoardDescriptor obd) throws Exception {

		CamelContext tempcontext = new DefaultCamelContext();
		MANOController mcontroller = this;
		try {
			RouteBuilder rb = new RouteBuilder() {
				@Override
				public void configure() throws Exception {
					from("timer://getVNFRepoTimer?delay=10000&period=2000&repeatCount=3&daemon=true")
							.log("Will check OSM version FOUR NSD repo").setBody().constant(obd)
							.bean(mcontroller, "getNSDStatusFromOSM4Client");
				}
			};
			tempcontext.addRoutes(rb);
			tempcontext.start();
			Thread.sleep(30000);
		} finally {
			tempcontext.stop();
		}

	}

	public ExperimentOnBoardDescriptor getNSDStatusFromOSM2Client(ExperimentOnBoardDescriptor obds) {

		Nsd nsd = null;
		List<urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.Nsd> nsds = OSMClient
				.getInstance(obds.getObMANOprovider()).getNSDs();
		if (nsds != null) {
			for (Nsd v : nsds) {
				if (v.getId().equalsIgnoreCase(obds.getVxfMANOProviderID())
						|| v.getName().equalsIgnoreCase(obds.getVxfMANOProviderID())) {
					nsd = v;
					break;
				}
			}
		}

		if (nsd == null) {
			obds.setOnBoardingStatus(OnBoardingStatus.UNKNOWN);
		} else {
			obds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
		}

		obds = this.getPortalRepositoryRef().updateExperimentOnBoardDescriptor(obds);

		return obds;
	}

	public ExperimentOnBoardDescriptor getNSDStatusFromOSM4Client(ExperimentOnBoardDescriptor obds) {
		ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd nsd = null;

		try {
			OSM4Client osm4Client = new OSM4Client(obds.getObMANOprovider().getApiEndpoint(),
					obds.getObMANOprovider().getUsername(), obds.getObMANOprovider().getPassword(), "admin");
			ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd[] nsds = osm4Client.getNSDs();
			if (nsds != null) {
				for (ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd v : nsds) {
					// || v.getAddedId().equalsIgnoreCase(obds.getExperimentMANOProviderID())
					if (v.getId().equalsIgnoreCase(obds.getDeployId())
							|| v.getName().equalsIgnoreCase(obds.getExperimentMANOProviderID())) {
						nsd = v;
						break;
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			System.out.println(e.getStackTrace());
			logger.error(e.getStackTrace());
		}

		// This is not necessary. We just want to be notified through logging that our
		// Object Model did not parse successfully the NSD.
		// The Onboarding is verified by OSM4 NBI during Onboarding
		// if (nsd == null) {
		// obds.setOnBoardingStatus(OnBoardingStatus.UNKNOWN);
		// } else {
		// obds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
		// }
		//
		// obds = this.getPortalRepositoryRef().updateExperimentOnBoardDescriptor(obds);

		return obds;
	}

	public void setPortalRepositoryRef(PortalRepository portalRepositoryRef) {
		this.portalRepositoryRef = portalRepositoryRef;
	}

	/**
	 * @return the portalRepositoryRef
	 */
	public PortalRepository getPortalRepositoryRef() {
		return portalRepositoryRef;
	}

	public ResponseEntity<String> offBoardNSDFromMANOProvider(ExperimentOnBoardDescriptor uexpobd) {
		// TODO Auto-generated method stub
		ResponseEntity<String> response = null;
		if (uexpobd.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FOUR")) {
			String nsd_id = uexpobd.getDeployId();
			OSM4Client osm4Client = new OSM4Client(uexpobd.getObMANOprovider().getApiEndpoint(),
					uexpobd.getObMANOprovider().getUsername(), uexpobd.getObMANOprovider().getPassword(), "admin");
			//// Get nsd list
			// ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd[] nsds = osm4Client.getNSDs();
			// for(ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd tmp : nsds)
			// {
			// // Check if nsd_id is available
			// if(tmp.getId().equals(nsd_id))
			// {
			// // If it is available, call offboarding
			response = osm4Client.deleteNSDPackage(nsd_id);
			// return response;
			// }
			// }
		}
		if (uexpobd.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM TWO")) {
			response = new ResponseEntity<>("Not implemented for OSMvTWO", HttpStatus.CREATED);
		}
		// return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
		return response;
	}

	public void deployNSDToMANOProvider(DeploymentDescriptor deploymentdescriptor) {

		if (deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0).getObMANOprovider()
				.getSupportedMANOplatform().getName().equals("OSM FOUR")) {
			// There can be multiple MANOs for the Experiment. We need to handle that also.
			OSM4Client osm4Client = new OSM4Client(
					deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0)
							.getObMANOprovider().getApiEndpoint(),
					deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0)
							.getObMANOprovider().getUsername(),
					deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0)
							.getObMANOprovider().getPassword(),
					"admin");

			NSCreateInstanceRequestPayload nscreateinstancerequestpayload = new NSCreateInstanceRequestPayload(
					osm4Client, deploymentdescriptor);
			// Get Experiment ID and VIM ID and create NS Instance.
			// String nsd_instance_id =
			// osm4Client.createNSInstance(deploymentdescriptor.getName(),deploymentdescriptor.getInfrastructureForAll().getVIMid(),
			// deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0).getDeployId());
			logger.info("NS Instance creation payload : " + nscreateinstancerequestpayload.toJSON());
			// String nsd_instance_id =
			// osm4Client.createNSInstance(nscreateinstancerequestpayload.toJSON());
			ResponseEntity<String> ns_instance_creation_entity = osm4Client
					.createNSInstance(nscreateinstancerequestpayload.toJSON());
			// The NS Instance ID is set

			// NS instance creation
			if (ns_instance_creation_entity == null || ns_instance_creation_entity.getStatusCode().is4xxClientError()
					|| ns_instance_creation_entity.getStatusCode().is5xxServerError()) {
				// NS instance creation failed
				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED);
				deploymentdescriptor.setFeedback(ns_instance_creation_entity.getBody().toString());
				DeploymentDescriptor deploymentdescriptor_final = portalRepositoryRef
						.updateDeploymentDescriptor(deploymentdescriptor);
				BusController.getInstance().deploymentInstantiationFailed(deploymentdescriptor_final);
				logger.info("NS Instance creation failed with response: "
						+ ns_instance_creation_entity.getBody().toString());
			} else {
				// String nsr_id =
				// osm4Client.instantiateNSInstance(nsd_instance_id,deploymentdescriptor.getName(),deploymentdescriptor.getInfrastructureForAll().getVIMid(),
				// deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0).getDeployId());
				JSONObject ns_instance_creation_entity_json_obj = new JSONObject(ns_instance_creation_entity.getBody());
				String nsd_instance_id = ns_instance_creation_entity_json_obj.getString("id");
				deploymentdescriptor.setInstanceId(nsd_instance_id);
				// Instantiate NS Instance
				NSInstantiateInstanceRequestPayload nsrequestpayload = new NSInstantiateInstanceRequestPayload(
						osm4Client, deploymentdescriptor);
				logger.info("NS Instantiation payload : " + nsrequestpayload.toJSON());
				// Here we need the feedback
				// String nsr_id = osm4Client.instantiateNSInstance(nsd_instance_id,
				// nsrequestpayload.toJSON());
				ResponseEntity<String> instantiate_ns_instance_entity = osm4Client.instantiateNSInstance(nsd_instance_id, nsrequestpayload.toJSON());
				if (instantiate_ns_instance_entity == null
						|| instantiate_ns_instance_entity.getStatusCode().is4xxClientError()
						|| instantiate_ns_instance_entity.getStatusCode().is5xxServerError()) {
					// NS Instantiation failed
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.FAILED);
					deploymentdescriptor.setFeedback(instantiate_ns_instance_entity.getBody().toString());
					logger.info("NS Instantiation failed. Status Code:"
							+ instantiate_ns_instance_entity.getStatusCode().toString() + ", Payload:"
							+ ns_instance_creation_entity.getBody().toString());
					// Save the changes to DeploymentDescriptor
					DeploymentDescriptor deploymentdescriptor_final = portalRepositoryRef
							.updateDeploymentDescriptor(deploymentdescriptor);
					BusController.getInstance().deploymentInstantiationFailed(deploymentdescriptor_final);
				} else {
					// NS Instantiation starts
					deploymentdescriptor.setStatus(DeploymentDescriptorStatus.INSTANTIATING);
					deploymentdescriptor.setFeedback(instantiate_ns_instance_entity.getBody().toString());
					logger.info("NS Instantiation of NS with id" + nsd_instance_id + " started.");
					// Save the changes to DeploymentDescriptor
					DeploymentDescriptor deploymentdescriptor_final = portalRepositoryRef
							.updateDeploymentDescriptor(deploymentdescriptor);
					BusController.getInstance().deploymentInstantiationSucceded(deploymentdescriptor_final);
				}
			}
		}
		return;
	}

	public void terminateNSFromMANOProvider(DeploymentDescriptor deploymentdescriptor) {
		if (deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0).getObMANOprovider()
				.getSupportedMANOplatform().getName().equals("OSM FOUR")) {
			// There can be multiple MANOs for the Experiment. We need to handle that also.
			OSM4Client osm4Client = new OSM4Client(
					deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0)
							.getObMANOprovider().getApiEndpoint(),
					deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0)
							.getObMANOprovider().getUsername(),
					deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0)
							.getObMANOprovider().getPassword(),
					"admin");
			// Get Experiment ID and VIM ID and create NS Instance.
			// NS instance termination
			if (osm4Client.terminateNSInstance(deploymentdescriptor.getInstanceId()) != null) {
				// NS Termination succeded
				logger.error("Termination of NS" + deploymentdescriptor.getInstanceId() + " succeded");
//				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.TERMINATING);				
//				deploymentdescriptor.setConstituentVnfrIps("N/A");

				if (osm4Client.deleteNSInstance(deploymentdescriptor.getInstanceId()) != null) {
					logger.error("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " succeded");
				} else {
					logger.error("Deletion of NS instance " + deploymentdescriptor.getInstanceId() + " failed");
				}
				DeploymentDescriptor deploymentdescriptor_final = portalRepositoryRef
						.updateDeploymentDescriptor(deploymentdescriptor);
				BusController.getInstance().terminateInstanceSucceded(deploymentdescriptor_final);
			} else {
				logger.error("Termination of NS instance " + deploymentdescriptor.getInstanceId() + " failed");
//				deploymentdescriptor.setStatus(DeploymentDescriptorStatus.TERMINATION_FAILED);				
//				// NS Termination failed
//				BusController.getInstance().terminateInstanceFailed( deploymentdescriptor );				
			}
		}
	}

}
