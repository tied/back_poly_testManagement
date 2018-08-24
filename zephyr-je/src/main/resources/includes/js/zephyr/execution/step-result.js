ZEPHYR.Schedule.Execution.TestStepResult = Backbone.Model.extend();

ZEPHYR.Schedule.Execution.TestStepResultCollection = Backbone.Collection.extend({
    model:ZEPHYR.Schedule.Execution.TestStepResult,
    url:function(){
    	return getRestURL() + "/stepResult"
    },
    /*This is for backward compatibility with older version of Backbone */
    where: function(attrs) {
        if (_.isEmpty(attrs)) return [];
        return this.filter(function(model) {
          for (var key in attrs) {
            if (attrs[key] !== model.get(key)) return false;
          }
          return true;
        });
      }
});