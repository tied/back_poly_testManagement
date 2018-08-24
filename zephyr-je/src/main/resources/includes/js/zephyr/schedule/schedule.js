ZEPHYR.Issue.Schedule = Backbone.Model.extend();

ZEPHYR.Issue.ScheduleCollection = Backbone.Collection.extend({
    model:ZEPHYR.Schedule.Execution.TestStepResult,
    url:function(){
    	return getRestURL() + "/execution"
    }
});

ZEPHYR.Issue.ScheduleModel = Backbone.Model.extend({
   url:function(){
    	var url = getRestURL() + "/execution/" + (this.executionId ? this.executionId : AJS.$('#zScheduleId').val()) + (this.executionId ? "?expand=checksteps" : "?expand=executionStatus");
    	return url;
    }
});
