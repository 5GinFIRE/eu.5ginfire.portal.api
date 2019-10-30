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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "ConstituentVxF")
@JsonIgnoreProperties(value = { "vxfref"  })
public class ConstituentVxF {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id = 0;

	@Basic()
	private int membervnfIndex;

	@Basic()
	private String vnfdidRef;
	
	@OneToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinTable()
	private VxFMetadata vxfref;

	public int getMembervnfIndex() {
		return membervnfIndex;
	}

	public void setMembervnfIndex(int membervnfIndex) {
		this.membervnfIndex = membervnfIndex;
	}

	public String getVnfdidRef() {
		return vnfdidRef;
	}

	public void setVnfdidRef(String vnfdidRef) {
		this.vnfdidRef = vnfdidRef;
	}

	public VxFMetadata getVxfref() {
		return vxfref;
	}

	public void setVxfref(VxFMetadata vxfref) {
		this.vxfref = vxfref;
	}
    
	
	
}
