package com.thed.zephyr.util.optionalservice;

import com.atlassian.greenhopper.model.validation.ErrorCollection;
import com.atlassian.greenhopper.service.ServiceOutcome;
import com.atlassian.greenhopper.service.sprint.Sprint;
import com.atlassian.greenhopper.service.sprint.SprintService;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Optional;
import org.apache.log4j.Logger;

/**
 * Created by smangal on 9/29/15.
 */
public class GHSprintService {
    private final com.atlassian.greenhopper.service.sprint.SprintService springService;
    private static final Logger log = Logger.getLogger(GHSprintService.class);

    public GHSprintService(SprintService springService) {
        this.springService = springService;
    }

    public Optional<Sprint> getSprint(ApplicationUser user, Long sprintId){
        ServiceOutcome<Sprint> sprintResult = springService.getSprint(user, sprintId);
        if(sprintResult.isValid()){
            return Optional.of(sprintResult.getValue());
        }else{
            //ErrorCollection[errors=[ErrorCollection.ErrorItem[contextId=<null>,messageKey=gh.sprint.error.not.found,params={}]],reasons=[NOT_FOUND]]
            ErrorCollection.Reason reason = sprintResult.getErrors().getDefinitiveReason();
            if(reason == ErrorCollection.Reason.NOT_FOUND){
                log.warn("Sprint not found with id " + sprintId);
                return Optional.absent();
            }
            throw new RuntimeException("Error in fetching Sprint \n"+ sprintResult.getErrors().toString());
        }
    }
}
