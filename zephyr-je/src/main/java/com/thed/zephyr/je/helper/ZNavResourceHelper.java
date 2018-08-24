package com.thed.zephyr.je.helper;


import com.atlassian.crowd.embedded.api.User;
import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.I18nHelper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.service.ColumnLayoutItemManager;
import com.thed.zephyr.je.service.ZFJCacheService;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.vo.ColumnItemLayoutBean;
import com.thed.zephyr.je.vo.ColumnLayoutBean;
import com.thed.zephyr.je.zql.model.ColumnLayoutItem;
import com.thed.zephyr.je.zql.model.ZFJColumnLayout;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ColumnItemLayoutBeanComparator;
import com.thed.zephyr.util.ZephyrCacheControl;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.stream.Collectors;


public class ZNavResourceHelper {
    protected final Logger log = Logger.getLogger(ZNavResourceHelper.class);

    private static final String CUSTOM_FIELD_ENAV_KEY = "-ENAV";
	private final ColumnLayoutItemManager columnLayoutItemManager;
	private final ZephyrCustomFieldManager zephyrCustomFieldManager;
    private final ZFJCacheService zfjCacheService;
    private final JiraAuthenticationContext authContext;
	
	public ZNavResourceHelper(ColumnLayoutItemManager columnLayoutItemManager, ZephyrCustomFieldManager zephyrCustomFieldManager,
                              ZFJCacheService zfjCacheService,JiraAuthenticationContext authContext) {
		this.columnLayoutItemManager=columnLayoutItemManager;
		this.zephyrCustomFieldManager = zephyrCustomFieldManager;
		this.zfjCacheService = zfjCacheService;
		this.authContext = authContext;
	}
	

	/**
	 * Find Available columns for Execution Navigator Column chooser
	 * @param loggedInUser
	 * @return
	 */
	public ColumnLayoutBean findAvailableColumns(User loggedInUser,int columnLayoutId,Integer zqlFilterId) {
		String userKey = UserCompatibilityHelper.getKeyForUser(loggedInUser);
		List<ColumnLayoutItem> columnLayoutItems =  columnLayoutItemManager.getColumnLayoutItems(userKey,zqlFilterId);
		return createColumnLayoutResponse(userKey,zqlFilterId,columnLayoutItems,columnLayoutId);
	}

	/**
	 * Build columnLayoutBean response
	 * @param userKey
	 * @param zqlFilterId
	 * @param columnLayoutItems
	 * @return
	 */
	private ColumnLayoutBean createColumnLayoutResponse(String userKey, Integer zqlFilterId,
			List<ColumnLayoutItem> columnLayoutItems,int columnLayoutId) {
		ColumnLayoutBean columnLayoutBean = new ColumnLayoutBean();
		columnLayoutBean.setUserName(userKey);
		columnLayoutBean.setExecutionFilterId(zqlFilterId);
		List<ColumnItemLayoutBean> items = new ArrayList<ColumnItemLayoutBean>();
		final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
        /*For backward compatibility, we will check both english and translated name*/
		ImmutableMap<String, String> layOutItems = ImmutableMap.<String, String>builder().put("Cycle Name", i18nHelper.getText("enav.newcycle.name.label"))
                                                    .put("Issue Key",i18nHelper.getText("cycle.reorder.executions.issue.label"))
                                                    .put("Test Summary", i18nHelper.getText("execute.test.testsummary.label"))
													.put("Labels", i18nHelper.getText("issue.field.labels"))
                                                    .put("Project Name",i18nHelper.getText("enav.projectname.label"))
                                                    .put("Priority",i18nHelper.getText("project.cycle.addTests.priority.label"))
                                                    .put("Component",i18nHelper.getText("je.gadget.common.component.label"))
                                                    .put("Version",i18nHelper.getText("je.gadget.common.version.label"))
                                                    .put("Execution Status",i18nHelper.getText("execute.test.executionstatus.label"))
                                                    .put("Executed By", i18nHelper.getText("project.cycle.schedule.table.column.executedBy"))
                                                    .put("Executed On", i18nHelper.getText("project.cycle.schedule.table.column.executedOn"))
                                                    .put("Creation Date", i18nHelper.getText("plugin.license.storage.admin.license.attribute.creationdate.title"))
                                                    .put("Execution Defect(s)", i18nHelper.getText("enav.search.execution.defects"))
                                                    .put("Assignee", i18nHelper.getText("project.cycle.schedule.table.column.assignee"))
													.put("Folder Name", i18nHelper.getText("enav.newfolder.name.label"))
													.put("Estimated Time", i18nHelper.getText("execute.test.workflow.estimated.time.label"))
													.put("Logged Time", i18nHelper.getText("execute.test.workflow.logged.time.label")).build();
		Map<String, Boolean> initialVisibleFalseColumnsMap = getVisibleFalseColumnsMap();
		for(Map.Entry<String, String> itemEntry : layOutItems.entrySet()) {
			ColumnItemLayoutBean columnItemLayoutBean = new ColumnItemLayoutBean();
			columnItemLayoutBean.setFilterIdentifier(itemEntry.getValue());
			columnItemLayoutBean.setVisible(false);

			if(columnLayoutId <= 0 && (columnLayoutItems == null || columnLayoutItems.size() == 0)) {
				//Changing the visibility of the columns to true for defaults.
				if(initialVisibleFalseColumnsMap.containsKey(itemEntry.getKey())) {
					columnItemLayoutBean.setVisible(false);
				} else {
					columnItemLayoutBean.setVisible(true);
				}
			}

			if(columnLayoutItems != null && columnLayoutItems.size() > 0) {
				for(ColumnLayoutItem columnLayoutItem : columnLayoutItems) {
					if(columnLayoutItem.getZFJColumnLayout() != null) {
						columnLayoutId = columnLayoutItem.getZFJColumnLayout().getID();
						if(StringUtils.equalsIgnoreCase(itemEntry.getKey(), columnLayoutItem.getFieldIdentifier()) || StringUtils.equalsIgnoreCase(itemEntry.getValue(), columnLayoutItem.getFieldIdentifier())) {
                        //if(columnLayoutItem.getZFJColumnLayout() != null) {
                            columnItemLayoutBean.setId(columnLayoutItem.getID());
                            columnItemLayoutBean.setOrderId(columnLayoutItem.getOrderId());
                            columnItemLayoutBean.setVisible(true);
                        }
					}
				}
			}
			items.add(columnItemLayoutBean);
		}

        CustomField[] customFields = zephyrCustomFieldManager.getAllCustomFieldsByEntityType(ApplicationConstants.ENTITY_TYPE.EXECUTION.name());

		if(Objects.nonNull(customFields) && customFields.length > 0) {
            if(columnLayoutItems != null && columnLayoutItems.size() > 0) {
                Map<Integer,ColumnLayoutItem> customFieldColumnLayoutItems = columnLayoutItems.stream().filter(Objects::nonNull).filter(customField -> Objects.nonNull(customField.getCustomFieldId())).collect(Collectors.toMap(ColumnLayoutItem::getCustomFieldId, p->p));
				Set<String> customFieldNameSet = new HashSet<>();
				List<CustomField> customFieldDistinctList = Arrays.asList(customFields).stream()
						.filter(e -> customFieldNameSet.add(e.getName()))
						.collect(Collectors.toList());

				for(CustomField customField : customFieldDistinctList){
                    ColumnItemLayoutBean columnItemLayoutBean = new ColumnItemLayoutBean();
                    if(MapUtils.isNotEmpty(customFieldColumnLayoutItems) &&
                            customFieldColumnLayoutItems.containsKey(customField.getID())) {
                        ColumnLayoutItem columnLayoutItem = customFieldColumnLayoutItems.get(customField.getID());
                        columnLayoutId = columnLayoutItem.getZFJColumnLayout().getID();
                        columnItemLayoutBean.setFilterIdentifier(customField.getName());
                        columnItemLayoutBean.setId(columnLayoutItem.getID());
                        columnItemLayoutBean.setOrderId(columnLayoutItem.getOrderId());
                        columnItemLayoutBean.setCustomFieldId(customField.getID());
                        columnItemLayoutBean.setVisible(true);
                    } else {
                        columnItemLayoutBean.setFilterIdentifier(customField.getName());
                        columnItemLayoutBean.setVisible(false);
                        columnItemLayoutBean.setCustomFieldId(customField.getID());
                    }
                    items.add(columnItemLayoutBean);
                }
            }else {
                Arrays.asList(customFields).stream().forEach(customField -> {
                    ColumnItemLayoutBean columnItemLayoutBean = new ColumnItemLayoutBean();
                    columnItemLayoutBean.setFilterIdentifier(customField.getName());
                    columnItemLayoutBean.setVisible(false);
                    columnItemLayoutBean.setCustomFieldId(customField.getID());
                    items.add(columnItemLayoutBean);

                });
            }
        }

		Collections.sort(items,getComparator());
		columnLayoutBean.setId(columnLayoutId != 0 ? columnLayoutId : null);
		columnLayoutBean.setColumnItemBean(items);

		return columnLayoutBean;
	}

    /**
	 * Save Column sorter for Execution Navigator Column chooser
	 * @param loggedInUser
	 * @param columnLayoutBean
	 * @return
	 */
	public Response saveAvailableColumns(User loggedInUser,ColumnLayoutBean columnLayoutBean) {
		String userKey = UserCompatibilityHelper.getKeyForUser(loggedInUser);
        ZFJColumnLayout columnLayout = null;
        // check if for given user and zqlFilter column configuration already exist
        List<ColumnLayoutItem> columnLayoutItems =  columnLayoutItemManager.getColumnLayoutItems(userKey,columnLayoutBean.getExecutionFilterId());
        // if there is no column configuration for give user and zqlFilter, create one
        if(null == columnLayoutItems || columnLayoutItems.isEmpty())
            columnLayout =  columnLayoutItemManager.saveZFJColumnLayoutItem(userKey, columnLayoutBean.getExecutionFilterId(), columnLayoutBean);
        else{
            log.error("Column configuration for given User and ZQLFilter already exists!");
            final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
            Response.ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
            builder.type(MediaType.APPLICATION_JSON);
            builder.entity(ImmutableMap.of("Error", i18nHelper.getText("zephyr.je.znav.column.config.save.error.duplicate")));
            return builder.build();
        }

		if(columnLayout == null) {
			return Response.status(Status.NOT_MODIFIED).entity(ImmutableMap.of("Error", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id","column layout data"))).cacheControl(ZephyrCacheControl.never()).build();
		}
		return Response.ok().entity(findAvailableColumns(loggedInUser, columnLayout.getID(),columnLayoutBean.getExecutionFilterId())).cacheControl(ZephyrCacheControl.never()).build();
	}
	
	/**
	 * Update Column sorter for Execution Navigator Column chooser
	 * @param id
	 * @param loggedInUser
	 * @param columnLayoutBean
	 * @return
	 */
	public Response updateAvailableColumns(final Integer id, final User loggedInUser,final ColumnLayoutBean columnLayoutBean) {
		String userKey = UserCompatibilityHelper.getKeyForUser(loggedInUser);
		ZFJColumnLayout columnLayout = columnLayoutItemManager.updateZFJColumnLayoutItem(id, userKey, columnLayoutBean.getExecutionFilterId(), columnLayoutBean);
		if (columnLayout == null) {
			return Response.status(Status.NOT_MODIFIED).entity(ImmutableMap.of("Error", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id","column layout data"))).cacheControl(ZephyrCacheControl.never()).build();
		}
		return Response.ok().entity(findAvailableColumns(loggedInUser, columnLayout.getID(), columnLayoutBean.getExecutionFilterId())).cacheControl(ZephyrCacheControl.never()).build();
	}
	
	protected Comparator getComparator() {
		return ColumnItemLayoutBeanComparator.INSTANCE;
	}

    public boolean havePermissionOnZqlFilter(Integer zqlFilterId, String userKey){
        if(null == zqlFilterId || zqlFilterId <=0 )
            return true;
        return columnLayoutItemManager.havePermissionOnZqlFilter(zqlFilterId, userKey);
    }
    
    private Map<String, Boolean> getVisibleFalseColumnsMap() {
    	Map<String, Boolean> visibleFalseColumnsMap = new HashMap<String, Boolean>();
    	visibleFalseColumnsMap.put("Labels", true);
    	return visibleFalseColumnsMap;
    }
}
