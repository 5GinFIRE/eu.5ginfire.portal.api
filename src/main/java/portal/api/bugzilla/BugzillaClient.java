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

package portal.api.bugzilla;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import portal.api.bugzilla.model.Bug;
import portal.api.bugzilla.model.Bugs;
import portal.api.bugzilla.model.Comment;
import portal.api.bugzilla.model.User;
import portal.api.bugzilla.model.Users;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.DeploymentDescriptorStatus;
import portal.api.model.DeploymentDescriptorVxFPlacement;
import portal.api.model.ExperimentMetadata;
import portal.api.model.PortalUser;
import portal.api.model.VFImage;
import portal.api.model.ValidationStatus;
import portal.api.model.VxFMetadata;
import portal.api.repo.PortalRepository;

public class BugzillaClient {

	private static final transient Log logger = LogFactory.getLog(BugzillaClient.class.getName());

	/** */
	private static BugzillaClient instance;


	/** */
	private static String BASE_SERVICE_URL = "https://portal.5ginfire.eu";

	/** */
	private static final String BUGHEADER =   "*************************************************\n"
											+ "THIS IS AN AUTOMATED ISSUE CREATED BY PORTAL API.\n"
											+ "*************************************************\n";


	public static BugzillaClient getInstance() {
		if (instance == null) {
			instance = new BugzillaClient();
			
			if (PortalRepository.getPropertyByName("maindomain").getValue() != null) {
				BASE_SERVICE_URL = PortalRepository.getPropertyByName("maindomain").getValue();
			}
		}
		return instance;
	}
	
	
		
	public static Bug transformDeployment2BugBody(DeploymentDescriptor descriptor) {

		String product = "5GinFIRE Operations";
		String component = "Operations Support" ;
		String summary = "[PORTAL] Deployment Request of NSD:" + descriptor.getExperiment().getName() + ",User: " + descriptor.getOwner().getUsername();
		String alias = descriptor.getUuid() ;

		String description = getDeploymentDescription( descriptor );		
		
		String status= "CONFIRMED";
		String resolution = null;
		if ( ( descriptor.getStatus() == DeploymentDescriptorStatus.SCHEDULED ) || ( descriptor.getStatus() == DeploymentDescriptorStatus.RUNNING )) {
			status = "IN_PROGRESS";
		} else  if ( ( descriptor.getStatus() == DeploymentDescriptorStatus.COMPLETED ) ) {
			status = "RESOLVED";
			resolution = "FIXED";
		} else if ( ( descriptor.getStatus() == DeploymentDescriptorStatus.REJECTED ) ) {
			status = "RESOLVED";
			resolution = "INVALID";
		}
		
		
		Bug b = createBug(product, component, summary, alias, description, descriptor.getOwner().getEmail(), status, resolution);
		
		return b;
	}
	
	
	public static Comment transformDeployment2BugComment(DeploymentDescriptor descriptor) {
		
		String description = getDeploymentDescription( descriptor );
				
		Comment b = createComment( description);
		
		return b;
	}
	
	
	/**
	 * @param descriptor
	 * @return
	 */
	private static String getDeploymentDescription(DeploymentDescriptor descriptor) {
		StringBuilder description =  new StringBuilder( BUGHEADER );

		description.append( "\nSTATUS: " + descriptor.getStatus() + "\n");
		if ( descriptor.getStartDate() != null ) {
			description.append( "\nFeedback: " + descriptor.getFeedback() );
			description.append( "\nScheduled Start Date: " + descriptor.getStartDate().toString() );
			description.append( "\nScheduled End Date: " + descriptor.getEndDate().toString() );
		} else {
			description.append( "\nNOT YET SCHEDULED \n");			
		}
		
		
		description.append(
						"\nDeployment Request by user :" + descriptor.getOwner().getUsername() 
						+"\nHere are the details:\n"
						+ "\nExperiment name: " + descriptor.getName() 
						+ "\nDescription: " + descriptor.getDescription() 
						+ "\nDate Created: " + descriptor.getDateCreated().toString() 
						+ "\nRequested Tentative Start date: " + descriptor.getStartReqDate().toString() 
						+ "\nRequested Tentative End date: " + descriptor.getEndReqDate().toString() 
						+ "\nExperiment (NSD) requested: " + descriptor.getExperiment().getName() );
		

		description.append( "\nConstituent VxF Placement " ) ;
		for (DeploymentDescriptorVxFPlacement pl : descriptor.getVxfPlacements()) {
			description.append( "\n  Constituent VxF: " + pl.getConstituentVxF().getVxfref().getName() + " - Infrastructure: " + pl.getInfrastructure().getName() );			
		}
		
		
				
						 
		description.append( "\n*************************************************\n");
		description.append( "\nTo manage this Request, go to: " + BASE_SERVICE_URL + "/#!/edit_deployment/" + descriptor.getId() ); 
		return description.toString();
	}


	public static Comment createComment( String description ) {
		
		Comment c = new Comment();
		c.setComment(description);
		c.setIs_markdown( false );
		c.setIs_private( false );	
		return c;
	}
	
	
	/**
	 * @param product
	 * @param component
	 * @param summary
	 * @param alias
	 * @param description
	 * @param ccemail
	 * @return
	 */
	public static Bug createBug(String product, String component, String summary, String alias, String description, String ccemail, String status, String resolution ) {
		
		Bug b = new Bug();
		b.setProduct(product);
		b.setComponent(component);
		b.setSummary(summary);
		b.setVersion( "unspecified" );
		List<Object> aliaslist = new ArrayList<>();
		aliaslist.add(alias);		
		b.setAlias( aliaslist );
		List<String> cclist = new ArrayList<>();
		cclist.add( ccemail );		
		b.setCc(cclist); 
		b.setDescription(description.toString());		
		b.setStatus(status);
		b.setResolution(resolution);
				
		return b;
	}
	
	
	public static User transformUser2BugzillaUser(PortalUser portalUser){
		
		User u = new User();
		u.setEmail( portalUser.getEmail()  );
		u.setFullName( portalUser.getName() );
		u.setPassword( UUID.randomUUID().toString() ); //no password. The user needs to reset it in the other system (e.g. Bugzilla)
		return u;
		
	}


	public static Bug transformVxFValidation2BugBody(VxFMetadata vxf) {

		String product = "5GinFIRE Operations";
		String component = "Validation" ;
		String summary = "[PORTAL] Validation Request for VxF:" + vxf.getName() + ", Owner: " + vxf.getOwner().getUsername();
		String alias = vxf.getUuid() ;
		
		StringBuilder description =  new StringBuilder( BUGHEADER );
		description.append( "\n Validation Status: " + vxf.getValidationStatus()  );
		description.append( "\n Certified: " + String.valueOf( vxf.isCertified() ).toUpperCase() );
		
		description.append( "\n\n VxF: " + vxf.getName());
		description.append( "\n Owner: " +  vxf.getOwner().getUsername() );
		description.append( "\n Vendor: " +  vxf.getVendor() );
		description.append( "\n Version: " + vxf.getVersion() );
		description.append( "\n Archive: " + vxf.getPackageLocation() );
		description.append( "\n UUID: " + vxf.getUuid()  );
		description.append( "\n ID: " + vxf.getId()   );
		description.append( "\n Date Created: " + vxf.getDateCreated().toString()   );
		description.append( "\n Date Updated: " + vxf.getDateUpdated().toString()   );

		description.append( "\n VDU Images: "    );
		for (VFImage img : vxf.getVfimagesVDU() ) {
			description.append( "\n\t Image: " + img.getName() + ", " + BASE_SERVICE_URL + "/#!/vfimage_view/" + img.getId()    );
			
		}
		 
		description.append( "\n\n*************************************************\n");
		description.append( "\nTo manage this , go to: " + BASE_SERVICE_URL + "/#!/vxf_edit//" + vxf.getId() ); 
		
		String status= "CONFIRMED";
		String resolution = null;
		if ( vxf.getValidationStatus().equals( ValidationStatus.UNDER_REVIEW ) )  {
			status = "IN_PROGRESS";
		} else  if ( vxf.isCertified()  &&  ( vxf.getValidationStatus().equals( ValidationStatus.COMPLETED ) ) ) {
			status = "RESOLVED";
			resolution = "FIXED";
		} else  if ( !vxf.isCertified()  &&  ( vxf.getValidationStatus().equals( ValidationStatus.COMPLETED ) ) ) {
			status = "RESOLVED";
			resolution = "INVALID";
		}
		
		
		Bug b = createBug(product, component, summary, alias, description.toString(), vxf.getOwner().getEmail(), status, resolution);
		
		return b;
	}
	
	
	public static Bug transformNSDValidation2BugBody(ExperimentMetadata nsd) {

		String product = "5GinFIRE Operations";
		String component = "Validation" ;
		String summary = "[PORTAL] Validation Request for NSD:" + nsd.getName() + ", Owner: " + nsd.getOwner().getUsername();
		String alias = nsd.getUuid() ;
		
		StringBuilder description =  new StringBuilder( BUGHEADER );
		description.append( "\n Validation Status: " + nsd.getValidationStatus()  );
		description.append( "\n Valid: " + String.valueOf( nsd.isValid() ).toUpperCase() );
		
		description.append( "\n\n NSD: " + nsd.getName());
		description.append( "\n Owner: " +  nsd.getOwner().getUsername() );
		description.append( "\n Vendor: " +  nsd.getVendor() );
		description.append( "\n Version: " + nsd.getVersion() );
		description.append( "\n Archive: " + nsd.getPackageLocation() );
		description.append( "\n UUID: " + nsd.getUuid()  );
		description.append( "\n ID: " + nsd.getId()   );
		description.append( "\n Date Created: " + nsd.getDateCreated().toString() );
		description.append( "\n Date Updated: " + nsd.getDateUpdated().toString() );
		

		 
		description.append( "\n\n*************************************************\n");
		description.append( "\nTo manage this , go to: " + BASE_SERVICE_URL + "/#!/vxf_edit//" + nsd.getId() ); 
		
		String status= "CONFIRMED";
		String resolution = null;
		if ( nsd.getValidationStatus().equals( ValidationStatus.UNDER_REVIEW ) )  {
			status = "IN_PROGRESS";
		} else  if ( nsd.isValid()  &&  ( nsd.getValidationStatus().equals( ValidationStatus.COMPLETED ) ) ) {
			status = "RESOLVED";
			resolution = "FIXED";
		} else  if ( !nsd.isValid()  &&  ( nsd.getValidationStatus().equals( ValidationStatus.COMPLETED ) ) ) {
			status = "RESOLVED";
			resolution = "INVALID";
		}
		
		
		Bug b = createBug(product, component, summary, alias, description.toString(), nsd.getOwner().getEmail(), status, resolution);
		
		return b;
	}

}
