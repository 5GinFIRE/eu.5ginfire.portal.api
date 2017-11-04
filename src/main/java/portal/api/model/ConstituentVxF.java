package portal.api.model;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;

@Entity(name = "ConstituentVxF")
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
