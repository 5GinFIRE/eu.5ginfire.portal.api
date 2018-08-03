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

package portal.api.bus;

import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.model.ModelCamelContext;

import portal.api.model.DeploymentDescriptor;
import portal.api.model.ExperimentMetadata;
import portal.api.model.ExperimentOnBoardDescriptor;
import portal.api.model.PortalUser;
import portal.api.model.VFImage;
import portal.api.model.VxFMetadata;
import portal.api.model.VxFOnBoardedDescriptor;
import portal.api.validation.ci.ValidationJobResult;

/**
 * Exposes messages to Bus. Usually they should be aynchronous.
 * Consult http://camel.apache.org/uris.html for URIs
 * sendmessage(direct:mplampla) is Synchronous in same Context
 * sendmessage(seda:mplampla) is aSynchronous in same Context
 *  * 
 * @author ctranoris
 * 
 * 
 *
 */
public class BusController {

	/** */
	private static BusController instance;
	
	/** the Camel Context configure via Spring. See bean.xml*/	
	private static ModelCamelContext actx;



	/**
	 * @return
	 */
	public static synchronized BusController getInstance() {
		if (instance == null) {
			instance = new BusController();
		}
		return instance;
	}
	

	/**
	 * @return
	 */
	public static ModelCamelContext getActx() {
		return actx;
	}

	/**
	 * @param actx
	 */
	public static void setActx(ModelCamelContext actx) {
		BusController.actx = actx;
	}

	/**
	 * Asynchronously sends to the routing bus (seda:users.create?multipleConsumers=true) that a new user is added
	 * @param u a {@link PortalUser}
	 */
	public void newUserAdded(PortalUser u) {
		
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:users.create?multipleConsumers=true");
		template.withBody( u ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:deployments.create?multipleConsumers=true) that a new user is added
	 * @param deployment a {@link DeploymentDescriptor}
	 */
	public void newDeploymentRequest(DeploymentDescriptor deployment) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:deployments.create?multipleConsumers=true");
		template.withBody( deployment ).asyncSend();
		
	}
	
	/**
	 * Asynchronously sends to the routing bus (seda:deployments.create?multipleConsumers=true) that a new user is added
	 * @param deployment a {@link DeploymentDescriptor}
	 */
	public void updateDeploymentRequest(DeploymentDescriptor deployment) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:deployments.update?multipleConsumers=true");
		template.withBody( deployment ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:vxf.create?multipleConsumers=true) that a new vxf is added
	 * @param deployment a {@link VxFMetadata}
	 */
	public void newVxFAdded(VxFMetadata vxf) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.create?multipleConsumers=true");
		template.withBody( vxf ).asyncSend();		
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.create?multipleConsumers=true) that a new NSD experiment is added
	 * @param deployment a {@link ExperimentMetadata}
	 */
	public void newNSDAdded(ExperimentMetadata experiment) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.create?multipleConsumers=true");
		template.withBody( experiment ).asyncSend();		
	}
	
	
	/**
	 * Asynchronously sends to the routing bus (seda:vxf.update?multipleConsumers=true) that a vxf is updated
	 * @param deployment a {@link VxFMetadata}
	 */
	public void updatedVxF(VxFMetadata vxf) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.update?multipleConsumers=true");
		template.withBody( vxf ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.update?multipleConsumers=true) that a  NSD experiment is updated
	 * @param experiment a {@link ExperimentMetadata}
	 */
	public void updateNSD(ExperimentMetadata experiment) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.update?multipleConsumers=true");
		template.withBody( experiment ).asyncSend();		
	}

	
	/**
	 * Asynchronously sends to the routing bus (seda:vxf.new.validation?multipleConsumers=true)to trigger VxF validation
	 * @param vxf a {@link VxFMetadata}
	 */
	public void validateVxF(VxFMetadata vxf) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.new.validation?multipleConsumers=true");
		template.withBody( vxf ).asyncSend();
		
	}
		
	/**
	 * Asynchronously sends to the routing bus (seda:vxf.validationresult.update?multipleConsumers=true)to trigger update VxF validation
	 * @param vresult  a {@link ValidationJobResult}
	 */
	public void updatedValidationJob(VxFMetadata vxf) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.validationresult.update?multipleConsumers=true");
		template.withBody( vxf ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.validate.new?multipleConsumers=true) to trigger NSD validation
	 * @param deployment a {@link ExperimentMetadata}
	 */
	public void validateNSD(ExperimentMetadata experiment) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.validate.new?multipleConsumers=true");
		template.withBody( experiment ).asyncSend();		
	}
	
	
	/**
	 * Asynchronously sends to the routing bus (seda:nsd.validate.update?multipleConsumers=true) to trigger NSD validation
	 * @param deployment a {@link ExperimentMetadata}
	 */
	public void validationUpdateNSD(ExperimentMetadata experiment) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.validate.update?multipleConsumers=true");
		template.withBody( experiment ).asyncSend();		
	}



	/**
	 * Asynchronously sends to the routing bus (seda:vxf.deleted?multipleConsumers=true) that a vxf is deleted
	 * @param deployment a {@link VxFMetadata}
	 */
	public void deletedVxF(VxFMetadata vxf) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.deleted?multipleConsumers=true");
		template.withBody( vxf ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.deleted?multipleConsumers=true) that a vxf is deleted
	 * @param deployment a {@link ExperimentMetadata}
	 */
	public void deletedExperiment(ExperimentMetadata nsd) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.deleted?multipleConsumers=true");
		template.withBody( nsd ).asyncSend();
		
	}


	/**
	 * @param vfimg
	 */
	public void newVFImageAdded(VFImage vfimg) {
		// TODO Auto-generated method stub
		
	}


	public void aVFImageUpdated(VFImage vfimg) {
		// TODO Auto-generated method stub
		
	}

	
	
	

	/**
	 * Asynchronously sends to the routing bus (seda:mano.onboard.vxf?multipleConsumers=true) to trigger new VXF onboarding to target MANOs that
	 * can support this VNF OSM version
	 * @param deployment a {@link VxFOnBoardedDescriptor}
	 */
	public void onBoardVxF(VxFOnBoardedDescriptor vxfobds) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:mano.onboard.vxf?multipleConsumers=true");
		template.withBody( vxfobds ).asyncSend();
	}

	/**
	 * Asynchronously sends to the routing bus (seda:vxf.offboard?multipleConsumers=true) to trigger new VXF offboarding 
	 * @param deployment a {@link VxFOnBoardedDescriptor}
	 */
	public void offBoardVxF(VxFOnBoardedDescriptor vxfobds) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.offboard?multipleConsumers=true");
		template.withBody( vxfobds ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.onboard?multipleConsumers=true) to trigger new NSD onboarding 
	 * @param deployment a {@link ExperimentOnBoardDescriptor}
	 */
	public void onBoardNSD(ExperimentOnBoardDescriptor uexpobd) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.onboard?multipleConsumers=true");
		template.withBody( uexpobd ).asyncSend();		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.onboard?multipleConsumers=true) to trigger new NSD offboarding 
	 * @param deployment a {@link ExperimentOnBoardDescriptor}
	 */
	public void offBoardNSD(ExperimentOnBoardDescriptor uexpobd) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.offboard?multipleConsumers=true");
		template.withBody( uexpobd ).asyncSend();		
	}
	
}
