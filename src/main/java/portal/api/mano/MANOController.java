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

import java.text.SimpleDateFormat;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import OSM4NBIClient.OSM4Client;
import portal.api.bus.BusController;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.DeploymentDescriptorStatus;
import portal.api.model.ExperimentMetadata;
import portal.api.model.ExperimentOnBoardDescriptor;
import portal.api.model.OnBoardingStatus;
import portal.api.model.VxFMetadata;
import portal.api.model.VxFOnBoardedDescriptor;
import portal.api.osm.client.OSMClient;
import portal.api.repo.PortalRepository;
import urn.ietf.params.xml.ns.yang.nfvo.vnfd.rev150910.vnfd.catalog.Vnfd;
import urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.Nsd;

/**
 * @author ctranoris
 *
 */
public class MANOController {
	
	/** This is also binded by Bean */
	private PortalRepository portalRepositoryRef;

	/** */
	private static final transient Log logger = LogFactory.getLog(MANOController.class.getName());
	
	public MANOController()
	{
		this.portalRepositoryRef=new PortalRepository();
	}

	/**
	 * onBoard a VNF to MANO Provider, as described by this descriptor
	 * 
	 * @param vxfobds
	 * @throws Exception 
	 */
	public void onBoardVxFToMANOProvider(VxFOnBoardedDescriptor vxfobd) throws Exception {

		//PortalRepository portalRepositoryRef = new PortalRepository();
		
		vxfobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		//This is the Deployment ID for the portal
		vxfobd.setDeployId(UUID.randomUUID().toString());
		VxFMetadata vxf = vxfobd.getVxf();
		if (vxf == null) {
			vxf = (VxFMetadata) portalRepositoryRef.getProductByID(vxfobd.getVxfid());
		}
		//Set MANO Provider VxF ID 
		vxfobd.setVxfMANOProviderID(vxf.getName());
		//Set onBoarding Date
		vxfobd.setLastOnboarding(new Date());

		VxFOnBoardedDescriptor vxfobds = portalRepositoryRef.updateVxFOnBoardedDescriptor(vxfobd);
		if (vxfobds == null) {
			throw new Exception("Cannot load VxFOnBoardedDescriptor");
		}			
	
		logger.info(portalRepositoryRef.toString());

		String pLocation = vxf.getPackageLocation();
		logger.info("VxF Package Location: " + pLocation);
		if (!pLocation.contains("http")) {
			pLocation = "http:" + pLocation;
			pLocation = pLocation.replace("\\", "/");
		}

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
			OSM4Client osm4Client = new OSM4Client(vxfobds.getObMANOprovider().getApiEndpoint(),vxfobds.getObMANOprovider().getUsername(),vxfobds.getObMANOprovider().getPassword(),"admin");			
			String vnfd_id = osm4Client.onBoardVNFD(pLocation);		
			if(vnfd_id == null)
			{
				vxfobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				vxfobds.getVxf().setCertified(false);
				VxFOnBoardedDescriptor vxfobds_final = portalRepositoryRef.updateVxFOnBoardedDescriptor(vxfobds);
				BusController.getInstance().onBoardVxFFailed( vxfobds_final );
				return;
			}		
			vxfobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
			vxfobds.getVxf().setCertified(true);
			// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
			vxfobds.setDeployId(vnfd_id);
			// What should be the vxf Name. Something like cirros_vnfd.
			vxfobds.setVxfMANOProviderID(vxf.getName());
			// Set Onboarding date
			vxfobds.setLastOnboarding(new Date());
			// Save the changes to vxfobds
			VxFOnBoardedDescriptor vxfobds_final = portalRepositoryRef.updateVxFOnBoardedDescriptor(vxfobds);
			BusController.getInstance().onBoardVxFSucceded( vxfobds_final );
			//This is not necessary any more.
			//// run in a thread the GET polling for a VNF onboarding status
			//ExecutorService executor = Executors.newSingleThreadExecutor();
			//executor.submit(() -> {
			//	try {
			//		checkOSM4VxFStatus(vxfobds_final);
			//	} catch (Exception e) {
			//		e.printStackTrace();
			//	}
			//});
		}

	}

	//Catch an VxF Onboarding Message
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
	
	
	public void onBoardNSDToMANOProvider(ExperimentOnBoardDescriptor uexpobd) throws Exception{

		uexpobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		//This is the Deployment ID for the portal		
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
		
		uexpobd.setVxfMANOProviderID(em.getName()); // Possible Error. This probably needs to be setExperimentMANOProviderID(em.getName())

		uexpobd.setLastOnboarding(new Date());

		ExperimentOnBoardDescriptor uexpobds = portalRepositoryRef.updateExperimentOnBoardDescriptor(uexpobd);
		if (uexpobds == null) {
			throw new Exception("Cannot load NSDOnBoardedDescriptor");
		}			
						
		String pLocation = em.getPackageLocation();
		logger.info("NSD Package Location: " + pLocation);		
		if (!pLocation.contains("http")) {
			pLocation = "http:" + pLocation;
			pLocation = pLocation.replace("\\", "/");			
		}

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
			OSM4Client osm4Client = new OSM4Client(uexpobds.getObMANOprovider().getApiEndpoint(),uexpobds.getObMANOprovider().getUsername(),uexpobds.getObMANOprovider().getPassword(),"admin");
			String nsd_id = osm4Client.onBoardNSD(pLocation);		
			if(nsd_id == null)
			{
				uexpobds.setOnBoardingStatus(OnBoardingStatus.FAILED);
				uexpobds.getExperiment().setValid(false);
				ExperimentOnBoardDescriptor uexpobd_final = portalRepositoryRef.updateExperimentOnBoardDescriptor(uexpobds);
				BusController.getInstance().onBoardNSDFailed( uexpobd_final );				
				return;
			}		
			else
			{
				uexpobds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
				uexpobds.getExperiment().setValid(true);
			}
			// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
			uexpobds.setDeployId(nsd_id);
			// What should be the NSD Name. Something like cirros_nsd.
			uexpobds.setExperimentMANOProviderID(em.getName());
			// Set Onboarding date
			uexpobds.setLastOnboarding(new Date());
			// Save the changes to vxfobds
			ExperimentOnBoardDescriptor uexpobd_final = portalRepositoryRef.updateExperimentOnBoardDescriptor(uexpobds);
			BusController.getInstance().onBoardNSDSucceded( uexpobd_final );

			//This is not necessary any more.
			//// run in a thread the GET polling for a VNF onboarding status
			//ExecutorService executor = Executors.newSingleThreadExecutor();
			//executor.submit(() -> {
			//	try {
			//		checkOSM4NSDStatus(uexpobd_final);
			//	} catch (Exception e) {
			//		e.printStackTrace();
			//	}
			//});
		}

	}

	/**
	 * offBoard a VNF to MANO Provider, as described by this descriptor
	 * 
	 * @param c
	 */
	public ResponseEntity<String> offBoardVxFFromMANOProvider(VxFOnBoardedDescriptor obd) throws HttpClientErrorException {
		// TODO Auto-generated method stub
		ResponseEntity<String> response = null;
		if (obd.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM FOUR")) {
			String vnfd_id=obd.getDeployId();
			OSM4Client osm4Client = new OSM4Client(obd.getObMANOprovider().getApiEndpoint(),obd.getObMANOprovider().getUsername(),obd.getObMANOprovider().getPassword(),"admin");			
			response=osm4Client.deleteVNFDPackage(vnfd_id);
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
							.log("Will check VNF repo")
							.setBody().constant(obd)
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
							.log("Will check OSM version FOUR VNF repo")
							.setBody().constant(obd)
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
		try
		{
			OSM4Client osm4Client = new OSM4Client(obds.getObMANOprovider().getApiEndpoint(),obds.getObMANOprovider().getUsername(),obds.getObMANOprovider().getPassword(),"admin");		
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
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
			System.out.println(e.getStackTrace());
			logger.error(e.getStackTrace());
		}
		
		//This is not necessary. We just want to be notified through logging that our Object Model did not parse successfully the VNFD.
		//The Onboarding is verified by OSM4 NBI during Onboarding
		//if (vnfd == null) {
		//	obds.setOnBoardingStatus(OnBoardingStatus.UNKNOWN);
		//} else {
		//	obds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
		//}
		//obds = this.getPortalRepositoryRef().updateVxFOnBoardedDescriptor(obds);

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
							.log("Will check NSD repo")
							.setBody().constant(obd)
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
							.log("Will check OSM version FOUR NSD repo")
							.setBody().constant(obd)
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
		List<urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.Nsd> nsds = OSMClient.getInstance(obds.getObMANOprovider()).getNSDs(); 
		if ( nsds != null ) {
			for (Nsd v : nsds) {
				if (v.getId().equalsIgnoreCase(obds.getVxfMANOProviderID() )
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
			OSM4Client osm4Client = new OSM4Client(obds.getObMANOprovider().getApiEndpoint(),obds.getObMANOprovider().getUsername(),obds.getObMANOprovider().getPassword(),"admin");
			ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd[] nsds = osm4Client.getNSDs();
			if (nsds != null) {
				for (ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd v : nsds) {
					//|| v.getAddedId().equalsIgnoreCase(obds.getExperimentMANOProviderID())
					if (v.getId().equalsIgnoreCase(obds.getDeployId()) 
							|| v.getName().equalsIgnoreCase(obds.getExperimentMANOProviderID())) {
						nsd = v;
						break;
					}
				}
			}
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
			System.out.println(e.getStackTrace());
			logger.error(e.getStackTrace());
		}

		//This is not necessary. We just want to be notified through logging that our Object Model did not parse successfully the NSD.
		//The Onboarding is verified by OSM4 NBI during Onboarding
		//if (nsd == null) {
		//	obds.setOnBoardingStatus(OnBoardingStatus.UNKNOWN);
		//} else {
		//	obds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
		//}
		//
		//obds = this.getPortalRepositoryRef().updateExperimentOnBoardDescriptor(obds);

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
			String nsd_id=uexpobd.getDeployId();
			OSM4Client osm4Client = new OSM4Client(uexpobd.getObMANOprovider().getApiEndpoint(),uexpobd.getObMANOprovider().getUsername(),uexpobd.getObMANOprovider().getPassword(),"admin");			
			response=osm4Client.deleteNSDPackage(nsd_id);
		}
		return response;		
	}		

}
