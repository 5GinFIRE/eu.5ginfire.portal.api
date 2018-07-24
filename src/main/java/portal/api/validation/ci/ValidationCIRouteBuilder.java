package portal.api.validation.ci;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import portal.api.repo.PortalRepository;

/**
 * @author ctranoris
 *
 */
public class ValidationCIRouteBuilder extends RouteBuilder {
	
	private static String JENKINSCIKEY = "test";
	private static String JENKINSCIURL = "localhost:13000";
	
	public void configure() {

		if (PortalRepository.getPropertyByName("jenkinsciurl").getValue() != null) {
			JENKINSCIURL = PortalRepository.getPropertyByName("jenkinsciurl").getValue();
		}
		if (PortalRepository.getPropertyByName("jenkinscikey").getValue() != null) {
			JENKINSCIKEY = PortalRepository.getPropertyByName("jenkinscikey").getValue();
		}
		
		if ( ( JENKINSCIURL == null ) || JENKINSCIURL.equals( "" ) ){
			return; //no routing towards JENKINS
		}
		if ( ( JENKINSCIKEY == null ) || JENKINSCIKEY.equals( "" ) ){
			return;//no routing towards JENKINS
		}
		
		
		/**
		 * Create VxF Validate New Route
		 */
		from("seda:vxf.new.validation?multipleConsumers=true")
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
		.toD( "http4://" + JENKINSCIKEY + "@" + JENKINSCIURL + "/job/validation_pipeline&buildWithParameters?token=PIPELINE_TOKEN&VNF_ID=${body.id}")
		.to("stream:out");
		
		

		
	}

}
