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
