package portal.api;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class MANORouteTest extends CamelTestSupport {
	
    @Override
    public RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
            	 from("direct:onboardVxF")
                 .log("Received Message is ${body} and Headers are ${headers}")
                 .to("mock:output");
            }
        };
    }

    @Test
    public void sampleMockTest() throws InterruptedException {

        String expected="Hello";

        /**
         * Producer Template.
         */
        MockEndpoint mock = getMockEndpoint("test1");
        mock.expectedBodiesReceived(expected);
        String input="Hello";
        template.sendBody("direct:sampleInput",input );
        assertMockEndpointsSatisfied();

    }

}
