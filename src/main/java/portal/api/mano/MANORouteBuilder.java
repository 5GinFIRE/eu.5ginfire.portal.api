package portal.api.mano;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;


public class MANORouteBuilder  extends RouteBuilder{
	
	public static void main(String[] args) throws Exception {
		//new Main().run(args);
		
		CamelContext context = new DefaultCamelContext();
		try {
			ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&amp;broker.useJmx=true"); 
			context.addComponent("jms", ActiveMQComponent.jmsComponentAutoAcknowledge(connectionFactory));			

			context.addRoutes( new MANORouteBuilder() );
			context.start();
			
			//test new user
//			FluentProducerTemplate template = context.createFluentProducerTemplate().to("seda:users.create?multipleConsumers=true");
//			PortalUser owner = new PortalUser();
//			owner.setEmail( "tranoris@example.org" );
//			owner.setName( "Christos Tranoris");
//			template.withBody( owner ).asyncSend();		
//			
//			// test New Deployment
//			FluentProducerTemplate template = context.createFluentProducerTemplate().to("seda:deployments.create?multipleConsumers=true");
//			String uuid = "02b0b0d9-d73a-451f-8cb2-79d398a375b4"; //UUID.randomUUID().toString();
//			DeploymentDescriptor deployment = new DeploymentDescriptor( uuid , "An Experiment");
//			deployment.setDescription("test asfdsf\n test asfdsf\n test asfdsf\n");
//			PortalUser owner = new PortalUser();
//			owner.setUsername( "admin" );
//			owner.setEmail( "tranoris@ece.upatras.gr" );
//			deployment.setOwner(owner);
//			deployment.setDateCreated( new Date());
//			deployment.setStartReqDate( new Date());
//			deployment.setEndReqDate( new Date());
//			ExperimentMetadata exper = new ExperimentMetadata();
//			exper.setName( "An experiment NSD" ); 
//			deployment.setExperiment(exper);
//			template.withBody( deployment ).asyncSend();			
//
//            Thread.sleep(4000);
//
//			// test Update Deployment
//			FluentProducerTemplate templateUpd = context.createFluentProducerTemplate().to("seda:deployments.update?multipleConsumers=true");
//			//DeploymentDescriptor deployment = new DeploymentDescriptor( uuid, "An Experiment");
//			//deployment.setDescription("test asfdsf\n test asfdsf\n test asfdsf\n");
//			//PortalUser owner = new PortalUser();
//			//owner.setUsername( "admin" );
//			//owner.setEmail( "tranoris@ece.upatras.gr" );
//			//deployment.setOwner(owner);
//			//deployment.setDateCreated( new Date());
//			//deployment.setStartReqDate( new Date());
//			//deployment.setEndReqDate( new Date());
//			
//			deployment.setStatus( DeploymentDescriptorStatus.SCHEDULED );
//			deployment.setStartDate(  new Date() );
//			deployment.setEndDate(  new Date() );
//			deployment.setFeedback( "A feedback\n more feedback " );			
//			templateUpd.withBody( deployment ).asyncSend();
			
			
			
            Thread.sleep(60000);
		} finally {			
            context.stop();
        }
		
		
	}
	

	public void configure() {
		
        from("direct:onboardVxF")
        .log("Received Message is ${body} and Headers are ${headers}")
        .to("mock:output");
	}


}
