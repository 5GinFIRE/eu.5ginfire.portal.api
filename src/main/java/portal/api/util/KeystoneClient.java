package portal.api.util;

import java.util.List;

import org.openstack4j.model.identity.v3.User;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.v3.Role;
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
				credentials("ctranoris", "ctranoris", domainIdentifier).authenticate();


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
