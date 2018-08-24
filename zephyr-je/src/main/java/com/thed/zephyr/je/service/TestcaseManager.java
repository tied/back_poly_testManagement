package com.thed.zephyr.je.service;

import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.je.vo.TestCase;
import com.thed.zephyr.je.vo.TestCase.Response;

public interface TestcaseManager {
	public Response createTestCase(TestCase testCase, ApplicationUser user) throws Exception;
}
