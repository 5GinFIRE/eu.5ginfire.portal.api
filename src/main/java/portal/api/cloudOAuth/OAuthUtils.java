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

package portal.api.cloudOAuth;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

public class OAuthUtils {

	private static final transient Log logger = LogFactory.getLog(OAuthUtils.class.getName());

	public static OAuthUser getOAuthUser(String authHeader, ClientAccessToken accessToken) {
		// get fi-ware user info

		try {
			WebClient fiwareService = WebClient.create("https://account.lab.fiware.org/user");
			fiwareService.replaceHeader("Authorization", authHeader);
			fiwareService.replaceQueryParam("auth_token", accessToken.getTokenKey());

			Response r = fiwareService.accept("application/json").type("application/json").get();
			// InputStream i = (InputStream)r.getEntity();
			// String s = IOUtils.toString(i);
			// logger.info("===  USER response: "+ s );
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser;
			parser = factory.createJsonParser((InputStream) r.getEntity());
			OAuthUser fu = parser.readValueAs(OAuthUser.class);

			logger.info("=== USER response: " + fu.toString());

			return fu;

		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;

	}
	
	public static String getOAuthUserExtendend(String CloudUSerNickName,  String authHeader, ClientAccessToken accessToken) {
		try {
			
			MappingJsonFactory factory = new MappingJsonFactory();
			
			WebClient cloudService = WebClient.create("https://account.lab.org/users/" + CloudUSerNickName + ".json");
			cloudService.replaceHeader("Authorization", authHeader);
			cloudService.replaceQueryParam("auth_token", accessToken.getTokenKey());

			Response r = cloudService.get();
			InputStream i2 = (InputStream) r.getEntity();
			String s2 = IOUtils.toString(i2);
			logger.info("===  USER users response: " + s2);

			return s2;

		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;

	}
}
