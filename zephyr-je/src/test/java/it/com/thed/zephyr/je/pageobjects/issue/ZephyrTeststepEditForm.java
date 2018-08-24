package it.com.thed.zephyr.je.pageobjects.issue;


import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.jira.pageobjects.components.restfultable.AbstractEditRow;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class ZephyrTeststepEditForm extends AbstractEditRow
{
  private static final String STEP = "zstep";
  private static final String DATA = "zresult";
  private static final String RESULT = "zdata";

  @Inject
  private PageBinder pageBinder;

  public ZephyrTeststepEditForm(By rowSelector)
  {
    super(rowSelector);
  }

  public ZephyrTeststepEditForm fill(String step, String data, String result)
  {
    getStepField().clear().type(step);
    getDataField().clear().type(data);
    getResultField().clear().type(result);

    return this;
  }

  public ZephyrTeststepEditForm submit()
  {
    getAddButton().click();
    Poller.waitUntilFalse(this.row.timed().hasClass("loading"));
    return this;
  }

  public ZephyrTeststepEditForm cancel()
  {
    getCancelLink().click();
    Poller.waitUntilFalse(this.row.timed().hasClass("loading"));
    return this;
  }

  public AbstractEditRow.Field getStepField()
  {
    return this.pageBinder.bind(ZField.class, new Object[] { this.row.find(By.cssSelector(".ztextarea[name|='step']")) });
  }

  public AbstractEditRow.Field getDataField()
  {
    return this.pageBinder.bind(ZField.class, new Object[] { this.row.find(By.cssSelector(".ztextarea[name|='data']"))  });
  }

  public AbstractEditRow.Field getResultField()
  {
    return this.pageBinder.bind(ZField.class, new Object[] { this.row.find(By.cssSelector(".ztextarea[name|='result']")) });
  }
  
  protected PageElement findInRowById(String id)
  {
    return this.row.find(By.id(id));
  }
  
  public static class ZField extends AbstractEditRow.Field
  {
    private PageElement field;

    public ZField(PageElement cell)
    {
      super(cell);
      this.field = cell;//.find(By.tagName("textarea"));
    }

    @Override
	public String value()
    {
      return this.field.getValue();
    }

    @Override
	public Field type(String value)
    {
      this.field.type(new CharSequence[] { value });
      return this;
    }

    @Override
	public Field clear()
    {
      this.field.clear();
      return this;
    }
  }
  
}