/**
 *
 */
package com.thed.zephyr.je.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.beehive.compat.ClusterLock;
import com.atlassian.beehive.compat.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.rest.delegate.ExecutionWorkflowResourceDelegate;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.je.vo.TeststepBean;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author smangal
 */
public class TeststepManagerImpl implements TeststepManager {

    protected final Logger log = Logger.getLogger(TeststepManagerImpl.class);
	private final ActiveObjects ao;
	private final StepResultManager stepResultManager;
	private final ClusterLockService clusterLockService;

	public TeststepManagerImpl(ActiveObjects ao, StepResultManager stepResultManager, ClusterLockServiceFactory clusterLockServiceFactory) {
		this.ao = checkNotNull(ao);
		this.stepResultManager = stepResultManager;
		this.clusterLockService = clusterLockServiceFactory.getClusterLockService();
	}

	@Override
	public Teststep getTeststep(Integer id) {
		Teststep step = ao.get(Teststep.class, id);
		return step;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.thed.jira.plugin.service.TeststepManager#getTeststeps(java.lang.Long)
	 */
	@Override
	public List<Teststep> getTeststeps(Long issueId, Optional<Integer> offset, Optional<Integer> limit) {
		Query query = Query.select().where("ISSUE_ID = ?", issueId).order(" ORDER_ID ASC ");
		if(offset.isPresent() && limit.isPresent()){
			query.offset(offset.get().intValue());
			query.limit(limit.get().intValue());
		}
		Teststep[] steps = ao.find(Teststep.class, query);
		return Arrays.asList(steps);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.thed.jira.plugin.service.TeststepManager#copySteps(java.lang.Long,
	 * java.lang.Long)
	 */
	@Override
	public Boolean copySteps(Long fromIssueId, Long toIssueId) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.thed.jira.plugin.service.TeststepManager#copyStepsInBulk(java.util.Map)
	 */
	@Override
	public Boolean copyStepsInBulk(Map<Long, Long> fromIssueToIssueIdMap) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.thed.jira.plugin.service.TeststepManager#removeTeststep(java.lang.Long)
	 */
	@Override
	public List<Teststep> removeTeststep(Integer id) {
		Teststep step = ao.get(Teststep.class, id);

		if (step != null) {
			Long issueId = step.getIssueId();

			Teststep[] steps = new Teststep[] { step };
			deleteTeststeps(steps);
			List<Teststep> teststeps = getTeststeps(issueId, Optional.empty(), Optional.empty());
			updateOrderId(teststeps);
			return teststeps;
		}

		// This will happen only if user entered step is not available in system.
		// Something is gone wrong!
		return new ArrayList<Teststep>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.thed.jira.plugin.service.TeststepManager#removeTeststep(java.lang.Long)
	 */
	@Override
	public void removeTeststeps(Long issueId) {
		Teststep[] steps = ao.find(Teststep.class, " ISSUE_ID = ?", issueId);
		deleteTeststeps(steps);
	}

	private Integer deleteTeststeps(final Teststep[] steps) {

		Integer noOfStepResults = ao.executeInTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction() {
				List<Integer> stepIds = new ArrayList<Integer>();
				for (Teststep step : steps) {
					stepIds.add(step.getID());
					stepResultManager.removeStepResultByStep(step.getID());
                    /**
                     * to delete the associated custom fields data for the step id.
                     */
					TestStepCf[] testStepCfs = ao.find(TestStepCf.class,"TEST_STEP_ID = ?",step.getID());
					if (Objects.nonNull(testStepCfs) && testStepCfs.length > 0) {
						ao.delete(testStepCfs);
					}
				}

				ao.delete(steps);
				return stepIds.size();
			}
		});
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.thed.jira.plugin.service.TeststepManager#saveTeststeps(java.util.List)
	 */
	@Override
	public Boolean saveTeststeps(List<Teststep> steps) {
		for (Teststep step : steps)
			step.save();
		return true;
	}

	/**
	 * This method assumes provided list has elements in right order and will update
	 * orderIds
	 *
	 * @param steps
	 * @return
	 */
	@Override
	public List<Teststep> updateOrderId(final List<Teststep> steps) {
		ao.executeInTransaction(new TransactionCallback<Boolean>() {
			@Override
			public Boolean doInTransaction() {
				int orderId = 1;
				for (Teststep step : steps) {
					step.setOrderId(orderId++);
					step.save();
				}
				return true;
			}
		});
		return steps;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.thed.jira.plugin.service.TeststepManager#saveTeststeps(java.util.List)
	 */
	@Override
	public List<Teststep> saveTeststepProperties(List<Map<String, Object>> stepsProperties) {
		List<Teststep> steps = new ArrayList<Teststep>();
		for (Map<String, Object> stepProperty : stepsProperties) {
			Teststep step = ao.create(Teststep.class, stepProperty);
			steps.add(step);
		}
		return steps;
	}

	@Override
	public Teststep createTeststep(TeststepBean stepBean, Long issueId) {
		// if(stepBean.orderId == null){
		// without offset, limit doesnt work in Oracle - Bug 4448
		Query query = Query.select().where("ISSUE_ID = ?", issueId).order(" ORDER_ID DESC ").offset(0).limit(1);
		Teststep[] oldSteps = ao.find(Teststep.class, query);
		if (oldSteps != null && oldSteps.length == 1) {
			stepBean.orderId = oldSteps[0].getOrderId() + 1;
		} else {
			stepBean.orderId = 1;
		}
		// }

		User user = JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext());
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("ORDER_ID", stepBean.orderId);
		values.put("ISSUE_ID", issueId);
		values.put("STEP", stepBean.step);
		values.put("DATA", stepBean.data);
		values.put("RESULT", stepBean.result);
		values.put("CREATED_BY", UserCompatibilityHelper.getKeyForUser(user));
		values.put("MODIFIED_BY", UserCompatibilityHelper.getKeyForUser(user));
		Teststep step = ao.create(Teststep.class, values);
		return step;
	}

	@Override
	public boolean verifyAndAddStepResult(Schedule schedule) {
		// Verify and Create Test Steps. Moved it from ExecuteTestAction. Should we move
		// this to a REST call???
		List<Teststep> steps = getTeststeps(schedule.getIssueId().longValue(), Optional.empty(), Optional.empty());
		List<Integer> createStepResultList = new ArrayList<Integer>();
		String lockName = "zeph-sched-" + schedule.getID();
		ClusterLock lock = clusterLockService.getLockForName(lockName);
		lock.lock();
		try {

            List<String> disabledProjectIdsList = JiraUtil.getDisabledProjectIdsList(ConfigurationConstants.ZEPHYR_DISABLE_PROJECT_EXEC_WORKFLOW);
            boolean isExecutionWorkflowEnabled = Boolean.TRUE;
            if(CollectionUtils.isNotEmpty(disabledProjectIdsList)) {
                if(disabledProjectIdsList.contains(String.valueOf(schedule.getProjectId()))) {
                    isExecutionWorkflowEnabled = Boolean.FALSE;
                }
            }

		    String executionWorkflowStatus = null != schedule.getExecutionWorkflowStatus() ? schedule.getExecutionWorkflowStatus().name() : StringUtils.EMPTY;

		    if(isExecutionWorkflowEnabled && StringUtils.isNotBlank(executionWorkflowStatus) && ExecutionWorkflowStatus.COMPLETED.name().equalsIgnoreCase(executionWorkflowStatus)) {
                log.debug("Execution workflow is completed hence will not add the newly created steps.");
            } else {
                for (Teststep step : steps) {
                    createStepResultList.add(step.getID());
                }

                List<StepResult> stepResults = stepResultManager.getStepResultsBySchedule(Integer.valueOf(schedule.getID()));
                for (StepResult sResult : stepResults) {
                    createStepResultList.remove(Integer.valueOf(sResult.getStep().getID()));
                }

                // Add "unexecuted" stepResult for all step which are not executed and don't
                // have entry for them in StepResult table.
                for (Integer noResultStepId : createStepResultList) {
                    // We need to have step_result_id so that step attachments can get the
                    // association available.
                    // This is the only place where we can create step_results as we have both
                    // step_id and schedule_id.
                    // May be we should try to find out how to create step_results in bulk to
                    // improve performance.
                    Map<String, Object> resultProperties = new HashMap<String, Object>();
                    resultProperties.put("SCHEDULE_ID", Integer.valueOf(schedule.getID()));
                    resultProperties.put("STEP_ID", Integer.valueOf(noResultStepId));
                    resultProperties.put("PROJECT_ID", schedule.getProjectId());
                    resultProperties.put("STATUS", "-1");
                    stepResultManager.addStepResult(resultProperties);
                }
            }

		} catch (Exception e) {
			log.fatal("", e);
			return false;
		} finally {
			if (lock != null)
				lock.unlock();
		}
		return true;
	}

	@Override
	public List<TestStepCf> getCustomFieldValuesForTeststep(Integer entityId) {
		Query query = Query.select();
		query.where("TEST_STEP_ID = ?", entityId);

		TestStepCf[] testStepCfs = ao.find(TestStepCf.class, query);

		if (Objects.nonNull(testStepCfs)) {
			return Arrays.asList(testStepCfs);
		}

		return Collections.EMPTY_LIST;
	}

	@Override
	public Integer getTotalStepCount(Long issueId) {
		Query query = Query.select().where("ISSUE_ID = ?", issueId);
		return ao.count(Teststep.class, query);
	}

    @Override
    public Teststep[] getNextTeststep(Long issueId, Integer nextOffsetValue, Integer limit) {
        Query query = Query.select().where("ISSUE_ID = ?", issueId).order(" ORDER_ID ASC ");
        query.offset(nextOffsetValue).limit(limit);
        Teststep[] steps = ao.find(Teststep.class, query);

        if (Objects.nonNull(steps) && steps.length > 0) {
            return steps;
        }

        return null;
    }

	@Override
	public Teststep getPrevTeststep(Long issueId, Integer prevOffsetValue, Integer limit) {
		Query query = Query.select().where("ISSUE_ID = ?", issueId).order(" ORDER_ID ASC ");
		query.offset(prevOffsetValue).limit(limit);
		Teststep[] steps = ao.find(Teststep.class, query);

		if (Objects.nonNull(steps) && steps.length > 0) {
			return steps[0];
		}
		return null;
	}
}
