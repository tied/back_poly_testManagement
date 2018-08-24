package com.thed.zephyr.je.pageobjects.admin;

import com.atlassian.jira.pageobjects.pages.AbstractJiraAdminPage;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;

public class ZephyrLicenseConfigPage extends AbstractJiraAdminPage{

	  private final static String URI = "/secure/admin/ZLicense.jspa";
	  private final static String LINK_ID = "zephyr_license";
	
	  @ElementBy(id = "license_table")
	  private PageElement licenseContainer;
	
	  @ElementBy(id = "Add")
	  private PageElement addLicenseButton;
	
	  @ElementBy(name = "newLicenseKey")
	  private PageElement licenseArea;
	  
	  @ElementBy(className ="errMsg")
	  private PageElement errorMessage;
	  	  
	  @Override
	  public String linkId()
	  {
	    return LINK_ID;
	  }
	
	  @Override
	  public TimedCondition isAt()
	  {
	    return licenseContainer.timed().isPresent();
	  }
	
	  @Override
	  public String getUrl()
	  {
	    return URI;
	  }
	
	  @Init
	  public void initialize()
	  {
	  }
	
	  public ZephyrLicenseConfigPage submit()
	  {
		addLicenseButton.click();
	    return this;
	  }
	  
	  public String getErrorMessage(){
		  return errorMessage.getText();
	  }
	  

	  public ZephyrLicenseConfigPage setLicense(String newLicenseKey){
		  licenseArea.clear();
		  licenseArea.type("RU5URVJQUklTRSMxI1plcGh5ciBmb3IgSklSQSAtIFVubGltaXRlZCB1c2VycyAjMSMwMS0zMC0yMDEyIzAxLTIwLTIwMTMjMDEtMjAtMjAxMyMwIzEjVW5saW1pdGVkI3NoYWlsZXNoLm1hbmdhbEBnbWFpbC5jb20jWkVQSF9URVNUSU5HI3NoYWlsZXNoLm1hbmdhbEBnbWFpbC5jb20=#MCwCFEOtLosdWS+UA4VyzgZCZaiPs7BZAhRF6xEISoVBABSaDU6P77Ow0ZBUXQ==@MIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYUAAoGBAMji7YJSjqTLqSk34OuCPCP+lw8RdsRWUBEbIVpjNQhuRX0X9XQlr8K8JhJ8v5CXCcX5sF255zvl4Il6VoTqu2PhehRcFW1/kTWSZnPO0ikRXKYV1HiLvaO9v9CR+zJnwyIAQ+7/VMRkGXEAaGdtybVf2+ECkKffLa50PciRtv7x");
		  return this;
	  }
}
