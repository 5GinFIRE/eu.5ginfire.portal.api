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

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.model.ModelCamelContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author ctranoris
 *
 */
public class CentralLogger {
	
	/** the Camel Context configure via Spring. See bean.xml*/	
	private static ModelCamelContext actx;
	
	/**
	 * @param cl
	 * @param amessage
	 * @param componentName
	 */
	public static void log(CLevel cl, String amessage){
	
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("time", Instant.now().toString());
			map.put("clevel", cl.toString() );
			map.put("message", amessage );
			

			String json;
			try {
				json = new ObjectMapper().writeValueAsString(map);
				//System.out.println(json);
				FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:centralLog?multipleConsumers=true");
				template.withBody( json ).asyncSend();

			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
	}
	
	/**
	 * @param values
	 * @param componentName
	 */
	public static void simpleMon( String values, String componentName){
		
	

		String json;
		try {

			HashMap<String, String> map =
			        new ObjectMapper().readValue( values, HashMap.class);

			map.put("time", Instant.now().toString());
			map.put("component", componentName );
			
			json = new ObjectMapper().writeValueAsString(map);
			//System.out.println(json);
			FluentProducerTemplate template = actx.createFluentProducerTemplate().to("seda:simplemon?multipleConsumers=true");
			template.withBody( json ).asyncSend();

		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
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
		CentralLogger.actx = actx;
	}

}
