package portal.api.validation.ci;

/**
 * @author ctranoris
 *
 */
public class ValidationJobResult {

	/** */
	private String vxfid;
	/** */
	private Boolean validationStatus;
	/** */
	private String outputLog;
	
		
	
	/**
	 * @return the vxfid
	 */
	public String getVxfid() {
		return vxfid;
	}
	/**
	 * @param vxfid the vxfid to set
	 */
	public void setVxfid(String vxfid) {
		this.vxfid = vxfid;
	}


	/**
	 * @return the validationStatus
	 */
	public Boolean getValidationStatus() {
		return validationStatus;
	}
	/**
	 * @param validationStatus the validationStatus to set
	 */
	public void setValidationStatus(Boolean validationStatus) {
		this.validationStatus = validationStatus;
	}
	/**
	 * @return the outputLog
	 */
	public String getOutputLog() {
		return outputLog;
	}
	/**
	 * @param outputLog the outputLog to set
	 */
	public void setOutputLog(String outputLog) {
		this.outputLog = outputLog;
	}

	
}
