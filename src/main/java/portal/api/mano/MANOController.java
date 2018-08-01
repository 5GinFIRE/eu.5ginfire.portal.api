package portal.api.mano;

import portal.api.model.VxFMetadata;
import portal.api.model.VxFOnBoardedDescriptor;
import portal.api.osm.client.OSMClient;

public class MANOController {
	
	
	/** */
	private static MANOController instance;
	
	/**
	 * @return
	 */
	public static synchronized MANOController getInstance() {
		if (instance == null) {
			instance = new MANOController();
		}
		return instance;
	}

	/**
	 * onBoard a VNF to MANO Provider, as described by this descriptor
	 * 
	 * @param vxfobds
	 */
	public void onBoardVxFToMANOProvider(VxFOnBoardedDescriptor vxfobds) {
				
		VxFMetadata vxf = vxfobds.getVxf();
		String pLocation = vxf.getPackageLocation();
		if ( !pLocation.contains( "http" )  ) {
			pLocation = "https:" + pLocation;
		}
		
		OSMClient.getInstance(vxfobds.getObMANOprovider()).createOnBoardVNFDPackage( pLocation,
				vxfobds.getDeployId());
		
	}


}
