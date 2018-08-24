package com.thed.zephyr.je.zql.core.mapper;

import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.constants.SimpleFieldSearchConstantsWithEmpty;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.query.clause.TerminalClauseImpl;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericValue;

import java.util.Comparator;

public class FixForVersionStatisticsMapper implements StatisticsMapper {
    private final VersionManager versionManager;
	private final String clauseName;
	private final String documentConstant;

	public FixForVersionStatisticsMapper(VersionManager versionManager) {
		this(versionManager, SystemSearchConstant.forFixForVersion());
	}

	public FixForVersionStatisticsMapper(VersionManager versionManager,
			SimpleFieldSearchConstantsWithEmpty searchConstants) {
		this(versionManager,
				searchConstants.getJqlClauseNames().getPrimaryName(),
				searchConstants.getIndexField());
	}

	public FixForVersionStatisticsMapper(VersionManager versionManager, String clauseName,
			String documentConstant) {
		this.versionManager = versionManager;
		this.clauseName = clauseName;
		this.documentConstant = documentConstant;
	}

	public String getDocumentConstant() {
		return documentConstant;
	}

	public Object getValueFromLuceneField(String documentValue) {
        String versionName = ApplicationConstants.UNSCHEDULED_VERSION_NAME;
        if (StringUtils.isNotBlank(documentValue)) {
			long versionId = Long.parseLong(documentValue);
            if (versionId > 0 ) {
                Version version = versionManager.getVersion(versionId);
                if(null != version)
                    versionName =  version.getName();
			}
		}
		return versionName;
	}

	public Comparator getComparator() {
		return NameComparator.INSTANCE;
	}

	public boolean isFieldAlwaysPartOfAnIssue() {
		return true;
	}

	public boolean isValidValue(Object value) {
		return true;
	}

	public SearchRequest getSearchUrlSuffix(Object value,SearchRequest searchRequest) {
		if (searchRequest == null) {
			return null;
		} else {
			JqlClauseBuilder builder = JqlQueryBuilder
					.newBuilder(searchRequest.getQuery()).where().defaultAnd();
			if (value != null) {
				final Long versionId = ((GenericValue) value).getLong("id");
				final Version version = versionManager.getVersion(versionId);
				builder.addClause(new TerminalClauseImpl(getClauseName(),
						Operator.EQUALS, version.getName()));
			} else {
				builder.addEmptyCondition(getClauseName());
			}
			return new SearchRequest(builder.buildQuery());
		}
	}

	protected String getClauseName() {
		return clauseName;
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final FixForVersionStatisticsMapper that = (FixForVersionStatisticsMapper) o;

		return (getDocumentConstant() != null ? getDocumentConstant().equals(
				that.getDocumentConstant())
				: that.getDocumentConstant() == null);
	}

	public int hashCode() {
		return (getDocumentConstant() != null ? getDocumentConstant()
				.hashCode() : 0);
	}
}
