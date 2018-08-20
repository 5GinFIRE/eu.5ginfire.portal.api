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

package portal.api.mano;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;


public class MANORouteBuilder  extends RouteBuilder{
	
	public static void main(String[] args) throws Exception {
		//new Main().run(args);
		
		
		CamelContext tempcontext = new DefaultCamelContext();
		try {
		RouteBuilder rb = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from( "timer://getVNFRepoTimer?period=2000&repeatCount=3&daemon=true"  )
        		.log( "Will check VNF repo");
            }
        };
        tempcontext.addRoutes( rb);
        tempcontext.start();
        Thread.sleep(30000);
		} finally {			
			tempcontext.stop();
        }
		
		
		
		
	}
	

	public void configure() {
		
       
	}


}
