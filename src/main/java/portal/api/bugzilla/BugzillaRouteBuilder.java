/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package portal.api.bugzilla;


import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.jms.ConnectionFactory;
import javax.net.ssl.SSLContext;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import portal.api.bugzilla.model.Bug;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.DeploymentDescriptorStatus;
import portal.api.model.ExperimentMetadata;
import portal.api.model.PortalUser;
import portal.api.routes.MyRouteBuilder.ABug;

/**
 * A simple example router from a file system to an ActiveMQ queue and then to a
 * file system
 *
 * @version
 */
public class BugzillaRouteBuilder extends RouteBuilder {

	private static String BUGZILLAKEY = "VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z";
	private static String BUGZILLAURL = "portal.5ginfire.eu:443/bugstaging";
	

	//private static ModelCamelContext actx;

	public static void main(String[] args) throws Exception {
		//new Main().run(args);
		
		CamelContext context = new DefaultCamelContext();
		try {
			ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&amp;broker.useJmx=true"); 
			context.addComponent("jms", ActiveMQComponent.jmsComponentAutoAcknowledge(connectionFactory));			

			context.addRoutes( new BugzillaRouteBuilder() );
			context.start();

			
			// test New Deployment
			FluentProducerTemplate template = context.createFluentProducerTemplate().to("seda:deployments.create?multipleConsumers=true");
			String uuid = "02b0b0d9-d73a-451f-8cb2-79d398a375b4"; //UUID.randomUUID().toString();
			DeploymentDescriptor deployment = new DeploymentDescriptor( uuid , "An Experiment");
			deployment.setDescription("test asfdsf\n test asfdsf\n test asfdsf\n");
			PortalUser owner = new PortalUser();
			owner.setUsername( "admin" );
			owner.setEmail( "tranoris@ece.upatras.gr" );
			deployment.setOwner(owner);
			deployment.setDateCreated( new Date());
			deployment.setStartReqDate( new Date());
			deployment.setEndReqDate( new Date());
			ExperimentMetadata exper = new ExperimentMetadata();
			exper.setName( "An experiment NSD" ); 
			deployment.setExperiment(exper);
			//template.withBody( deployment ).asyncSend();
			

            Thread.sleep(4000);

			// test Update Deployment
			FluentProducerTemplate templateUpd = context.createFluentProducerTemplate().to("seda:deployments.update?multipleConsumers=true");
			//DeploymentDescriptor deployment = new DeploymentDescriptor( uuid, "An Experiment");
			//deployment.setDescription("test asfdsf\n test asfdsf\n test asfdsf\n");
			//PortalUser owner = new PortalUser();
			//owner.setUsername( "admin" );
			//owner.setEmail( "tranoris@ece.upatras.gr" );
			//deployment.setOwner(owner);
			//deployment.setDateCreated( new Date());
			//deployment.setStartReqDate( new Date());
			//deployment.setEndReqDate( new Date());
			
			deployment.setStatus( DeploymentDescriptorStatus.SCHEDULED );
			deployment.setStartDate(  new Date() );
			deployment.setEndDate(  new Date() );
			deployment.setFeedback( "A feedback\n more feedback " );			
			templateUpd.withBody( deployment ).asyncSend();
			
			
			
            Thread.sleep(10000);
		} finally {			
            context.stop();
        }
		
		
	}

	
	public void configure() {

		ModelCamelContext actx = this.getContext();

		HttpComponent httpComponent = getContext().getComponent("https4", HttpComponent.class);
		httpComponent.setHttpClientConfigurer(new MyHttpClientConfigurer());

		
		/**
		 * Create Deployment Route
		 */
		from("seda:deployments.create?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformDeployment2BugBody")
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo( String.class ).to("stream:out")
		.errorHandler(deadLetterChannel("direct:dlq_deployments")
				.maximumRedeliveries( 120 ) //let's try for the next 2 hours to send it....
				.redeliveryDelay( 60000 ).useOriginalMessage()
				.deadLetterHandleNewException( false )
				//.logExhaustedMessageHistory(false)
				.logExhausted(true)
				.logHandled(true)
				//.retriesExhaustedLogLevel(LoggingLevel.WARN)
				.retryAttemptedLogLevel( LoggingLevel.WARN) )
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
		//.setHeader("X-BUGZILLA-API-KEY", constant( BUGZILLAKEY ) )
		.toD( "https4://" + BUGZILLAURL + "/rest.cgi/bug?api_key="+ BUGZILLAKEY +"&throwExceptionOnFailure=true")
		.to("stream:out");
		
		
		/**
		 * Update Deployment Route
		 */
		from("seda:deployments.update?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformDeployment2BugBody")
		.process( BugDeploymentUpdateProcessor )
		.marshal().json( JsonLibrary.Jackson, true)
		.convertBodyTo( String.class ).to("stream:out")
		.errorHandler(deadLetterChannel("direct:dlq")
				.maximumRedeliveries( 120 ) //let's try for the next 2 hours to send it....
				.redeliveryDelay( 60000 ).useOriginalMessage()
				.deadLetterHandleNewException( false )
				//.logExhaustedMessageHistory(false)
				.logExhausted(true)
				.logHandled(true)
				//.retriesExhaustedLogLevel(LoggingLevel.WARN)
				.retryAttemptedLogLevel( LoggingLevel.WARN) )
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.PUT))
		//.setHeader("X-BUGZILLA-API-KEY", constant( BUGZILLAKEY ) )		
		.toD( "https4://" + BUGZILLAURL + "/rest.cgi/bug/${header.uuid}?api_key="+ BUGZILLAKEY +"&throwExceptionOnFailure=true")
		.to("stream:out");

		//dead Letter Queue if everything fails to connect
		from("direct:dlq_deployments")
		.setBody()
//		.body(DeploymentDescriptor.class)
//		.bean( BugzillaClient.class, "transformDeployment2BugBody")
		.body(String.class)
		.to("stream:out");
		
	}

	

	Processor BugDeploymentUpdateProcessor = new Processor() {
		
		@Override
		public void process(Exchange exchange) throws Exception {

			Map<String, Object> headers = exchange.getIn().getHeaders(); 
			Bug desc = exchange.getIn().getBody( Bug.class ); 
		    headers.put("uuid", desc.getAliasFirst()  );
		    exchange.getOut().setHeaders(headers);
		    
		    //copy Description to Comment
		    desc.setComment( BugzillaClient.createComment( desc.getDescription() ) );
		    //delete Description
		    desc.setDescription( null );
		    desc.setAlias( null ); //dont put any Alias		    
		    //desc.setStatus( "IN_PROGRESS" );
		    		    
//		    desc.setProduct( null );
//		    desc.setVersion( null );
//		    desc.setSummary( null );
//		    desc.setComponent( null);
		    desc.setCc( null );
		    
		    exchange.getOut().setBody( desc  );
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


