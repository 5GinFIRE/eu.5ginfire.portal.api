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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import portal.api.model.InstalledVxF;
import portal.api.model.InstalledVxFStatus;

public class InstalledServiceTest {


	private static final transient Log logger = LogFactory.getLog(InstalledServiceTest.class.getName());
	
	@Test
	public void testGetUuid() {
		InstalledVxF is = installedServiceInit();
		assertNotNull(is.getUuid());				
	}

	@Test
	public void testSetUuid() {
		InstalledVxF is = installedServiceInit();
		UUID uuid = UUID.randomUUID();
		is.setUuid(uuid.toString());
		assertEquals(uuid.toString(), is.getUuid());
		
	}

	@Test
	public void testGetRepoUrl() {
		InstalledVxF is = installedServiceInit();
		assertNotNull(is.getRepoUrl());		
				
	}

	@Test
	public void testSetRepoUrl() {
		InstalledVxF is = installedServiceInit();
		String url= "testurl";
		is.setRepoUrl(url);
		assertEquals(url, is.getRepoUrl());
	}

	@Test
	public void testGetInstalledVersion() {
		InstalledVxF is = installedServiceInit();
		assertNotNull(is.getInstalledVersion());		
	}

	@Test
	public void testSetInstalledVersion() {
		InstalledVxF is = installedServiceInit();
		String version= "2.2vv2";
		is.setInstalledVersion(version);
		assertEquals(version, is.getInstalledVersion());
	}
	
	@Test
	public void testSetStatus() {
		InstalledVxF is = installedServiceInit();
		
		is.setStatus( InstalledVxFStatus.INSTALLING ); 
		assertEquals(InstalledVxFStatus.INSTALLING, is.getStatus());
	}
	
	
	//helpers
	private InstalledVxF installedServiceInit(){
		UUID uuid =  UUID.randomUUID();		
		String repoUrl="repourl";
		InstalledVxF is = new InstalledVxF(uuid.toString() , repoUrl);
		is.setInstalledVersion("1.1v");
		return is;
	}

}
