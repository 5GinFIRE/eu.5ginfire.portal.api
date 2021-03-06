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

package portal.api.bugzilla;


import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.net.ssl.SSLContext;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import portal.api.bugzilla.model.Bug;
import portal.api.repo.PortalRepository;

/**
 * @author ctranoris
 *
 */
public class BugzillaRouteBuilder extends RouteBuilder {

	private static String BUGZILLAKEY = "";
	private static String BUGZILLAURL = "portal.5ginfire.eu:443/bugzilla";
	
	

	private static final transient Log logger = LogFactory.getLog( BugzillaRouteBuilder.class.getName() );
	

	//private static ModelCamelContext actx;

	public static void main(String[] args) throws Exception {
		//new Main().run(args);
		
		CamelContext context = new DefaultCamelContext();
		try {
			ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&amp;broker.useJmx=true"); 
			context.addComponent("jms", ActiveMQComponent.jmsComponentAutoAcknowledge(connectionFactory));			

			context.addRoutes( new BugzillaRouteBuilder() );
			context.start();
			
			//test new user
//			FluentProducerTemplate template = context.createFluentProducerTemplate().to("seda:users.create?multipleConsumers=true");
//			PortalUser owner = new PortalUser();
//			owner.setEmail( "tranoris@example.org" );
//			owner.setName( "Christos Tranoris");
//			template.withBody( owner ).asyncSend();		
//			
//			// test New Deployment
//			FluentProducerTemplate template = context.createFluentProducerTemplate().to("seda:deployments.create?multipleConsumers=true");
//			String uuid = "02b0b0d9-d73a-451f-8cb2-79d398a375b4"; //UUID.randomUUID().toString();
//			DeploymentDescriptor deployment = new DeploymentDescriptor( uuid , "An Experiment");
//			deployment.setDescription("test asfdsf\n test asfdsf\n test asfdsf\n");
//			PortalUser owner = new PortalUser();
//			owner.setUsername( "admin" );
//			owner.setEmail( "tranoris@ece.upatras.gr" );
//			deployment.setOwner(owner);
//			deployment.setDateCreated( new Date());
//			deployment.setStartReqDate( new Date());
//			deployment.setEndReqDate( new Date());
//			ExperimentMetadata exper = new ExperimentMetadata();
//			exper.setName( "An experiment NSD" ); 
//			deployment.setExperiment(exper);
//			template.withBody( deployment ).asyncSend();			
//
//            Thread.sleep(4000);
//
//			// test Update Deployment
//			FluentProducerTemplate templateUpd = context.createFluentProducerTemplate().to("seda:deployments.update?multipleConsumers=true");
//			//DeploymentDescriptor deployment = new DeploymentDescriptor( uuid, "An Experiment");
//			//deployment.setDescription("test asfdsf\n test asfdsf\n test asfdsf\n");
//			//PortalUser owner = new PortalUser();
//			//owner.setUsername( "admin" );
//			//owner.setEmail( "tranoris@ece.upatras.gr" );
//			//deployment.setOwner(owner);
//			//deployment.setDateCreated( new Date());
//			//deployment.setStartReqDate( new Date());
//			//deployment.setEndReqDate( new Date());
//			
//			deployment.setStatus( DeploymentDescriptorStatus.SCHEDULED );
//			deployment.setStartDate(  new Date() );
//			deployment.setEndDate(  new Date() );
//			deployment.setFeedback( "A feedback\n more feedback " );			
//			templateUpd.withBody( deployment ).asyncSend();
			
			
			
            Thread.sleep(60000);
		} finally {			
            context.stop();
        }
		
		
	}

	
	public void configure() {

		if (PortalRepository.getPropertyByName("bugzillaurl").getValue() != null) {
			BUGZILLAURL = PortalRepository.getPropertyByName("bugzillaurl").getValue();
		}
		if (PortalRepository.getPropertyByName("bugzillakey").getValue() != null) {
			BUGZILLAKEY = PortalRepository.getPropertyByName("bugzillakey").getValue();
		}
		
		if ( ( BUGZILLAURL == null ) || BUGZILLAURL.equals( "" ) ){
			return; //no routing towards Bugzilla
		}
		if ( ( BUGZILLAKEY == null ) || BUGZILLAKEY.equals( "" ) ){
			return;//no routing towards Bugzilla
		}
		

		HttpComponent httpComponent = getContext().getComponent("https4", HttpComponent.class);
		httpComponent.setHttpClientConfigurer(new MyHttpClientConfigurer());

		/**
		 * Create New Issue in Bugzilla. The body is a {@link Bug}
		 */
		from("direct:bugzilla.newIssue")
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo( String.class ).to("stream:out")
		.errorHandler(deadLetterChannel("direct:dlq_bugzilla")
				.maximumRedeliveries( 4 ) //let's try for the next 120 mins to send it....
				.redeliveryDelay( 60000 ).useOriginalMessage()
				.deadLetterHandleNewException( false )
				//.logExhaustedMessageHistory(false)
				.logExhausted(true)
				.logHandled(true)
				//.retriesExhaustedLogLevel(LoggingLevel.WARN)
				.retryAttemptedLogLevel( LoggingLevel.WARN) )
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
		.toD( "https4://" + BUGZILLAURL + "/rest.cgi/bug?api_key="+ BUGZILLAKEY +"&throwExceptionOnFailure=true")
		.to("stream:out");
		
		/**
		 * Update issue in bugzilla. The body is a {@link Bug}. header.uuid is used to select the bug
		 */
		from("direct:bugzilla.updateIssue")
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo( String.class ).to("stream:out")
		.errorHandler(deadLetterChannel("direct:dlq_bugzilla")
				.maximumRedeliveries( 4 ) //let's try for the next 120 minutess to send it....
				.redeliveryDelay( 60000 ).useOriginalMessage()
				.deadLetterHandleNewException( false )
				//.logExhaustedMessageHistory(false)
				.logExhausted(true)
				.logHandled(true)
				//.retriesExhaustedLogLevel(LoggingLevel.WARN)
				.retryAttemptedLogLevel( LoggingLevel.WARN) )
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.PUT))
		.toD( "https4://" + BUGZILLAURL + "/rest.cgi/bug/${header.uuid}?api_key="+ BUGZILLAKEY +"&throwExceptionOnFailure=true")
		.to("stream:out");
		
		
		/**
		 * Create user route, from seda:users.create?multipleConsumers=true
		 */
		
		from("seda:users.create?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformUser2BugzillaUser")
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo( String.class ).to("stream:out")
		.errorHandler(deadLetterChannel("direct:dlq_users")
				.maximumRedeliveries( 4 ) //let's try 10 times to send it....
				.redeliveryDelay( 60000 ).useOriginalMessage()
				.deadLetterHandleNewException( false )
				//.logExhaustedMessageHistory(false)
				.logExhausted(true)
				.logHandled(true)
				//.retriesExhaustedLogLevel(LoggingLevel.WARN)
				.retryAttemptedLogLevel( LoggingLevel.WARN) )
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
		.toD( "https4://" + BUGZILLAURL + "/rest.cgi/user?api_key="+ BUGZILLAKEY +"&throwExceptionOnFailure=true")
		.to("stream:out");
		
		
		/**
		 * Create Deployment Route Issue
		 */
		from("seda:deployments.create?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformDeployment2BugBody")
		.to("direct:bugzilla.newIssue");
				
		/**
		 * Update Deployment Route
		 */
		from("seda:deployments.update?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformDeployment2BugBody")
		.process( BugHeaderExtractProcessor )
		.to("direct:bugzilla.updateIssue");
		
		

		/**
		 * dead Letter Queue Users if everything fails to connect
		 */
		from("direct:dlq_users")
		.setBody()
//		.body(DeploymentDescriptor.class)
//		.bean( BugzillaClient.class, "transformDeployment2BugBody")
		.body(String.class)
		.to("stream:out");
		
		
		from("direct:bugzilla.bugmanage")
		.choice()
		.when( issueExists )
			.log( "Update ISSUE for ${body.alias} !" )		
			.process( BugHeaderExtractProcessor )
			.to("direct:bugzilla.updateIssue")
			.endChoice()
		.otherwise()
			.log( "New ISSUE for ${body.alias} !" )	
			.to("direct:bugzilla.newIssue")
			.endChoice();
		
		
		/**
		 * Create VxF Validate New Route
		 */
		String jenkinsURL = null;
		if (PortalRepository.getPropertyByName("jenkinsciurl").getValue() != null) {
			jenkinsURL = PortalRepository.getPropertyByName("jenkinsciurl").getValue();
		}
		if ( ( jenkinsURL != null ) && ( !jenkinsURL.equals( "" ) ) ){
			from("seda:vxf.new.validation?multipleConsumers=true")
			.delay(30000)			
			.bean( BugzillaClient.class, "transformVxFValidation2BugBody")
			.to("direct:bugzilla.newIssue");
		}
		
		
	
		
		/**
		 * Update Validation Route
		 */
		from("seda:vxf.validationresult.update?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformVxFValidation2BugBody")
		.to("direct:bugzilla.bugmanage");
		
		
		/**
		 * Create VxF Validate New Route
		 */
		from("seda:vxf.onboard?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformVxFAutomaticOnBoarding2BugBody")
		.to("direct:bugzilla.newIssue");

		/**
		 * Create VxF OffBoard New Route
		 */
		from("seda:vxf.offboard?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformVxFAutomaticOffBoarding2BugBody")
		.to("direct:bugzilla.bugmanage");
		
		/**
		 * Automatic OnBoarding Route Success
		 */		
		from("seda:vxf.onboard.success?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformVxFAutomaticOnBoarding2BugBody")
		.process( BugHeaderExtractProcessor )
		.to("direct:bugzilla.updateIssue");
		//.to("direct:bugzilla.bugmanage");
		

		/**
		 * Automatic OnBoarding Route Fail
		 */		
		from("seda:vxf.onboard.fail?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformVxFAutomaticOnBoarding2BugBody")
		.process( BugHeaderExtractProcessor )
		.to("direct:bugzilla.updateIssue");
		//.to("direct:bugzilla.bugmanage");	

		/**
		 * Automatic OnBoarding Route Result
		 */		
		from("seda:vxf.onboard.result?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformVxFAutomaticOnBoarding2BugBody")
		.process( BugHeaderExtractProcessor )
		.to("direct:bugzilla.updateIssue");
		//.to("direct:bugzilla.bugmanage");	
		
		
		
		
		/**
		 * IMPORTANT NOTE: NSD ISSUE VALIDATION IS DISABLED FOR NOW
		 * SINCE THERE IS NO nsd VALIDATION!
		//Create NSD Validate New Route 
		from("seda:nsd.validate.new?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformNSDValidation2BugBody")
		.to("direct:bugzilla.newIssue");
				
		//Create NSD Validation Update Route		 
		from("seda:nsd.validate.update?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformNSDValidation2BugBody")
		.choice()
		.when( issueExists )
			.log( "Update ISSUE for validating ${body.alias} !" )		
			.process( BugHeaderExtractProcessor )
			.to("direct:bugzilla.updateIssue")
			.endChoice()
		.otherwise()
			.log( "New ISSUE for validating ${body.alias} !" )	
			.to("direct:bugzilla.newIssue")
			.endChoice();

		 */
		
		/**
		 * Create NSD onboard New Route
		 */
		from("seda:nsd.onboard?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformNSDAutomaticOnBoarding2BugBody")
		.to("direct:bugzilla.newIssue");

		/**
		 * Create NSD offboard New Route
		 */
		from("seda:nsd.offboard?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformNSDAutomaticOffBoarding2BugBody")
		.to("direct:bugzilla.bugmanage");
		
		/**
		 * Automatic OnBoarding Route Success
		 */		
		from("seda:nsd.onboard.success?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSDAutomaticOnBoarding2BugBody")
		.process( BugHeaderExtractProcessor )
		.to("direct:bugzilla.updateIssue");

		
		/**
		 * Automatic OnBoarding Route Fail
		 */		
		from("seda:nsd.onboard.fail?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSDAutomaticOnBoarding2BugBody")
		.process( BugHeaderExtractProcessor )
		.to("direct:bugzilla.updateIssue");


		/**
		 * Automatic NS Instantiation Route Success
		 */		
		from("seda:nsd.deployment.instantiation.success?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSInstantiation2BugBody")
		.to("direct:bugzilla.bugmanage");	

		/**
		 * Automatic NS Termination Route Success
		 */		
		from("seda:nsd.deployment.termination.success?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSInstantiation2BugBody")
		.to("direct:bugzilla.bugmanage");	

		from("seda:nsd.deployment.termination.fail?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSInstantiation2BugBody")
		.to("direct:bugzilla.bugmanage");	

		/**
		 * OSM4 Communication
		 */		
		from("seda:communication.osm4.fail?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformOSM4CommunicationFail2BugBody")
		.to("direct:bugzilla.bugmanage");
		
		from("seda:communication.osm4.success?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformOSM4CommunicationSuccess2BugBody")
		.to("direct:bugzilla.bugmanage");
		
		/**
		 * OSM5 Communication
		 */		
		from("seda:communication.osm5.fail?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformOSM5CommunicationFail2BugBody")
		.to("direct:bugzilla.bugmanage");
		
		from("seda:communication.osm5.success?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformOSM5CommunicationSuccess2BugBody")
		.to("direct:bugzilla.bugmanage");
		
		/**
		 * NS Scheduling Route
		 */		
		from("seda:nsd.schedule?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSInstantiation2BugBody")
		.to("direct:bugzilla.bugmanage");	
		
		/**
		 * Automatic NS Instantiation Route Fail
		 */		
		from("seda:nsd.deployment.instantiation.fail?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSInstantiation2BugBody")
		.to("direct:bugzilla.bugmanage");	
				
		/**
		 * Automatic NS Termination Route Success
		 */		
		from("seda:nsd.instance.termination.success?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSTermination2BugBody")
		.to("direct:bugzilla.bugmanage");	

		/**
		 * Automatic NS Termination Route Fail
		 */		
		from("seda:nsd.instance.termination.fail?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSTermination2BugBody")
		.to("direct:bugzilla.bugmanage");	

		/**
		 * Automatic NS Deletion Route Success
		 */		
		from("seda:nsd.instance.deletion.success?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSDeletion2BugBody")
		.to("direct:bugzilla.bugmanage");	
		
		/**
		 * Automatic NS Deletion Route Fail
		 */		
		from("seda:nsd.instance.deletion.fail?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformNSDeletion2BugBody")
		.to("direct:bugzilla.bugmanage");	
		
		/**
		 * Reject Deployment Route Issue
		 */
		from("seda:nsd.deployment.reject?multipleConsumers=true")
		.delay(30000)		
		.bean( BugzillaClient.class, "transformDeployment2BugBody")
		.to("direct:bugzilla.bugmanage");		

		from("direct:issue.get")
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.GET))
		.toD( "https4://" + BUGZILLAURL + "/rest.cgi/bug/${header.uuid}?api_key="+ BUGZILLAKEY +"&throwExceptionOnFailure=true");
		
		
		/**
		 * dead Letter Queue if everything fails to connect
		 */
		from("direct:dlq_bugzilla")
		.setBody()
		.body(String.class)
		.to("stream:out");
		
	}

	Predicate issueExists = new Predicate() {
		
		@Override
		public boolean matches(Exchange exchange) {
			
			Bug aBug = exchange.getIn().getBody( Bug.class );
			Object m = null;
			try{
				FluentProducerTemplate template = exchange.getContext().createFluentProducerTemplate()
					.withHeader("uuid", aBug.getAliasFirst()  )
					.to( "direct:issue.get");
				m = template.request();
			}catch( CamelExecutionException e){
				logger.error( "issueExists: " + e.getMessage() );
				//e.printStackTrace();
			}
			
			if ( m != null )	
			{
				return true;
			}
			else {
				return false;
			}
			
		}
	};

	Processor BugHeaderExtractProcessor = new Processor() {
		
		@Override
		public void process(Exchange exchange) throws Exception {

			Map<String, Object> headers = exchange.getIn().getHeaders(); 
			Bug aBug = exchange.getIn().getBody( Bug.class ); 
		    headers.put("uuid", aBug.getAliasFirst()  );
		    exchange.getOut().setHeaders(headers);
		    
		    //copy Description to Comment
		    aBug.setComment( BugzillaClient.createComment( aBug.getDescription() ) );
		    //delete Description
		    aBug.setDescription( null );
		    aBug.setAlias( null ); //dont put any Alias		
		    aBug.setCc( null );
		    
		    exchange.getOut().setBody( aBug  );
		    // copy attachements from IN to OUT to propagate them
		    exchange.getOut().setAttachments(exchange.getIn().getAttachments());
			
		}
	};
	
	
	


	public class MyHttpClientConfigurer implements HttpClientConfigurer {

		@Override
		public void configureHttpClient(HttpClientBuilder hc) {
			try {
				SSLContext sslContext;
				sslContext = new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();

				//hc.setSSLContext(sslContext).setSSLHostnameVerifier(new NoopHostnameVerifier()).build();

				SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory( sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				hc.setSSLSocketFactory(sslConnectionFactory);
				Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				        .register("https", sslConnectionFactory)
				        .build();

				HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);

				hc.setConnectionManager(ccm);

			} catch (KeyManagementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	
}


