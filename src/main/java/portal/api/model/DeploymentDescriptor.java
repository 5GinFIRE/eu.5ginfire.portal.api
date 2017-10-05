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
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;


/**
 * @author ctranoris
 *
 */
@Entity(name = "DeploymentDescriptor")
public class DeploymentDescriptor {
	

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id = 0;
	
	@Basic()
	private String name = null;
	

	@Lob
	@Column(name = "LDESCRIPTION", columnDefinition = "LONGTEXT")
	private String description = null;
	


	@Lob
	@Column(name = "FEEDBACK", columnDefinition = "LONGTEXT")
	private String feedback = null;

	@Basic()
	private DeploymentDescriptorStatus status = DeploymentDescriptorStatus.UNDER_REVIEW;
	
	

	@Basic()
	private Date dateCreated;

	@Basic()
	private Date startReqDate;

	@Basic()	
	private Date endReqDate;

	@Basic()	
	private Date startDate;
	@Basic()	
	private Date endDate;

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinColumns({ @JoinColumn() })
	private ExperimentMetadata experiment = null;


	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinColumns({ @JoinColumn() })
	private PortalUser owner = null;
	
	

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	

	public PortalUser getOwner() {
		return owner;
	}

	public void setOwner(PortalUser owner) {
		this.owner = owner;
	}

	
	public ExperimentMetadata getExperiment() {
		return experiment;
	}

	public void setExperiment(ExperimentMetadata e) {
		this.experiment = e;
	}
	
	public DeploymentDescriptorStatus getStatus() {
		return status;
	}

	public void setStatus(DeploymentDescriptorStatus status) {
		this.status = status;
	}

	public Date getStartReqDate() {
		return startReqDate;
	}

	public void setStartReqDate(Date startReqDate) {
		this.startReqDate = startReqDate;
	}

	public Date getEndReqDate() {
		return endReqDate;
	}

	public void setEndReqDate(Date endReqDate) {
		this.endReqDate = endReqDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback;
	}


	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
}
