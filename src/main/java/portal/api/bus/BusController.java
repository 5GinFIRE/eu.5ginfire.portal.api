package portal.api.bus;

import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.model.ModelCamelContext;

import portal.api.model.DeploymentDescriptor;
import portal.api.model.ExperimentMetadata;
import portal.api.model.ExperimentOnBoardDescriptor;
import portal.api.model.PortalUser;
import portal.api.model.VxFMetadata;
import portal.api.model.VxFOnBoardedDescriptor;

/**
 * Exposes messages to Bus. Usually they should be aynchronous.
 * Consult http://camel.apache.org/uris.html for URIs
 * sendmessage(direct:mplampla) is Synchronous in same Context
 * sendmessage(seda:mplampla) is sSynchronous in same Context
 *  * 
 * @author ctranoris
 * 
 * 
 *
 */
public class BusController {

	/** */
	private static BusController instance;
	
	/** the Camel Context configure via Spring. See bean.xml*/	
	private static ModelCamelContext actx;



	/**
	 * @return
	 */
	public static synchronized BusController getInstance() {
		if (instance == null) {
			instance = new BusController();
		}
		return instance;
	}
	

	/**
	 * @return
	 */
	public static ModelCamelContext getActx() {
		return actx;
	}

	/**
	 * @param actx
	 */
	public static void setActx(ModelCamelContext actx) {
		BusController.actx = actx;
	}

	/**
	 * Asynchronously sends to the routing bus (seda:users.create?multipleConsumers=true) that a new user is added
	 * @param u a {@link PortalUser}
	 */
	public void newUserAdded(PortalUser u) {
		
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:users.create?multipleConsumers=true");
		template.withBody( u ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:deployments.create?multipleConsumers=true) that a new user is added
	 * @param deployment a {@link DeploymentDescriptor}
	 */
	public void newDeploymentRequest(DeploymentDescriptor deployment) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:deployments.create?multipleConsumers=true");
		template.withBody( deployment ).asyncSend();
		
	}
	
	/**
	 * Asynchronously sends to the routing bus (seda:deployments.create?multipleConsumers=true) that a new user is added
	 * @param deployment a {@link DeploymentDescriptor}
	 */
	public void updateDeploymentRequest(DeploymentDescriptor deployment) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:deployments.update?multipleConsumers=true");
		template.withBody( deployment ).asyncSend();
		
	}


	public void newVxFAdded(VxFMetadata vxf) {
		
		// TODO Auto-generated method stub
		
	}


	public void newNSFAdded(ExperimentMetadata experiment) {
		// TODO Auto-generated method stub
		
	}


	public void onBoardVxF(VxFOnBoardedDescriptor vxfobds) {
		// TODO Auto-generated method stub
		
	}


	public void offBoardVxF(VxFOnBoardedDescriptor u) {
		// TODO Auto-generated method stub
		
	}


	public void onBoardNSD(ExperimentOnBoardDescriptor uexpobd) {
		// TODO Auto-generated method stub
		
	}


	public void offBoardNSD(ExperimentOnBoardDescriptor u) {
		// TODO Auto-generated method stub
		
	}
	
}
