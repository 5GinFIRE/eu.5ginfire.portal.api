package portal.api.bugzilla.model;

public class ErrorMsg {

	String message;
	
	public ErrorMsg() {
		super();
		this.message = "";
	}

	public ErrorMsg(String message) {
		super();
		this.message = message;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
