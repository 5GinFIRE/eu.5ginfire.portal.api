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

package centralLog.api;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import portal.api.repo.PortalRepository;


public class CentralLoggerRouteBuilder  extends RouteBuilder{
	

	private static String CENTRALLOGGERURL = "";
	

	/** 
	 * every 1 hour post Components Status
	 */
	private static final int REFRESH_PERIOD = 60*60*1000;
	//private static final int REFRESH_PERIOD = 60*1000;
	/** */
	private static final transient Log logger = LogFactory.getLog( CentralLoggerRouteBuilder.class.getName());
	
	
	
	public void configure() {

		if (PortalRepository.getPropertyByName("centrallogerurl").getValue() != null) {
			CENTRALLOGGERURL = PortalRepository.getPropertyByName("centrallogerurl").getValue();
		}
		
		if ( ( CENTRALLOGGERURL == null ) || CENTRALLOGGERURL.equals( "" ) ){
			logger.info( "NO CENTRALLOGGERURL ROUTING. ELASTICURL = " + CENTRALLOGGERURL);
			return; //no routing towards Bugzilla
		}
		
		
	
		String url = CENTRALLOGGERURL.replace( "https://", "https4://").replace( "http://", "http4://") + "/log";

		from("seda:centralLog")	
        .setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
		.toD(  url  );
		
	
	       
	}




	

}
