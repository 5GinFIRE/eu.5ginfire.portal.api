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

package portal.api.validation.ci;

import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import portal.api.bugzilla.BugzillaClient;
import portal.api.bugzilla.model.Bug;
import portal.api.model.ExperimentMetadata;
import portal.api.model.Product;
import portal.api.model.ValidationJob;
import portal.api.model.ValidationStatus;
import portal.api.model.VxFMetadata;
import portal.api.repo.PortalRepository;

/**
 * @author ctranoris
 *
 */
public class ValidationCIRouteBuilder extends RouteBuilder {

	private static String JENKINSCIKEY = "5ginfire@test";
	private static String PIPELINE_TOKEN = "test";	 
	private static String JENKINSCIURL = "localhost:13000";
	
	public void configure() {

		if (PortalRepository.getPropertyByName("jenkinsciurl").getValue() != null) {
			JENKINSCIURL = PortalRepository.getPropertyByName("jenkinsciurl").getValue();
		}
		if (PortalRepository.getPropertyByName("jenkinscikey").getValue() != null) {
			JENKINSCIKEY = PortalRepository.getPropertyByName("jenkinscikey").getValue();
		}
		if (PortalRepository.getPropertyByName("pipelinetoken").getValue() != null) {
			PIPELINE_TOKEN = PortalRepository.getPropertyByName("pipelinetoken").getValue();
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
		.log( "Submit new validation request for VNF_ID=${body.getId}" )	
		.errorHandler(deadLetterChannel("direct:dlq_validations")
				.maximumRedeliveries( 3 ) //let's try 3 times to send it....
				.redeliveryDelay( 30000 ).useOriginalMessage()
				.deadLetterHandleNewException( false )
				//.logExhaustedMessageHistory(false)
				.logExhausted(true)
				.logHandled(true)
				//.retriesExhaustedLogLevel(LoggingLevel.WARN)
				.retryAttemptedLogLevel( LoggingLevel.WARN) )
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
		.process( headerExtractProcessor )
		.toD( "http4://" + JENKINSCIURL + "/job/validation_pipeline/buildWithParameters?token=" + PIPELINE_TOKEN + "&VNF_ID=${header.id}")
		.to("stream:out");
				
		
		/**
		 * dead Letter Queue Users if everything fails to connect
		 */
		from("direct:dlq_validations")
		//.setBody()
		//.body(String.class)
		.process( ErroneousValidationProcessor )
		.to( "seda:vxf.validationresult.update?multipleConsumers=true")
		.to("stream:out");
		
	}
	
	Processor ErroneousValidationProcessor = new Processor() {
		
		@Override
		public void process(Exchange exchange) throws Exception {

			Map<String, Object> headers = exchange.getIn().getHeaders(); 
			Product aProd = exchange.getIn().getBody( Product.class ); 
			
		    		
			if (aProd instanceof VxFMetadata) {
				((VxFMetadata) aProd).setValidationStatus( ValidationStatus.COMPLETED );
			} else if (aProd instanceof ExperimentMetadata) {
				((ExperimentMetadata) aProd).setValidationStatus( ValidationStatus.COMPLETED );
			}
			
			
			if ( aProd.getValidationJobs() != null ) {
				ValidationJob j = new ValidationJob();
				j.setDateCreated( new Date() );
				j.setJobid("ERROR");
				j.setValidationStatus(false);
				j.setOutputLog( "There is an error from the Validation Service" );
				aProd.getValidationJobs().add(j);
			}
		    
		    exchange.getOut().setBody( aProd  );
		    // copy attachements from IN to OUT to propagate them
		    exchange.getOut().setAttachments(exchange.getIn().getAttachments());
			
		}
	};
	
	
	Processor headerExtractProcessor = new Processor() {
		
		@Override
		public void process(Exchange exchange) throws Exception {

			Map<String, Object> headers = exchange.getIn().getHeaders(); 
			VxFMetadata m = exchange.getIn().getBody( VxFMetadata.class ); 
		    headers.put("id", m.getId()  );
		    String encoding = Base64.getEncoder().encodeToString( (JENKINSCIKEY).getBytes() );
		    headers.put("Authorization",  "Basic " + encoding  );
		    
		    exchange.getOut().setHeaders(headers);
		    
//		    //copy Description to Comment
//		    aBug.setComment( BugzillaClient.createComment( aBug.getDescription() ) );
//		    //delete Description
//		    aBug.setDescription( null );
//		    aBug.setAlias( null ); //dont put any Alias		
//		    aBug.setCc( null );
		    
		    exchange.getOut().setBody( ""  );
		    // copy attachements from IN to OUT to propagate them
		    exchange.getOut().setAttachments(exchange.getIn().getAttachments());
			
		}
	};

}
