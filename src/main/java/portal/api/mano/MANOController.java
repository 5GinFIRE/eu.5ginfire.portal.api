package portal.api.mano;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.core.Context;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import portal.api.bugzilla.BugzillaClient;
import portal.api.model.OnBoardingStatus;
import portal.api.model.VxFMetadata;
import portal.api.model.VxFOnBoardedDescriptor;
import portal.api.osm.client.OSMClient;
import portal.api.repo.PortalRepository;
import portal.api.repo.PortalRepositoryAPIImpl;
import urn.ietf.params.xml.ns.yang.nfvo.vnfd.rev150910.vnfd.catalog.Vnfd;

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
				
		logger.info(portalRepositoryRef.toString() );		
		
		VxFMetadata vxf = vxfobds.getVxf();
		String pLocation = vxf.getPackageLocation();
		if ( !pLocation.contains( "http" )  ) {
			pLocation = "https:" + pLocation;
		}
		
		if (  vxfobds.getObMANOprovider().getSupportedMANOplatform().getName().equals("OSM TWO") ) {			
			OSMClient.getInstance(vxfobds.getObMANOprovider()).createOnBoardVNFDPackage( pLocation,
					vxfobds.getDeployId());
			//run in a thread
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(() -> {
				try {
					checkVxFStatus( vxfobds );
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

	private void checkVxFStatus( VxFOnBoardedDescriptor obd ) throws Exception {
		
		
		CamelContext tempcontext = new DefaultCamelContext();
		MANOController mcontroller = this;
		try {
			RouteBuilder rb = new RouteBuilder() {
	            @Override
	            public void configure() throws Exception {
	                from( "timer://getVNFRepoTimer?delay=2000&period=2000&repeatCount=3&daemon=true"  )
	        		.log( "Will check VNF repo")
	        		.setBody().constant( obd )
	        		.bean( mcontroller  , "getVxFStatus");
	            }
	        };
	        tempcontext.addRoutes( rb);
	        tempcontext.start();
	        Thread.sleep(30000);
		} finally {			
			tempcontext.stop();
        }
		
	}
	
	public VxFOnBoardedDescriptor getVxFStatus( VxFOnBoardedDescriptor obds ) {


		Vnfd vnfd = null;
		List<Vnfd> vnfds = OSMClient.getInstance(obds.getObMANOprovider()).getVNFDs();
		if ( vnfds != null ) {
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

		
		this.getPortalRepositoryRef().updateVxFOnBoardedDescriptor(obds);

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
	
	
	
}
