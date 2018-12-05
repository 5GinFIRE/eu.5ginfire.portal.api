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

package portal.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import portal.api.model.VFImage.RefVxF;

/**
 * @author ctranoris
 *
 */

@Entity(name = "Infrastructure")
@JsonIgnoreProperties(value = { "supportedImages"  })
public class Infrastructure {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id = 0;

	@Basic()
	private String organization = null;
	@Basic()
	private String name = null;
	@Basic()
	private String email = null;
	
	@Basic()
	private String datacentername = null;
	
	@Basic()
	private String vimid = null;

	@ManyToMany(cascade = {  CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	@JoinTable()
	private List<VFImage> supportedImages = new ArrayList<>();
	

	@Transient
	private List<RefVFImage> refSupportedImages = new ArrayList<>();
	

	public String getDatacentername() {
		return datacentername;
	}

	public void setDatacentername(String datacentername) {
		this.datacentername = datacentername;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}

	public String getVIMid() {
		return vimid;
	}

	public void setVIMid(String vimid) {
		this.vimid = vimid;
	}
	
	/**
	 * @return the supportedImages
	 */
	public List<VFImage> getSupportedImages() {
		return supportedImages;
	}

	/**
	 * @param supportedImages the supportedImages to set
	 */
	public void setSupportedImages(List<VFImage> supportedImages) {
		this.supportedImages = supportedImages;
	}

	

	/**
	 * @return the refSupportedImages
	 */
	public List<RefVFImage> getRefSupportedImages() {
		refSupportedImages.clear();
		for (VFImage vfimg : supportedImages) {
			RefVFImage ref = new RefVFImage( vfimg.getId(), vfimg.getName());
			refSupportedImages.add( ref );
		}
		return refSupportedImages;
	}

	/**
	 * @param refSupportedImages the refSupportedImages to set
	 */
	public void setRefSupportedImages(List<RefVFImage> refSupportedImages) {
		this.refSupportedImages = refSupportedImages;
	}



	/**
	 * Locally used to report back objects, otherwise the response would be recursive
	 * @author ctranoris
	 *
	 */
	static class RefVFImage {
		
		/** */
		private long id;
		/** */
		private String name;
		
		public RefVFImage() {
			
		}
		
		public RefVFImage(long id2, String name2) {
			id = id2;
			name = name2;
					
		}

		/**
		 * @return the id
		 */
		public long getId() {
			return id;
		}

		/**
		 * @param id the id to set
		 */
		public void setId(long id) {
			this.id = id;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
	}



	/**
	 * @param id2
	 * @return
	 */
	public Object getSupportedImageById(int id2) {
		for (VFImage refVFImage : supportedImages) {
			if ( refVFImage.getId() == id2  ){
				return refVFImage;
			}
		}
		return null;
	}
	
}
