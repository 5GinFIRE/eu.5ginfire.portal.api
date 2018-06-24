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

package portal.api.routes;


import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.jms.ConnectionFactory;
import javax.net.ssl.SSLContext;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spring.Main;
import org.apache.camel.support.SynchronizationAdapter;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import portal.api.bugzilla.BugzillaRouteBuilder;
import portal.api.bugzilla.model.Bug;
import portal.api.bugzilla.model.Bugs;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.PortalUser;

/**
 * A simple example router from a file system to an ActiveMQ queue and then to a
 * file system
 *
 * @version
 */
public class MyRouteBuilder extends RouteBuilder {

	private static String BUGZILLAURL = "portal.5ginfire.eu:443/bugstaging";
	
	static Future<String> resultAsynReply;
	/**
	 * Allow this route to be run as an application
	 */
	public static void main(String[] args) throws Exception {
		//new Main().run(args);
		
		CamelContext context = new DefaultCamelContext();
		try {
			ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&amp;broker.useJmx=true"); 
			context.addComponent("jms", ActiveMQComponent.jmsComponentAutoAcknowledge(connectionFactory));			


			context.addRoutes( new MyRouteBuilder() );			
			context.start();
						
			AProcess sb = new AProcess();			
			sb.exampleProcess("a body"); 
			sb.exampleSeda( "seda example" );	
//
			System.out.println("Received BUG resultAsynReply: " + resultAsynReply.get()  );

			
			
            Thread.sleep(60000);
		} finally {			
            context.stop();
        }
		
		
	}

	private static ModelCamelContext actx;

	public void configure() {

		actx = this.getContext();

		HttpComponent httpComponent = getContext().getComponent("https4", HttpComponent.class);
		httpComponent.setHttpClientConfigurer(new MyHttpClientConfigurer());
		
//		// populate the message queue with some messages
//		from("file:src/data?noop=true").to("jms:test.MyQueue");
//		from("jms:test.MyQueue")
//		.to("file://target/test");
//		// set up a listener on the file component
//		from("file://target/test?noop=true")
//		.bean(new AProcess());

		
		
		
		from("direct:start")
		.errorHandler(deadLetterChannel("direct:dlq")
				.maximumRedeliveries( 10 )
				.redeliveryDelay( 5000 ).useOriginalMessage()
				.deadLetterHandleNewException( false )
				//.logExhaustedMessageHistory(false)
				.logExhausted(true)
				.logHandled(true)
				//.retriesExhaustedLogLevel(LoggingLevel.WARN)
				.retryAttemptedLogLevel( LoggingLevel.WARN) )
		.convertBodyTo(String.class)
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.GET))
		.threads( 10 )
		.toD( "https4://" + BUGZILLAURL + "/rest.cgi/bug/${body}?throwExceptionOnFailure=true") ;
		
		
		
		from("direct:dlq").setBody().body(String.class) .to("stream:out");
		
		from("direct:IssueByAlias")
		.toD( "https4://" + BUGZILLAURL + "/rest.cgi/bug?alias=${header.alias}&throwExceptionOnFailure=false")
		.bean( ABug.class , "aMethod" )
		.bean( ABug.class, "getBugId")
		.setHeader( "id" ).body()  //now post a comment to that bug
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
		.setBody()
		.simple(  "{  \"comment\" : \"a comment\" }" )
		.toD( "https4://" + BUGZILLAURL + "/rest.cgi/bug/${header.id}/comment?api_key=VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z&throwExceptionOnFailure=false")
		.to("stream:out");
//		.bean( ABug.class , "aMethod" )
//		.to("stream:out");
		
		from("direct:newIssue")
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
		//.setHeader("X-BUGZILLA-API-KEY", constant("VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z") )
		.toD( "https4://" + BUGZILLAURL + "/rest.cgi/bug?api_key=VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z&throwExceptionOnFailure=false");
		
		
		from("seda:users?multipleConsumers=true")
		.setBody()
		.simple(  "route A" )
		.to("stream:out");
		
		
		from("seda:users?multipleConsumers=true")
		.setBody()
		.simple(  "route B" )
		.to("stream:out");
		
		//from("timer://myTimer?period=5000").setBody().simple("Hello World Camel fired at ${header.firedTime}").bean(new SomeBean());

	}

	public static class AProcess {

		public void exampleProcess(String body) {
			System.out.println("Body in SomeBean: " + body);

			System.out.println("========== AProcess GET A BUG ==================");
			FluentProducerTemplate template = actx.createFluentProducerTemplate(). to("direct:start");

//			String result = template
//					.withHeader("id", "1")										
//					.request(String.class);
			//just send and forget
			Future<Exchange> asyncres = template
					.withHeader("id", "1").withBody("1")										
					.asyncSend();
			
			System.out.println("Received BUG  asyncSend (nothing yet): " + asyncres.toString() );
			
			//just send continue processing. Get reply elsewhere. Get is blocking then
			FluentProducerTemplate templateAR = actx.createFluentProducerTemplate(). to("direct:start");
			resultAsynReply = templateAR
					.withHeader("id", "1").withBody("1")										
					.asyncRequest( String.class);
			System.out.println("Received BUG  asyncSend (nothing yet): " + resultAsynReply.toString() );
			
			//let's get it with a callback
			ProducerTemplate template2 = actx.createProducerTemplate();
			MyCallback callback = new MyCallback();
			template2.asyncCallbackRequestBody( "direct:start", "64a52a4d-526d-49eb-9f30-38f9f40c5d55", callback);
			
			System.out.println("========== END AProcess ==================");


//			System.out.println("==========GET A BUG By ALIAS==================");
//			template = actx.createFluentProducerTemplate().to("direct:IssueByAlias");
//
//			result = template
//					.withHeader("alias", "64a52a4d-526d-49eb-9f30-38f9f40c5d55")					
//					.request(String.class);
//			System.out.println("Received: " + result);
			
			
			
			
			
//			String response = result;
//			ObjectMapper mapper = new ObjectMapper(new JsonFactory());
//
//			try {
//				Bugs b = mapper.readValue(response.toString(), Bugs.class);
//
//				if ((b != null) && (b.getBugs() != null) && (b.getBugs().size() > 0)) {
//					
//					System.out.println("=============== b.getBugs().get(0) = " + b.getBugs().get(0).getId() );
//				}
//
//			} catch (JsonParseException e) {
//				e.printStackTrace();
//			} catch (JsonMappingException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//
//			System.out.println("============================");
			
			
//			System.out.println("==========POST NEW BUG==================");
//			template = actx.createFluentProducerTemplate().to("direct:newIssue");
//
//			String alias =  UUID.randomUUID().toString();
//			result = template
//					.withHeader("id", "1")
//					.withHeader("X-BUGZILLA-API-KEY", "VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z" )
//					.withBody( "{  \"product\" : \"Staging\", \"component\" : \"Staging component\",  \"version\" : \"unspecified\",  \"summary\" : \"This is a test bug - please disregard " +  alias +  "\""
//							+ ",  \"description\" : \"This is a test bug - please disregard " +  alias +  "\""
//							+ ",  \"alias\" : \"" +  alias +  "\""
//							+ ",  \"cc\" : \"tranoris@ieee.org\"" 
//							+  "}" )
//					.request(String.class);
//			System.out.println("POST result Received: " + result);
//
//			System.out.println("============================");
			
	
			
//			System.out.println("==========POST NEW Dpeployment BUG==================");
//			template = actx.createFluentProducerTemplate().to("direct:newIssue");
//
//			String alias =  "626d2f28-78ad-4ff6-881b-0b27c57129b1";
//			String result = template
//					.withHeader("id", "1")
//					.withHeader("X-BUGZILLA-API-KEY", "VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z" )
//					.withBody( "{  \"product\" : \"5GinFIRE Operations\", \"component\" : \"Operations Support\",  \"version\" : \"unspecified\",  \"summary\" : \"[BYPORTAL] New Deployment Request " +  alias +  "\""
//							+ ",  \"description\" : \"By owner admin " +  alias +  "\""
//							+ ",  \"alias\" : \"" +  alias +  "\""
//							+  "}" )
//					.request(String.class);
//			System.out.println("POST result Received: " + result);

			
			
			System.out.println("============================");
			
		}
		
		public void exampleSeda(String body) {
			System.out.println("========= send exampleSeda ==================");
			FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:users?multipleConsumers=true");
			String result = template.request(String.class);
			System.out.println("Received from seda request : " + result);
		}
	}



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
	
	public static class ABug {

		public static Bug aMethod(String body) {

			String response = body;
			ObjectMapper mapper = new ObjectMapper(new JsonFactory());

			try {
				Bugs b = mapper.readValue(response.toString(), Bugs.class);

				if ((b != null) && (b.getBugs() != null) && (b.getBugs().size() > 0)) {
					
					System.out.println("=============== b.getBugs().get(0) = " + b.getBugs().get(0).getId() );
					return b.getBugs().get(0);
				}

			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;

		}
		
		public static String getBugId(Bug b) {

			if (b != null){
				return b.getId() + "";
			}
			
			return null;
		}
	}
	
	
	
	/**
     * Our own callback that will gather all the responses.
     * We extend the SynchronizationAdapter class as we then only need to override the onComplete method.
     */
    private static class MyCallback extends SynchronizationAdapter {

        // below the String elements are added in the context of different threads so that we should make
        // sure that this's done in a thread-safe manner, that's no two threads should call the data.add()
        // method below concurrently, so why we use Vector here and not e.g. ArrayList
        private final List<String> data = new Vector<>();

        @Override
        public void onComplete(Exchange exchange) {
            // this method is invoked when the exchange was a success and we can get the response
            String body = exchange.getOut().getBody(String.class);
            System.out.println("MyCallback onComplete  = " + body );

        }

    }
// END SNIPPET: e2
}


