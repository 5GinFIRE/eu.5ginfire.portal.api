package portal.api.bus;

import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.model.ModelCamelContext;

import portal.api.model.PortalUser;

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
	
	/** the Camel Context configure via Spring*/
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
	 * Synchronously propagates to the routing bus that a new user is added
	 * @param u a {@link PortalUser}
	 */
	public void newUserAdded(PortalUser u) {
		
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:users.new?multipleConsumers=true");
		
	}
	
}
