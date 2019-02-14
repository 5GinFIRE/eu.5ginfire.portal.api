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

import org.apache.camel.CamelContext;
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
                
                from( "timer://getNSDRepoTimer?period=2000&repeatCount=3&daemon=true"  )
        		.log( "Will check NSD repo");
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
		
		/**
		 * OnBoard New Added VxF
		 */		
		//We get the message here and we need to route it to the proper point.
		//If onboarding is successfull we need to send a Bugzilla message
		//If it is unsuccessful we need to send another Bugzilla message
		from("seda:vxf.onboard?multipleConsumers=true")
		.doTry()
		.bean(new MANOController(),"onBoardVxFToMANOProvider") //returns exception or nothing
		.log("VNFD Onboarded Successfully")
		.doCatch(Exception.class)
		.log("VNFD Onboarding failed!");

		from("seda:nsd.onboard?multipleConsumers=true")
		.doTry()
		.bean(new MANOController(),"onBoardNSDToMANOProvider") //returns exception or nothing
		.log("NSD Onboarded Successfully")
		.doCatch(Exception.class)
		.log("NSD Onboarding failed!");		

		
		from("seda:nsd.deploy?multipleConsumers=true")
		.doTry()
		.bean(new MANOController(),"deployNSDToMANOProvider") //returns exception or nothing
		.log("NS deployed Successfully").stop()
		.doCatch(Exception.class)
		.log("NS deployment failed!").stop();		

		from("seda:nsd.deployment.complete?multipleConsumers=true")
		.doTry()
		.bean(new MANOController(),"terminateNSFromMANOProvider") //returns exception or nothing
		.log("NS completed Successfully")
		.doCatch(Exception.class)
		.log("NS completion failed!").stop();
		
		//from("timer://checkAndDeployTimer?delay=2m&period=120000").bean(new MANOController(),"checkAndDeployExperimentToMANOProvider").stop();
		//from("timer://checkAndTerminateTimer?delay=2m&period=120000").bean(new MANOController(),"checkAndTerminateExperimentToMANOProvider").stop();
		
		from("timer://checkAndUpdateRunningDeploymentDescriptors?delay=2m&period=120000").bean(MANOController.class,"checkAndUpdateRunningDeploymentDescriptors").stop();
		
		
	}


}
