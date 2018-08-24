package com.thed.zephyr.je.config.customfield.searcher;


import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.indexers.impl.AbstractCustomFieldIndexer;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.thed.zephyr.je.vo.TeststepBean;

/**
 * A simple custom field indexer for the number custom fields
 *
 * @since v4.0
 */
public class TeststepCustomfieldIndexer extends AbstractCustomFieldIndexer
{
	protected static final Logger log = Logger.getLogger(TeststepCustomfieldIndexer.class);

	private final CustomField teststepField;

    public TeststepCustomfieldIndexer(final FieldVisibilityManager fieldVisibilityManager, final CustomField customField)
    {
        super(fieldVisibilityManager, notNull("customField", customField));
        this.teststepField = customField;
    }

    @Override
	public void addDocumentFieldsSearchable(final Document doc, final Issue issue)
    {
        addDocumentFields(doc, issue, Field.Index.NOT_ANALYZED_NO_NORMS);
    }

    @Override
	public void addDocumentFieldsNotSearchable(final Document doc, final Issue issue)
    {
        addDocumentFields(doc, issue, Field.Index.NO);
    }

    private void addDocumentFields(final Document doc, final Issue issue, final Field.Index indexType)
    {
        /*
		 * getValue() calls the CustomFieldType getValueFromIssue method so this
		 * Object is the custom field's transport object.
		 */
		Object value = teststepField.getValue(issue);
		
		log.debug("TeststepCustomfieldIndexer - addDocumentFields - adding field to index");

		if (value == null) {
			return;
		}
        
		@SuppressWarnings("unchecked")
		List<TeststepBean> teststepList = (List<TeststepBean>) value;

		for (TeststepBean teststep : teststepList) {
			
			if (StringUtils.isNotBlank(teststep.getStep())) {
	            doc.add(new Field(getDocumentFieldId(), teststep.getStep(), Field.Store.YES, indexType));
			}
			
			if (StringUtils.isNotBlank(teststep.getData())) {
	            doc.add(new Field(getDocumentFieldId(), teststep.getData(), Field.Store.YES, indexType));
			}

			if (StringUtils.isNotBlank(teststep.getResult())) {
	            doc.add(new Field(getDocumentFieldId(), teststep.getResult(), Field.Store.YES, indexType));
			}

		}
		
    }
}