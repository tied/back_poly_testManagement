 var InAppMessage = function(options) {

     var inAppServerUrl = window.inAppMessageUrlFieldValue || document.getElementById('inAppMessageUrlField').value;
     if(inAppServerUrl && inAppServerUrl.substr(inAppServerUrl.length -1) === '/') {
 		var inAppServerUrl = inAppServerUrl.substr(0, inAppServerUrl.length -1)
 	}

 	var inAppIframeSrc = inAppServerUrl + '/index';
 	var options = options;
 	 
 	var loadInAppWrapper = function() {

 		var inAppWrapper = createInAppWrapper(options.className);
 		var iFrame = document.createElement('iframe');
 			iFrame.src = inAppIframeSrc;
 		inAppWrapper.appendChild(iFrame);
 
 		if(options.containerId) {
 			document.getElementById(options.containerId).appendChild(inAppWrapper);
 		} else {
 			document.body.appendChild(inAppWrapper);
 		}
 	};

 	var createInAppWrapper = function(inAppClassName) {
 		var inAppWrapper = document.createElement('div');
 			inAppWrapper.className = 'in-app-wrapper ' + inAppClassName;
 		if(advOptIn == 'false') {
 			inAppWrapper.className += ' opt-in-window-wrapper';
 		}
 		return inAppWrapper;
 	};
 	 
 	var toggleAdd = function () {
		var wrapper = document.getElementsByClassName('in-app-wrapper')[0];
        za.track({
            'event': "Click on in app message",
            "eventType": "Click"
        }, function(res){
            console.log('Analytics test: -> ',res);
        });

		if (!wrapper) {
			loadInAppWrapper();
			document.getElementById('in-app-toggle-icon').classList.add('inapp-clicked');
		} else {
			if (wrapper.style.display == "block") {
				document.getElementById('in-app-toggle-icon').classList.remove('inapp-clicked');
				wrapper.style.display = 'none';
			} else {
				document.getElementById('in-app-toggle-icon').classList.add('inapp-clicked');
				wrapper.style.display = 'block';
				setNotificationsCount('0');
			}
		}
	};

	var postMessageToInAppFrame = function() {
		for (var i=0;i<window.frames.length;i++) {
			var zephyrBaseUrl = InAppMessage && InAppMessage.prototype && InAppMessage.prototype.zephyrBaseUrl;
			zephyrBaseUrl = zephyrBaseUrl || document.getElementById('zephyrBaseUrl').value;
			if(contextPath) {
				zephyrBaseUrl = zephyrBaseUrl.split(contextPath)[0];
			}

			window.frames[i].postMessage({isForInApp : true, baseUrl : zephyrBaseUrl, optInStatus: advOptIn, productType: 'zfj'}, "*");
		}
	};


	var registerEvents = function() {
		window.addEventListener("message", function(data) {
			if (data.data.isFromInApp) {
				if (data.data.count) {
					var inAppShowHideButton = document.getElementById('in-app-toggle-icon');
					setNotificationsCount(data.data.count);
				} else if (data.data.click) {
				  	if (za != undefined) {
			            za.track({'event': ZephyrEvents.IN_APP_DISTRIBUTION_CLICK, 'analyticsType':'click', data:{distributionId : data.data.click}},
			                function (res) {
			                    console.log('Analytics test: -> ',res);
			            });
			        }
				} else if (data.data.baseUrl) {
					postMessageToInAppFrame();
				} else if(data.data.isOptIn) {
					if(!data.data.isOptInStatus) {
						optOutConfirmationDialog(data.data.isOptInStatus);
					} else {
						updateOptInStatus(data.data.isOptInStatus);
					}
				}
			}
		}, false);
	};

	registerEvents();

	var optOutConfirmationDialog = function(isOptInStatus) {
		var dialog = new JIRA.FormDialog({
	        id: "opt-in-confirmation-dialog",
	        content: function (callback) {
	            var innerHtmlStr = ZEPHYR.Templates.InAppMessage.optOutConfirmation();
	            callback(innerHtmlStr);
	        },
	        submitHandler: function (e) {
	        	updateOptInStatus(isOptInStatus, function() {
	        		dialog.hide();
	        	});
	            e.preventDefault();
	        }
	    });

	    dialog.show();
	};

	var updateOptInStatus = function(isOptInStatus, cb) {
		AJS.$.ajax({
			url: contextPath + '/rest/zephyr/latest/preference/advoptin?isOptIn=' + isOptInStatus + '&_=' + new Date().getTime(),
			type: 'PUT',
			complete: function(xhr) {
				var res = JSON.parse(xhr.responseText);
				advOptIn = res.advOptIn;
				if(cb && typeof cb === 'function') {
					cb();
				}
				if(advOptIn == 'true') {
					AJS.$('.in-app-wrapper').removeClass('opt-in-window-wrapper');
				} else {
					AJS.$('.in-app-wrapper').addClass('opt-in-window-wrapper');
					za.track({'event': ZephyrEvents.IN_APP_OPT_OUT, 'analyticsType':'click'},
		                function (res) {
		                    console.log('Analytics test: -> ',res);
		            });
				}
				postMessageToInAppFrame();
			}
		});
	}

	var setNotificationsCount = function(count) {
		var inAppShowHideButton = document.getElementById('in-app-toggle-icon');
		inAppShowHideButton.setAttribute('data-count' , count);
	}

	var advOptIn = null;

 	this.createShowHideButton = function () {
 		if(!inAppServerUrl) {
	 		return;
	 	}
	 	var that = this;
		AJS.$.ajax({
			url: contextPath + '/rest/zephyr/latest/preference/advoptin?_=' + new Date().getTime(),
			type: 'GET',
			complete: function(xhr) {
				var res = JSON.parse(xhr.responseText);
				advOptIn = res.advOptIn;
				var inAppShowHideButton = document.createElement('div');
				inAppShowHideButton.id = 'in-app-toggle-icon';
				inAppShowHideButton.className = 'in-app-toggle-icon ' + options.className;
				inAppShowHideButton.addEventListener('click' , toggleAdd.bind(this));
				inAppShowHideButton.setAttribute('data-count' , '0');
				inAppShowHideButtonDom = inAppShowHideButton;
		 
				if(options.containerId) {
		 			document.getElementById(options.containerId).appendChild(inAppShowHideButton);
		 		} else {
		 			document.body.appendChild(inAppShowHideButton);
		 		}

				loadInAppWrapper();
			}
		});
	};

	 //Receive distribution events and pass to analytic service
     var eventMethod = window.addEventListener ? "addEventListener" : "attachEvent";
     var eventer = window[eventMethod];
     var messageEvent = eventMethod == "attachEvent" ? "onmessage" : "message";
     eventer(messageEvent,function(e) {
         var key = e.message ? "message" : "data";
         var data = e[key];
		 za.track(data, function (res) {
				 console.log('Analytics test: -> ',res);
			 });
      },false);
 }