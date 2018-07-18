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

package portal.api.util;

import java.util.List;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.User;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeystoneClient {

	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(KeystoneClient.class);
		OSFactory.enableHttpLoggingFilter(true);
		
		// use Identifier.byId("domainId") or Identifier.byName("example-domain")
		Identifier domainIdentifier = Identifier.byId("default");
		
		
		OSClientV3 os = OSFactory.builderV3().endpoint("http://150.140.184.235:5000/v3").
				credentials("ctranoris", "", domainIdentifier).authenticate();


		System.out.println(" user = " + os.identity().roles());
		// Get a list of all roles
		List<? extends User> us = os.identity().users().list();
		for (User user : us) {
			System.out.println(" user = " + user.getName() + ", email = " + user.getEmail()  );

		}

		List<? extends Role> ls = os.identity().roles().list();
		for (Role role : ls) {

			System.out.println("role = " + role.toString());
		}
	}

}
