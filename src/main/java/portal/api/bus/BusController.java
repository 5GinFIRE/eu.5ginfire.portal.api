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

import java.util.concurrent.Future;

import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.model.ModelCamelContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import portal.api.mano.MANOController;
import portal.api.mano.MANOStatus;
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


	private static final transient Log logger = LogFactory.getLog( MANOController.class.getName());


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
	 * 
	 * utility function to stop ProducerTemplate
	 * @param result
	 * @param template
	 */
	private void waitAndStopForTemplate(Future<Exchange> result, FluentProducerTemplate template) {
		while (true) {			
			if (result.isDone()) {
				logger.info( "waitAndStopForTemplate: " + template.toString() + " [STOPPED]");
				try {
					template.stop();
					template.clearAll();
					template.cleanUp();
					break;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			try {
				logger.info( "waitAndStopForTemplate: " + template.toString() + " [WAITING...]");
				Thread.sleep( 5000 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
	}
	
	/**
	 * Asynchronously sends to the routing bus (seda:users.create?multipleConsumers=true) that a new user is added
	 * @param user a {@link PortalUser}
	 */
	public void newUserAdded(int userid) {
		
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:users.create?multipleConsumers=true");
		template.withBody( userid ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:deployments.create?multipleConsumers=true) that a new user is added
	 * @param deployment a {@link DeploymentDescriptor}
	 */
	public void newDeploymentRequest(int deploymentdescriptorid) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:deployments.create?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:deployments.create?multipleConsumers=true) that a new user is added
	 * @param deployment a {@link DeploymentDescriptor}
	 */
	public void rejectDeploymentRequest(int deploymentdescriptorid) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:deployments.reject?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();
		
	}
	
	/**
	 * Asynchronously sends to the routing bus (seda:deployments.create?multipleConsumers=true) that a new user is added
	 * @param deployment a {@link DeploymentDescriptor}
	 */
	public void updateDeploymentRequest(int deploymentdescriptorid) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:deployments.update?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:vxf.create?multipleConsumers=true) that a new vxf is added
	 * @param deployment a {@link VxFMetadata}
	 */
	// NOT USED
	public void newVxFAdded(long vxfmetadataid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.create?multipleConsumers=true");
		template.withBody( vxfmetadataid ).asyncSend();				
	}

	/**
	 * Asynchronously sends to the routing bus (seda:vxf.onboard?multipleConsumers=true) to upload a new vxf
	 * @param deployment a {@link VxFMetadata}
	 */
	public void onBoardVxFAdded(int vxfobdid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.onboard?multipleConsumers=true");
		template.withBody( vxfobdid ).asyncSend();				
	}

	public void onBoardVxFFailed(int vxfobdid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.onboard.fail?multipleConsumers=true");
		template.withBody( vxfobdid ).asyncSend();			
	}

	public void onBoardVxFSucceded(int vxfobdid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.onboard.success?multipleConsumers=true");
		template.withBody( vxfobdid ).asyncSend();				
	}
	
	/**
	 * Asynchronously sends to the routing bus (seda:nsd.create?multipleConsumers=true) that a new NSD experiment is added
	 * @param deployment a {@link ExperimentMetadata}
	 */
	// There is no listener for nsd.create
	public void newNSDAdded(long experimentmetadataid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.create?multipleConsumers=true");
		template.withBody( experimentmetadataid ).asyncSend();		
	}

	public void onBoardNSDFailed(int uexpobdid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.onboard.fail?multipleConsumers=true");
		template.withBody( uexpobdid ).asyncSend();			
	}

	public void onBoardNSDSucceded(int uexpobdid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.onboard.success?multipleConsumers=true");
		template.withBody( uexpobdid ).asyncSend();				
	}
	

	public void scheduleExperiment(int deploymentdescriptorid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.schedule?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();				
	}
	

	public void deployExperiment(int deploymentdescriptorid) {		
		logger.info( "deployExperiment: to(\"seda:nsd.deploy?multipleConsumers=true\")");		
		FluentProducerTemplate	template = actx.createFluentProducerTemplate().to("seda:nsd.deploy?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();	
	}

	// seda:nsd.deployment
	
	public void deploymentInstantiationSucceded(int deploymentdescriptorid)
	{
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.deployment.instantiation.success?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}
	
	public void deploymentInstantiationFailed(int deploymentdescriptorid)
	{
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.deployment.instantiation.fail?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}

	public void deploymentTerminationSucceded(int deploymentdescriptorid)
	{
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.deployment.termination.success?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}
	
	public void deploymentTerminationFailed(int deploymentdescriptorid)
	{
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.deployment.termination.fail?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}
	
	public void completeExperiment(int deploymentdescriptorid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.deployment.complete?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();				
	}

	public void deleteExperiment(int deploymentdescriptorid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.deployment.delete?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}

	public void rejectExperiment(int deploymentdescriptorid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.deployment.reject?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();				
	}

	public void osm4CommunicationFailed(Class<MANOStatus> manostatus)
	{
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:communication.osm4.fail?multipleConsumers=true");
		template.withBody(manostatus).asyncSend();						
	}

	public void osm4CommunicationRestored(Class<MANOStatus> manostatus)
	{
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:communication.osm4.success?multipleConsumers=true");
		template.withBody(manostatus).asyncSend();						
	}
	
	public void osm5CommunicationFailed(Class<MANOStatus> manostatus)
	{
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:communication.osm5.fail?multipleConsumers=true");
		template.withBody(manostatus).asyncSend();						
	}

	public void osm5CommunicationRestored(Class<MANOStatus> manostatus)
	{
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:communication.osm5.success?multipleConsumers=true");
		template.withBody(manostatus).asyncSend();						
	}
	
	public void terminateInstanceSucceded(int deploymentdescriptorid)
	{
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.instance.termination.success?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}

	public void terminateInstanceFailed(int deploymentdescriptorid)
	{
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.instance.termination.fail?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();						
	}

	public void deleteInstanceSucceded(int deploymentdescriptorid) {
		// TODO Auto-generated method stub
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.instance.deletion.success?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();								
	}
		
	public void deleteInstanceFailed(int deploymentdescriptorid) {
		// TODO Auto-generated method stub
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.instance.deletion.fail?multipleConsumers=true");
		template.withBody( deploymentdescriptorid ).asyncSend();								
	}
		
	/**
	 * Asynchronously sends to the routing bus (seda:vxf.update?multipleConsumers=true) that a vxf is updated
	 * @param deployment a {@link VxFMetadata}
	 */
	// There is no listener for this
	public void updatedVxF(long vxfmetadataid) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.update?multipleConsumers=true");
		template.withBody( vxfmetadataid ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.update?multipleConsumers=true) that a  NSD experiment is updated
	 * @param experiment a {@link ExperimentMetadata}
	 */
	// There is no listener for this
	public void updateNSD(long experimentmetadataid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.update?multipleConsumers=true");
		template.withBody( experimentmetadataid ).asyncSend();		
	}

	
	/**
	 * Asynchronously sends to the routing bus (seda:vxf.new.validation?multipleConsumers=true)to trigger VxF validation
	 * @param vxf a {@link VxFMetadata}
	 */
	public void validateVxF(long vxfmetadataid) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.new.validation?multipleConsumers=true");
		template.withBody( vxfmetadataid ).asyncSend();
		
	}
		
	/**
	 * Asynchronously sends to the routing bus (seda:vxf.validationresult.update?multipleConsumers=true)to trigger update VxF validation
	 * @param vresult  a {@link ValidationJobResult}
	 */
	public void updatedValidationJob(long vxfmetadataid) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.validationresult.update?multipleConsumers=true");
		template.withBody( vxfmetadataid ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.validate.new?multipleConsumers=true) to trigger NSD validation
	 * @param deployment a {@link ExperimentMetadata}
	 */
	// There is no listener for this
	public void validateNSD(long experimentmetadataid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.validate.new?multipleConsumers=true");
		template.withBody( experimentmetadataid ).asyncSend();		
	}
	
	
	/**
	 * Asynchronously sends to the routing bus (seda:nsd.validate.update?multipleConsumers=true) to trigger NSD validation
	 * @param deployment a {@link ExperimentMetadata}
	 */
	// There is no listener for this
	public void validationUpdateNSD(long experimentmetadataid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.validate.update?multipleConsumers=true");
		template.withBody( experimentmetadataid ).asyncSend();		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:vxf.deleted?multipleConsumers=true) that a vxf is deleted
	 * @param deployment a {@link VxFMetadata}
	 */
	// There is no listener for this
	public void deletedVxF(long vxfmetadataid) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.deleted?multipleConsumers=true");
		template.withBody( vxfmetadataid ).asyncSend();
		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.deleted?multipleConsumers=true) that a vxf is deleted
	 * @param deployment a {@link ExperimentMetadata}
	 */
	// There is no listener for this
	public void deletedExperiment(long experimentmetadataid) {

		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.deleted?multipleConsumers=true");
		template.withBody( experimentmetadataid ).asyncSend();
		
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
	//There is no listener for this
	public void onBoardVxF(long vxfobdsid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:mano.onboard.vxf?multipleConsumers=true");
		template.withBody( vxfobdsid ).asyncSend();
	}

	/**
	 * Asynchronously sends to the routing bus (seda:vxf.offboard?multipleConsumers=true) to trigger new VXF offboarding 
	 * @param deployment a {@link VxFOnBoardedDescriptor}
	 */
	public void offBoardVxF(int vxfobdsid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:vxf.offboard?multipleConsumers=true");
		template.withBody( vxfobdsid ).asyncSend();		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.onboard?multipleConsumers=true) to trigger new NSD onboarding 
	 * @param deployment a {@link ExperimentOnBoardDescriptor}
	 */
	public void onBoardNSD(int uexpobdid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.onboard?multipleConsumers=true");
		template.withBody( uexpobdid ).asyncSend();		
	}

	/**
	 * Asynchronously sends to the routing bus (seda:nsd.offboard?multipleConsumers=true) to trigger new NSD offboarding 
	 * @param deployment a {@link ExperimentOnBoardDescriptor}
	 */
	public void offBoardNSD(int uexpobdid) {
		FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:nsd.offboard?multipleConsumers=true");
		template.withBody( uexpobdid ).asyncSend();		
	}
}
