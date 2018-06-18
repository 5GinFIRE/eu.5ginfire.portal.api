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
package portal.api.routes;


import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.UUID;

import javax.net.ssl.SSLContext;

import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spring.Main;
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

import portal.api.bugzilla.model.Bugs;

/**
 * A simple example router from a file system to an ActiveMQ queue and then to a
 * file system
 *
 * @version
 */
public class MyRouteBuilder extends RouteBuilder {

	/**
	 * Allow this route to be run as an application
	 */
	public static void main(String[] args) throws Exception {
		new Main().run(args);
	}

	private static ModelCamelContext actx;

	public void configure() {

		actx = this.getContext();

		HttpComponent httpComponent = getContext().getComponent("https4", HttpComponent.class);
		httpComponent.setHttpClientConfigurer(new MyHttpClientConfigurer());
		
		

		// populate the message queue with some messages
		from("file:src/data?noop=true").to("jms:test.MyQueue");

		from("jms:test.MyQueue")
		.to("file://target/test");

		// set up a listener on the file component
		from("file://target/test?noop=true")
		.bean(new SomeBean());

		from("direct:start")
		.toD( "https4://portal.5ginfire.eu:443/bugzilla/rest.cgi/bug/${header.id}?throwExceptionOnFailure=false");
		

		from("direct:IssueByAlias")
		.toD( "https4://portal.5ginfire.eu:443/bugzilla/rest.cgi/bug?alias=${header.alias}&throwExceptionOnFailure=false");
		
		from("direct:newIssue")
		.setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
		//.setHeader("X-BUGZILLA-API-KEY", constant("VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z") )
		.toD( "https4://portal.5ginfire.eu:443/bugzilla/rest.cgi/bug?api_key=VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z&throwExceptionOnFailure=false");
		

	}

	public static class SomeBean {

		public void someMethod(String body) {
			System.out.println("Received: " + body);

			System.out.println("==========GET A BUG==================");
			FluentProducerTemplate template = actx.createFluentProducerTemplate().to("direct:start");

			String result = template
					.withHeader("id", "1")										
					.request(String.class);
			System.out.println("Received: " + result);

			System.out.println("============================");


			System.out.println("==========GET A BUG By ALIAS==================");
			template = actx.createFluentProducerTemplate().to("direct:IssueByAlias");

			result = template
					.withHeader("alias", "64a52a4d-526d-49eb-9f30-38f9f40c5d55")					
					.request(String.class);
			System.out.println("Received: " + result);
			
			
			String response = result;
			ObjectMapper mapper = new ObjectMapper(new JsonFactory());

			try {
				Bugs b = mapper.readValue(response.toString(), Bugs.class);

				if ((b != null) && (b.getBugs() != null) && (b.getBugs().size() > 0)) {
					
					System.out.println("=============== b.getBugs().get(0) = " + b.getBugs().get(0).getId() );
				}

			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("============================");
			
			
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
}
