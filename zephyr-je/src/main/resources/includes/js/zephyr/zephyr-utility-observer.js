function ObserverUtility() {
  this.observer = new ResizeObserver(function(mutations) {
    mutations.forEach(function(mutation) {
        //console.log('mutation', mutation);
        if(mutation.target.id === 'editable-schedule-defects') {
          resizeTextarea();
        }
    });
  });

  function resizeTextarea() {
    if(!AJS.$('.jira-multi-select').find('textarea')[0] || !AJS.$("#editable-schedule-defects .representation ul li:last-child")[0]) {
      return;
    }
    var t = AJS.$('.jira-multi-select').find('textarea')[0];
    t.style.height = "1px";
    t.style.height = AJS.$('.jira-multi-select').find('.representation').height() + 10 +"px";
    t.style.paddingLeft = AJS.$(".representation ul li:last-child").offset().left + AJS.$(".representation ul li:last-child").width() - AJS.$(".representation ul").offset().left + 8 + "px";
    t.style.paddingTop = AJS.$(".representation ul li:last-child").offset().top - AJS.$(".representation ul").offset().top + 4 + "px";
  }
}