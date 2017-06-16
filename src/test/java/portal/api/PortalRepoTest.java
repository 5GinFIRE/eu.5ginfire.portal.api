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

package portal.api;

import static org.junit.Assert.*;

import java.util.UUID;

import portal.api.impl.PortalJpaController;
import portal.api.model.ExperimentMetadata;
import portal.api.model.PortalUser;
import portal.api.model.VxFMetadata;
import portal.api.model.Category;
import portal.api.model.Container;
import portal.api.model.DeployArtifact;
import portal.api.model.DeployContainer;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.DeploymentDescriptorStatus;
import portal.api.model.ProductExtensionItem;
import portal.api.model.SubscribedResource;
import portal.api.util.EncryptionUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:contextTest.xml" })
//@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
//@Transactional
public class PortalRepoTest {

	@Autowired
	private  PortalJpaController portalJpaControllerTest;

	// private static final transient Log logger = LogFactory.getLog(PortalRepoTest.class.getName());

	@Before
	public void deletePreviousobjectsDB() {

		portalJpaControllerTest.deleteAllUsers();
		portalJpaControllerTest.deleteAllProducts();
		portalJpaControllerTest.deleteAllSubscribedResources();
		portalJpaControllerTest.deleteAllCategories();

	}

	@Test
	public void testWriteReadDB() {

		portalJpaControllerTest.getAllProductsPrinted();
		
		PortalUser bu = new PortalUser();
		bu.setOrganization("UoP");
		bu.setName("aname");
		bu.setUsername("ausername");
		bu.setPassword("apassword");
		bu.setEmail("e@e.com");

		portalJpaControllerTest.saveUser(bu);
		
		VxFMetadata bmeta = new VxFMetadata();
		bmeta.setName("avxf");
		String uuid = UUID.randomUUID().toString();
		bmeta.setUuid(uuid);
		bmeta.setLongDescription("longDescription");
		bmeta.setShortDescription("shortDescription");
		bmeta.setPackageLocation("packageLocation");
		bmeta.addExtensionItem("aname", "avalue");
		bmeta.addExtensionItem("aname", "avalue");
		bmeta.addExtensionItem("aname1", "avalue1");
		bu.addProduct(bmeta);

		portalJpaControllerTest.updatePortalUser(bu);
		
		// change name and reSave
		bmeta = (VxFMetadata) portalJpaControllerTest.readProductByUUID(uuid);
		bmeta.setName("NewVxFName");
		portalJpaControllerTest.updateProduct(bmeta);		

		portalJpaControllerTest.getAllProductsPrinted();
		
		bmeta = new VxFMetadata();
		String uuid2 = UUID.randomUUID().toString();
		bmeta.setUuid(uuid2);
		bmeta.setName("avxf2");
		bmeta.setLongDescription("longDescription2");
		bmeta.setShortDescription("shortDescription2");
		bmeta.setPackageLocation("packageLocation2");
		bu = portalJpaControllerTest.readPortalUserByUsername("ausername");
		bu.addProduct(bmeta);

		portalJpaControllerTest.updatePortalUser(bu);

		PortalUser testbu = portalJpaControllerTest.readPortalUserByUsername("ausername");
		assertEquals("aname", testbu.getName());
		assertEquals(EncryptionUtil.hash("apassword"), testbu.getPassword());
		assertEquals("UoP", testbu.getOrganization());
		assertEquals("e@e.com", testbu.getEmail());


		portalJpaControllerTest.getAllProductsPrinted();
		
		assertEquals(2, testbu.getProducts().size());

		VxFMetadata testbm = (VxFMetadata) portalJpaControllerTest.readProductByUUID(uuid);
		assertEquals("NewVxFName", testbm.getName());
		assertEquals(uuid, testbm.getUuid());
		assertNotNull(testbm.getOwner());
		assertEquals("ausername", testbm.getOwner().getUsername());
		assertEquals( 2, testbm.getExtensions().size() );
		assertEquals( "aname", testbm.getExtensions().get(0).getName() );
		assertEquals( "aname1", testbm.getExtensions().get(1).getName() );

		bu = new PortalUser();
		bu.setOrganization("UoP2");
		bu.setName("aname2");
		bu.setUsername("ausername2");
		bu.setPassword("apassword2");

		portalJpaControllerTest.saveUser(bu);
		portalJpaControllerTest.getAllUsersPrinted();
		assertEquals(2, portalJpaControllerTest.countUsers());

	}

	@Test
	public void testSubscribedResources() {
		SubscribedResource sm = new SubscribedResource();
		sm.setURL("testURL");

		assertEquals("testURL", sm.getURL());

		portalJpaControllerTest.saveSubscribedResource(sm);

		sm.setURL("testURL1");
		portalJpaControllerTest.updateSubscribedResource(sm);

		SubscribedResource testsm = portalJpaControllerTest.readSubscribedResourceById(sm.getId());
		assertEquals("testURL1", testsm.getURL());

		sm = new SubscribedResource();
		sm.setURL("anotherTestURL");
		portalJpaControllerTest.saveSubscribedResource(sm);
		portalJpaControllerTest.getAllSubscribedResourcesPrinted();
		assertEquals(2, portalJpaControllerTest.countSubscribedResources());

		portalJpaControllerTest.deleteSubscribedResource(sm.getId());

	}
	
	@Test
	public void testWriteReadApplications() {
		
		Category c = new Category();
		c.setName("acat1");
		assertEquals("acat1", c.getName());
		Category c2 = new Category();
		c2.setName("acat2");
		
		PortalUser bu = new PortalUser();
		bu.setUsername("ausernameWRA");

		ExperimentMetadata appmeta = new ExperimentMetadata();
		appmeta.setName("app");
		String uuid = UUID.randomUUID().toString();
		appmeta.setUuid(uuid);
		appmeta.setLongDescription("longDescription");
		appmeta.setShortDescription("shortDescription");
		appmeta.getCategories().add(c);
		appmeta.getCategories().add(c2);
		ProductExtensionItem item = new ProductExtensionItem();
		item.setName("param1");
		item.setValue("value1");
		appmeta.addExtensionItem(item );
		ProductExtensionItem item2 = new ProductExtensionItem();
		item.setName("param2");
		item.setValue("value2");
		appmeta.addExtensionItem(item2 );
		bu.addProduct(appmeta);

		portalJpaControllerTest.saveUser(bu);

		// change name and reSave
		appmeta.setName("NewAppName");
		portalJpaControllerTest.updateProduct(appmeta);
		assertEquals(2, appmeta.getCategories().size() );
		assertEquals(2, appmeta.getExtensions().size() );

		ExperimentMetadata appmeta2 = new ExperimentMetadata();
		appmeta2.setName("app2");
		appmeta2.setLongDescription("longDescription2");
		appmeta2.setShortDescription("shortDescription2");
		appmeta2.setOwner(bu);
		appmeta2.getCategories().add(c);
		bu.addProduct(appmeta2);

		portalJpaControllerTest.updatePortalUser(bu);
		portalJpaControllerTest.getAllUsersPrinted();

		PortalUser testbu = portalJpaControllerTest.readPortalUserByUsername("ausernameWRA");
		assertEquals(2, testbu.getProducts().size());

		ExperimentMetadata testApp = (ExperimentMetadata) portalJpaControllerTest.readProductByUUID(uuid);
		assertEquals("NewAppName", testApp.getName());
		assertEquals(uuid, testApp.getUuid());
		assertNotNull(testApp.getOwner());
		assertEquals("ausernameWRA", testApp.getOwner().getUsername());
		portalJpaControllerTest.getAllCategoriesPrinted();
		assertEquals("acat1", testApp.getCategories().get(0).getName());


	}
	
	@Test
	public void testDeployDescriptorApplications() {
		Category c = new Category();
		c.setName("acat1");
		PortalUser bu = new PortalUser();
		bu.setUsername("ausername123");
		
		//add a couple of vxfs
		VxFMetadata bmeta = new VxFMetadata();
		bmeta.setName("vxf1");
		String uuid = UUID.randomUUID().toString();
		bmeta.setUuid(uuid);
		bmeta.addExtensionItem("aname1", "avalue1");
		bmeta.addExtensionItem("aname2", "avalue2");
		bu.addProduct(bmeta);
		
		VxFMetadata bmeta2 = new VxFMetadata();
		bmeta2.setName("vxf2");
		uuid = UUID.randomUUID().toString();
		bmeta2.setUuid(uuid);
		bmeta2.addExtensionItem("aname11", "avalue11");
		bmeta2.addExtensionItem("aname21", "avalue21");
		bu.addProduct(bmeta2);		
		
		//add an application description
		ExperimentMetadata app = new ExperimentMetadata();
		app.setName("myapp");
		uuid = UUID.randomUUID().toString();
		app.setUuid(uuid);
		app.setLongDescription("longDescription");
		app.setShortDescription("shortDescription");
		app.getCategories().add(c);
		Container container = new Container(); //add a container
		container.setName("Container0");		
		DeployArtifact deployArtifact = new DeployArtifact();
		deployArtifact.setName(bmeta2.getName() );
		deployArtifact.setUuid(bmeta2.getUuid());
		container.getDeployArtifacts().add(deployArtifact);
		app.getContainers().add(container );		
		bu.addProduct(app);
		
		//now create a dployment
		DeploymentDescriptor dd = new DeploymentDescriptor();
		dd.setBaseApplication(app);
		dd.setName("a test DeployDescriptor");
		dd.setOwner(bu);
		dd.setStatus( DeploymentDescriptorStatus.PENDING_ADMIN_AUTH );
		DeployContainer deplContainer = new DeployContainer();
		deplContainer.setName("deploy1");
		DeployArtifact deployArtifactInst = new DeployArtifact();
		deployArtifactInst.setName( dd.getBaseApplication().getContainers().get(0).getDeployArtifacts().get(0).getName() );
		deployArtifactInst.setUuid( dd.getBaseApplication().getContainers().get(0).getDeployArtifacts().get(0).getUuid() );		
		deplContainer.getDeployArtifacts().add(deployArtifactInst);
		SubscribedResource targetResource = new SubscribedResource();
		targetResource.setURL("targetIP");
		deplContainer.setTargetResource(targetResource );
		dd.getDeployContainers().add(deplContainer);
		bu.getDeployments().add(dd);//now add the deployment to the user

		portalJpaControllerTest.saveUser(bu);
		

		PortalUser testbu = portalJpaControllerTest.readPortalUserByUsername("ausername123") ;		

		assertEquals(1, testbu.getDeployments().size()  );
		assertEquals(1, testbu.getDeployments().get(0).getDeployContainers().size()  );
		assertEquals("myapp", testbu.getDeployments().get(0).getBaseApplication().getName() );
		assertEquals("targetIP", testbu.getDeployments().get(0).getDeployContainers().get(0).getTargetResource().getURL()  );
		
	}

}
