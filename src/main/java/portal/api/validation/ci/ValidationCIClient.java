package portal.api.validation.ci;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import portal.api.bugzilla.BugzillaClient;
import portal.api.model.VxFMetadata;

/**
 * @author ctranoris
 *
 */
public class ValidationCIClient {
		

	private static final transient Log logger = LogFactory.getLog(BugzillaClient.class.getName());

	/** */
	private static ValidationCIClient instance;

	

	public static ValidationCIClient getInstance() {
		if (instance == null) {
			instance = new ValidationCIClient();
		}
		return instance;
	}
	
	
	public static void transformVxF2ValidationRequest(VxFMetadata vxf) {
		
	}

}
