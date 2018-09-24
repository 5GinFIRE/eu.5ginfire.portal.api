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

import javax.ws.rs.core.Context;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import OSM4NBIClient.OSM4Client;
import portal.api.bugzilla.BugzillaClient;
import portal.api.model.ExperimentMetadata;
import portal.api.model.ExperimentOnBoardDescriptor;
import portal.api.model.OnBoardingStatus;
import portal.api.model.VxFMetadata;
import portal.api.model.VxFOnBoardedDescriptor;
import portal.api.osm.client.OSMClient;
import portal.api.repo.PortalRepository;
import portal.api.repo.PortalRepositoryAPIImpl;
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
	
	

	/**
	 * onBoard a VNF to MANO Provider, as described by this descriptor
	 * 
	 * @param vxfobds
	 * @throws Exception 
	 */
	public void onBoardVxFToMANOProvider(VxFOnBoardedDescriptor vxfobds) throws Exception {

		logger.info(portalRepositoryRef.toString());

		VxFMetadata vxf = vxfobds.getVxf();
		String pLocation = vxf.getPackageLocation();
		if (!pLocation.contains("http")) {
			pLocation = "https:" + pLocation;
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
				VxFOnBoardedDescriptor vxfobds_final = portalRepositoryRef.updateVxFOnBoardedDescriptor(vxfobds);
				return;
			}		
			// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
			vxfobds.setDeployId(vnfd_id);
			// What should be the vxf Name. Something like cirros_vnfd.
			vxfobds.setVxfMANOProviderID(vxf.getName());
			// Set Onboarding date
			vxfobds.setLastOnboarding(new Date());
			// Save the changes to vxfobds
			VxFOnBoardedDescriptor vxfobds_final = portalRepositoryRef.updateVxFOnBoardedDescriptor(vxfobds);

			// run in a thread the GET polling for a VNF onboarding status
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(() -> {
				try {
					checkOSM4VxFStatus(vxfobds_final);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

	}

	public void onBoardNSDToMANOProvider(ExperimentOnBoardDescriptor uexpobd) throws Exception{

		ExperimentMetadata em = uexpobd.getExperiment();
		String pLocation = em.getPackageLocation();
		if (!pLocation.contains("http")) {
			pLocation = "http:" + pLocation;
			pLocation = pLocation.replace("\\", "/");			
		}

		logger.info("Experiment Package Location: " + em.getPackageLocation());
		// Here we need to get a better solution for the OSM version names.
		if (uexpobd.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM2")) {
			OSMClient.getInstance(uexpobd.getObMANOprovider()).createOnBoardNSDPackage(pLocation,
					uexpobd.getDeployId());
			// run in a thread the GET polling for a NSD onboarding status
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(() -> {
				try {
					checkNSDStatus(uexpobd);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}		
		if (uexpobd.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM4")) {
			OSM4Client osm4Client = new OSM4Client(uexpobd.getObMANOprovider().getApiEndpoint(),uexpobd.getObMANOprovider().getUsername(),uexpobd.getObMANOprovider().getPassword(),"admin");
			String nsd_id = osm4Client.onBoardNSD(pLocation);		
			if(nsd_id == null)
			{
				uexpobd.setOnBoardingStatus(OnBoardingStatus.FAILED);
				ExperimentOnBoardDescriptor uexpobd_final = portalRepositoryRef.updateExperimentOnBoardDescriptor(uexpobd);
				return;
			}		
			else
			{
				uexpobd.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
			}
			// The Deploy ID is set as the VNFD Package id in OSMANO4Provider
			uexpobd.setDeployId(nsd_id);
			// What should be the NSD Name. Something like cirros_nsd.
			uexpobd.setExperimentMANOProviderID(em.getName());
			// Set Onboarding date
			uexpobd.setLastOnboarding(new Date());
			// Save the changes to vxfobds
			ExperimentOnBoardDescriptor uexpobd_final = portalRepositoryRef.updateExperimentOnBoardDescriptor(uexpobd);

			// run in a thread the GET polling for a VNF onboarding status
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(() -> {
				try {
					checkOSM4NSDStatus(uexpobd_final);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

	}

	/**
	 * offBoard a VNF to MANO Provider, as described by this descriptor
	 * 
	 * @param c
	 */
	public void offBoardVxF(VxFOnBoardedDescriptor obd) {
		// TODO Auto-generated method stub

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

		if (vnfd == null) {
			obds.setOnBoardingStatus(OnBoardingStatus.UNKNOWN);
		} else {
			obds.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
		}

		obds = this.getPortalRepositoryRef().updateVxFOnBoardedDescriptor(obds);

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
		OSM4Client osm4Client = new OSM4Client(obds.getObMANOprovider().getApiEndpoint(),obds.getObMANOprovider().getUsername(),obds.getObMANOprovider().getPassword(),"admin");
		ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd[] nsds = osm4Client.getNSDs();
		if (nsds != null) {
			for (ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd v : nsds) {
				if (v.getId().equalsIgnoreCase(obds.getExperimentMANOProviderID())
						|| v.getName().equalsIgnoreCase(obds.getExperimentMANOProviderID())) {
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
	
	

	public void setPortalRepositoryRef(PortalRepository portalRepositoryRef) {
		this.portalRepositoryRef = portalRepositoryRef;
	}

	/**
	 * @return the portalRepositoryRef
	 */
	public PortalRepository getPortalRepositoryRef() {
		return portalRepositoryRef;
	}

	public void offBoardNSDFromMANOProvider(ExperimentOnBoardDescriptor uexpobd) {
		// TODO Auto-generated method stub
		
	}

}
