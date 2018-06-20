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

import portal.api.bugzilla.model.Bug;
import portal.api.bugzilla.model.Bugs;
import portal.api.model.DeploymentDescriptor;

/**
 * A simple example router from a file system to an ActiveMQ queue and then to a
 * file system
 *
 * @version
 */
public class BugzillaRouteBuilder extends RouteBuilder {

	private static final String BUGZILLAKEY = "VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z";
	private static String BUGZILLAURL = "portal.5ginfire.eu:443/bugstaging";
	

	//private static ModelCamelContext actx;

	public void configure() {

		//actx = this.getContext();

		HttpComponent httpComponent = getContext().getComponent("https4", HttpComponent.class);
		httpComponent.setHttpClientConfigurer(new MyHttpClientConfigurer());

		
		
		from("seda:deployments.create?multipleConsumers=true")
		.bean( BugzillaClient.class, "transformDeployment2BugBody")
		.convertBodyTo(String.class)
		.errorHandler(deadLetterChannel("direct:dlq")
				.maximumRedeliveries( 10 )
				.redeliveryDelay( 5000 ).useOriginalMessage()
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
		


		from("direct:dlq").setBody().body(String.class) .to("stream:out");
		
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
	
	
	
}


