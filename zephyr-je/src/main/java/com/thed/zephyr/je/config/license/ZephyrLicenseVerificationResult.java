package com.thed.zephyr.je.config.license;

import java.net.URI;

import com.thed.zephyr.util.ZephyrLicenseException;

public class ZephyrLicenseVerificationResult {

	private boolean valid;
	private ZephyrLicenseException exception;
	private String errorMessage;
	private String generalMessage;
	private URI forwardURI;
	
	
	public ZephyrLicenseVerificationResult(){
		
	}
	
	public ZephyrLicenseVerificationResult(boolean valid,
			ZephyrLicenseException lic, String errorMessage,
			String generalMessage, URI forwardURI) {
		super();
		this.valid = valid;
		this.exception = lic;
		this.errorMessage = errorMessage;
		this.generalMessage = generalMessage;
		this.forwardURI = forwardURI;
	}
	
	public boolean isValid() {
		return valid;
	}
	public void setValid(boolean valid) {
		this.valid = valid;
	}
	public ZephyrLicenseException getException() {
		return exception;
	}
	public void setException(ZephyrLicenseException lic) {
		this.exception = lic;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public String getGeneralMessage() {
		return generalMessage;
	}
	public void setGeneralMessage(String generalMessage) {
		this.generalMessage = generalMessage;
	}
	public URI getForwardURI() {
		return forwardURI;
	}
	public void setForwardURI(URI forwardURI) {
		this.forwardURI = forwardURI;
	}
	
	
}
