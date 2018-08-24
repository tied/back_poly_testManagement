var isLoadedInIframe = function() {
    try {
        return (window !== window.parent);
    } catch(e) {
        return false;
    }
}

var InitPageContent = function(initCallback) {
    if(isLoadedInIframe()) {
        AJS.$(window).load(function(){
            initCallback();
        });
    } else {
        AJS.$(document).ready(function(){
            initCallback();
        });
    }
}

InitPageContent(function(){
    if (!window.zEncKeyFld && !window.zEncKeyVal)
        AJS.$.ajax({
            url: AJS.contextPath() + "/secure/ZephyrEncKeyLoadAction.jspa",
            type: 'GET',
            async: false,
            dataType: "html",
            success: function (data) {
                if (data) {
                    var parser = new DOMParser();
                    var htmlDoc = parser.parseFromString(data, "text/html");
                    if (htmlDoc) {
                        var encKeyFld = htmlDoc.getElementById('zEncKeyFld').value;
                        var encKeyVal = htmlDoc.getElementById('zEncKeyVal').value;
                        var showWF = htmlDoc.getElementById('zshowWF').value;

                        window.zEncKeyFld = encKeyFld;
                        window.zEncKeyVal = encKeyVal;
                        window.zshowWF = showWF;
                    }
                }
            }
        });
});