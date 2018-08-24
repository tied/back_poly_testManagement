AJS.$.namespace("Zephyr.Datacenter.Info")
if (typeof ZEPHYR == 'undefined') {
	var ZEPHYR = {};
}
Zephyr.Datacenter = (function () {
	var infoClass = new function() {
		this.syncUpData = {}
		this.formData = {}

		this.weekOrder = {
			"1" : "first",
			"2" : "second",
			"3" : "third",
			"4" : "fourth",
			"L" : "last"
		}

		this.dayOrder = {
			"1" : "sunday",
			"2" : "monday",
			"3" : "tuesday",
			"4" : "wednesday",
			"5" : "thursday",
			"6" : "friday",
			"7" : "saturday"
		}

		this.interval = {
			"180" : "every 3 hours",
			"120" : "every 2 hours",
			"60" : "every hour",
			"30" : "every 30 minutes",
			"15" : "every 15 minutes"
		}

		this.integrityCheckerPaginationObject = {
			zicTotalExecutionCounttab : 0,
			zicTotalCycleCounttab : 0,
			zicExecutionCountByCycletab : 0,
			zicExecutionCountByFoldertab : 0,
			zicIssueCountByProjecttab : 0,
			zicTeststepResultCountByExecutiontab : 0,
 			zicTeststepCountByIssuetab : 0
		}

		this.getsupportLogs = function() {
			that = this;
			AJS.$("#support-tool-tab .overlay-icon").removeClass('hidden');
			AJS.$('#downloadzfjlogsusingJIRA').hide();
			AJS.$("#downloadzfjlogs").attr("disabled", true);
			var zfjlogs = AJS.$("#zephyr-je-zfjlog").is(":checked");
			var zfjshared = AJS.$("#zephyr-je-zfjshared").is(":checked");
			var zfjdb = false;
			if(zfjshared==true){
				zfjdb = true;
			}
			var zfjtomcatlog = AJS.$("#zephyr-je-zfjserverlog").is(":checked");
			var queryParam = "zfjlogs=" + zfjlogs +"&";
			queryParam += "zfjdb=" + zfjdb +"&";
			queryParam += "zfjshared=" + zfjshared +"&";
			queryParam += "zfjtomcatlog=" + zfjtomcatlog;
			if (zfjlogs || zfjshared || zfjdb || zfjtomcatlog) {
				var progressBarContainer = '';
				jQuery.ajax({
						url : contextPath + "/rest/zephyr/latest/datacenter/downloadSupportzip?" + queryParam,
						type : "GET",
						complete : function(jqXHR, textStatus) {
							if (jqXHR.status == 200) {
								if (jQuery.parseJSON(jqXHR.responseText).url != undefined) {
									if(jQuery.parseJSON(jqXHR.responseText).url=="redirectToJiraSupport"){
										AJS.$('#downloadzfjlogsusingJIRA').show();
										AJS.$('#redirecttoJIRASupport').show();
										AJS.$('#downloadzfjlogs').hide();
									}else{
										AJS.$('#downloadzfjlogsusingJIRA').hide();
										AJS.$('#redirecttoJIRASupport').hide();
										AJS.$('#downloadzfjlogs').show();
										var link = document.createElement("a");
										link.download = "supportTool.zip";
										link.href = jQuery
												.parseJSON(jqXHR.responseText).url;
										document.body.appendChild(link);
										link.click();
										$('#general-config-aui-message-bar').show().empty();
										$('#general-config-aui-message-bar').html('<span><strong style="color:green">' + AJS.I18n.getText('zephyr-je.supporttool.select.zipfile.downloaded.successful') + '</strong></span>');

										setTimeout(function(){
											$('#general-config-aui-message-bar').hide();
											}, 5000);
										document.body.removeChild(link);
										delete link;
									}
								} else {
									$('#general-config-aui-message-bar').show().empty();
									$('#general-config-aui-message-bar').html('<span><strong>' + jQuery.parseJSON(jqXHR.responseText).alreadyInprocess + '</strong></span>');
									setTimeout(function(){
										$('#general-config-aui-message-bar').hide();
										}, 5000);
								}
							}
							if (jqXHR.status == 401) {
								window.location = contextPath
										+ "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
							} else if (jqXHR.status >= 400) {
								AJS.$('#general-config-aui-message-bar').show().empty();
								AJS.$('#general-config-aui-message-bar').html('<span><strong style="color:red">' + AJS.I18n.getText('zephyr-je.supporttool.select.zipfile.downloaded.error') + '</strong></span>');
								setTimeout(function(){
									AJS.$('#general-config-aui-message-bar').hide();
									}, 5000);
							}
							AJS.$("#downloadzfjlogs").attr("disabled", false); 
							AJS.$("#support-tool-tab .overlay-icon").addClass('hidden');
						}
					});
			} else {
				AJS.$("#support-tool-tab .overlay-icon").addClass('hidden');
				$('#general-config-aui-message-bar').show().empty();
				$("#general-config-aui-message-bar").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr-je.supporttool.select.zipfile.label') + '</strong></span>');
				setTimeout(function(){
					$('#general-config-aui-message-bar').hide();
					}, 3000);
			}
		}

		this.initSyncupComponent = function() {
			this.syncUpData = {
				mode : 'daily',
				daysOfWeek : [],
				intervalValue : '0'
			}
			jQuery.ajax({
					url: contextPath + "/rest/zephyr/latest/datacenter/backupRecovery",
				type : "GET",
				accept : "GET",
					complete : function(jqXHR, textStatus) {
						if (jqXHR.status == 200) {
							this.formData = JSON.parse(jqXHR.responseText);
							this.displayScheduleStatus();
						}else{

						}
						if (jqXHR.status == 401) {
							window.location = contextPath
									+ "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
						}
					}.bind(this)
				});
			AJS.$(document).on('click', "#syncUpDaily-option", this.switchToDaily.bind(this));
			AJS.$(document).on('click', "#syncUpDaysOfWeek-option",	this.switchToDaysOfWeek.bind(this));
			AJS.$(document).on('click', "#syncUpDaysOfMonth-option",	 this.switchToDaysOfMonth.bind(this));
			AJS.$(document).on('click', "#syncUpAdvanced-option",	this.switchToAdvanced.bind(this));
			AJS.$(document).on('change', "#syncUpInterval", this.toggleFrequencyControl.bind(this));
			AJS.$(document).on('click', ".syncUpWeekdayCheckbox", this.syncUpWeekdayUpdate.bind(this));
			AJS.$(document).on('click', "#edit-recovery-settings", this.openSettingsPopup.bind(this));
			AJS.$(document).on('click', "#index-recovery-submit", this.submitFileNameForRecovery.bind(this));
		}

		this.displayScheduleStatus = function() {
			if(this.formData.recoveryEnabled === "true") {
				AJS.$(AJS.$('#cronScheduleStatus td')[1]).html('<strong class="status-active">ON</strong>');
				AJS.$(AJS.$('#cronScheduleDescription td')[1]).html('<strong>'+ this.getScheduleDescription() +'</strong>');
				AJS.$(AJS.$('#cronScheduleDirectory td')[1]).html('<strong>' + this.formData.rootPath + '</strong>');
				AJS.$('.scheduleInfo').css('display', 'table-row');
			} else {
				AJS.$(AJS.$('#cronScheduleStatus td')[1]).html('<strong class="status-inactive">OFF</strong>');
				AJS.$('.scheduleInfo').css('display', 'none');
			}
		}

		this.submitFileNameForRecovery = function() {
			var fileName = AJS.$('#index-recovery-file-name').val();
			if(!fileName.length) {
				AJS.$('.syncUpError').html('<span><strong style="color:red">' + AJS.I18n.getText('zephyr-je.recoverybackup.noFilename') + '</strong></span>');
				setTimeout(function() {
					AJS.$('.syncUpError').html('');
				}, 2000);
			} else {
				jQuery.ajax({
						url: contextPath + "/rest/zephyr/latest/datacenter/indexRecovery",
					type : "PUT",
					accept : "PUT",
					contentType :"application/json",
					dataType: "json",
					data: JSON.stringify({"indexRecoveryFileName":fileName}),
						complete : function(jqXHR, textStatus) {
							if (jqXHR.status == 401) {
								window.location = contextPath
										+ "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
							}
							if (jqXHR.status == 200) {
								AJS.$('.syncUpError').html('');
								AJS.$('.syncUpError').html('<span><strong style="color:green">' + AJS.I18n.getText('zephyr-je.recoverybackup.successful') + '</strong></span>');
							} else {
								AJS.$('.syncUpError').html('');
								var responseJson = JSON.parse(jqXHR.responseText);
								AJS.$('.syncUpError').html('<span><strong style="color:red">' + responseJson.error + '</strong></span>');
							}
						}
					});
			}
		}

		this.openSettingsPopup = function() {
			//hit api to get status
			jQuery.ajax({
					url: contextPath + "/rest/zephyr/latest/datacenter/backupRecovery",
				type : "GET",
				accept : "GET",
					complete : function(jqXHR, textStatus) {
						if (jqXHR.status == 200) {
							this.formData = JSON.parse(jqXHR.responseText);
							dialog = new JIRA.FormDialog({
									id: "edit-recovery-dialog",
									content: function (callback) {
											/*Short cut of creating view, move it to Backbone View and do it in render() */
				                        	var date = new Date();
				                        	var formattedDate = formatDate(date);
											var innerHtmlStr = ZEPHYR.Datacenter.cronSchedulerForm({formattedDate:formattedDate});
											callback(innerHtmlStr);
									},

									submitHandler: function (e) {
										this.runCronSyncJob();
										// dialog.hide();
									}.bind(this)
							}).bind(this);
							//form popuation
							dialog.show();

							AJS.$("input[name=recoveryEnabled][value=" + this.formData["recoveryEnabled"] + "]").attr('checked', 'checked');
							if(this.formData["recoveryEnabled"] == 'true') {

								AJS.$("input[name=syncUpScheduleDailyWeeklyMonthly][value=" + this.formData["mode"] + "]").trigger("click");
								switch(this.formData["mode"]) {
									case 'daily':
										AJS.$("#syncUpInterval").val(this.formData["interval"]);
										this.toggleFrequencyControl();
										if(this.formData["interval"] == "0") {
											AJS.$("#syncUpRunOnceHours").val(this.formData["onceHours"]);
											AJS.$("#syncUpRunOnceMinutes").val(this.formData["onceMinutes"]);
											AJS.$("#syncUpRunOnceMeridian").val(this.formData["onceMeridian"]);
										} else {
											AJS.$("#syncUpRunFromHours").val(this.formData["fromHours"]);
											AJS.$("#syncUpRunFromMeridian").val(this.formData["fromMeridian"]);
											AJS.$("#syncUpRunToHours").val(this.formData["toHours"]);
											AJS.$("#syncUpRunToMeridian").val(this.formData["toMeridian"]);
										}
										break;
									case 'daysOfWeek':
										AJS.$("#syncUpInterval").val(this.formData["interval"]);
										this.toggleFrequencyControl();
										if(this.formData["interval"] == "0") {
											AJS.$("#syncUpRunOnceHours").val(this.formData["onceHours"]);
											AJS.$("#syncUpRunOnceMinutes").val(this.formData["onceMinutes"]);
											AJS.$("#syncUpRunOnceMeridian").val(this.formData["onceMeridian"]);
										} else {
											AJS.$("#syncUpRunFromHours").val(this.formData["fromHours"]);
											AJS.$("#syncUpRunFromMeridian").val(this.formData["fromMeridian"]);
											AJS.$("#syncUpRunToHours").val(this.formData["toHours"]);
											AJS.$("#syncUpRunToMeridian").val(this.formData["toMeridian"]);
										}
										this.formData.weekdays.forEach(function(option) {
											AJS.$("input[name=syncUpWeekdayCheckbox][value=" + option + "]").attr('checked', 'checked');
										});
										this.syncUpWeekdayUpdate();
										break;
									case 'daysOfMonth':
										AJS.$("input[name=syncUpDaysOfMonthOpt][value=" + this.formData["daysOfMonth"] + "]").attr('checked', 'checked');
										if(this.formData["daysOfMonth"] == "dayOfMonth") {
											AJS.$("#syncUpMonthDay").val(this.formData["monthDay"]);
										}else {
											AJS.$("#syncUpWeek").val(this.formData["week"]);
											AJS.$("#syncUpDay").val(this.formData["day"]);
										}
										break;
									case 'advanced':
										AJS.$('#syncUpCronString').val(this.formData["cronString"]);
										break;
								}
								AJS.$(".syncUpServerTime").html(AJS.I18n.getText('cron.editor.current.server.time') + ' ' + this.formData.serverTime);
							}
						}
						if (jqXHR.status == 401) {
							window.location = contextPath
									+ "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
						}
					}.bind(this)
				});
		}

		this.getScheduleDescription = function() {
			var schedule;
			if(this.formData["mode"] == "daily") {
				schedule = "";
				if(this.formData["interval"] == "0") {
					schedule = schedule + "Daily once at " +  this.formData["onceHours"] +  ":" + this.formData["onceMinutes"] + " " + this.formData["onceMeridian"];
				} else {
					schedule = schedule +  "Daily " + this.interval[this.formData["interval"]] + " from " + this.formData["fromHours"] +  " " + this.formData["fromMeridian"] + " to " + this.formData["toHours"] + " " + this.formData["toMeridian"];
				}
			}
			if(this.formData["mode"] == "daysOfWeek") {
				schedule = "";
				if(this.formData["interval"] == "0") {
					schedule = schedule + "Daily once at " +  this.formData["onceHours"] +  ":" + this.formData["onceMinutes"] + " " + this.formData["onceMeridian"] + " " + "on";
					this.formData.weekdays.forEach(function(option) {
						schedule = schedule + " " + option + " ";
					});
				} else {
					schedule = schedule +  this.interval[this.formData["interval"]] +  " from " + this.formData["fromHours"] +  " " + this.formData["fromMeridian"] + " to " + this.formData["toHours"] + " " + this.formData["toMeridian"] + " " + "on";
					this.formData.weekdays.forEach(function(option) {
							schedule = schedule + " " + option + " ";
					});
				}
			}
			if(this.formData["mode"] == "daysOfMonth") {
				schedule = "";
				if(this.formData["daysOfMonth"] == "dayOfMonth") {
					schedule = schedule +  "day " + this.formData["monthDay"] + " of every month at ";
				}else {
					schedule = schedule + this.weekOrder[this.formData['week']] + " " + this.dayOrder[this.formData['day']] + " " + "of every month at ";
				}

				schedule = schedule + this.formData["onceHours"] +  ":" + this.formData["onceMinutes"] + " " + this.formData["onceMeridian"];
			}
			if(this.formData["mode"] == "advanced") {
				schedule = "";
				schedule = schedule + this.formData["cronString"];
			}

			return schedule;

		}

    function pad(num) {
      return String("0"+num).slice(-2);
		}
    function formatDate(d) {
        var day = d.getDate();
        var month = d.getMonth()+1;
        var year = d.getFullYear();
        return ""+pad(day)+"-"+pad(month)+"-"+year+" "+d.toLocaleTimeString();
    }

		this.switchToDaily = function() {
			this.syncUpData.mode = 'daily';
			AJS.$('#syncUpDaysOfWeek, #syncUpDaysOfMonth, #syncUpAdvanced').css('display', 'none');
			AJS.$('#syncUpFreqDiv').css('display', 'block');
			AJS.$('#syncUpInnerFreqDiv').css('display', 'inline');
			this.toggleFrequencyControl();
		}

		this.switchToDaysOfWeek = function() {
			this.syncUpData.mode = 'daysOfWeek';
			AJS.$('#syncUpDaysOfWeek, #syncUpFreqDiv').css('display', 'block');
			AJS.$('#syncUpDaysOfMonth, #syncUpAdvanced').css('display', 'none');
			AJS.$('#syncUpInnerFreqDiv').css('display', 'inline');
			this.toggleFrequencyControl()
		}

		this.switchToDaysOfMonth = function() {
			this.syncUpData.mode = 'daysOfMonth';
			AJS.$('#syncUpDaysOfWeek, #syncUpAdvanced, #syncUpInnerFreqDiv, #syncUpRunMany').css('display', 'none');
			AJS.$('#syncUpDaysOfMonth, #syncUpFreqDiv').css('display', 'block');
			AJS.$('#syncUpRunOnce').css('display', 'inline');
		}

		this.switchToAdvanced = function() {
			this.syncUpData.mode = 'advanced';
			AJS.$('#syncUpDaysOfWeek, #syncUpDaysOfMonth, #syncUpFreqDiv').css('display', 'none');
			AJS.$('#syncUpAdvanced').css('display', 'block');
			AJS.$('#syncUpInnerFreqDiv').css('display', 'inline');
		}

		this.toggleFrequencyControl  = function() {
			this.syncUpData.intervalValue = AJS.$('#syncUpInterval').val();
			if(this.syncUpData.intervalValue !== '0' && this.syncUpData.mode !== 'daysOfMonth') {
				AJS.$('#syncUpRunOnce').css('display', 'none');
				AJS.$('#syncUpRunMany').css('display', 'inline');
			} else {
				AJS.$('#syncUpRunOnce').css('display', 'inline');
				AJS.$('#syncUpRunMany').css('display', 'none');
			}
		}

		this.syncUpWeekdayUpdate = function() {
			var weekDays = AJS.$('#syncUpDaysOfWeek').find('.syncUpWeekdayCheckbox');
			weekDays.each(function(option) {
				if(weekDays[option].checked && (this.syncUpData.daysOfWeek.indexOf(weekDays[option].value) === -1)) {
					this.syncUpData.daysOfWeek.push(weekDays[option].value);
				} else if(!weekDays[option].checked && (this.syncUpData.daysOfWeek.indexOf(weekDays[option].value) > -1)) {
					this.syncUpData.daysOfWeek.splice(this.syncUpData.daysOfWeek.indexOf(weekDays[option].value), 1);
				}
			}.bind(this));
		}



		this.calculateCronExp = function() {
			cronData = {
				recoveryEnabled : '',
				mode : '',
				daysOfMonth : '',
				monthDay : '',
				week : '',
				day : '',
				interval : '',
				onceHours : '',
				onceMinutes : '',
				onceMeridian : '',
				fromHours : '',
				fromMeridian : '',
				toHours : '',
				toMeridian : '',
				weekdays : [],
				cronString : '',
				rootPath : this.formData.rootPath
			};
			var atHours = parseInt(AJS.$('#syncUpRunOnceHours').val());
		  var atMinutes = parseInt(AJS.$('#syncUpRunOnceMinutes').val());
		  if(atHours === 12) {
		    atHours = 0;
		  }
		  var atMeridian = AJS.$('#syncUpRunOnceMeridian').val();
		  if(atMeridian === 'pm') {
		    atHours = parseInt(atHours) + 12;
		  }
		  var fromHour = parseInt(AJS.$('#syncUpRunFromHours').val());
		  var toHour = parseInt(AJS.$('#syncUpRunToHours').val());
		  if(fromHour === 12) {
		    fromHour = 0;
		  }
		  if(toHour === 12) {
		    toHour = 0;
		  }
		  if(AJS.$('#syncUpRunFromMeridian').val() === 'pm') {
		    fromHour = parseInt(fromHour) + 12;
		  }
		  if(AJS.$('#syncUpRunToMeridian').val() === 'pm') {
		    toHour = parseInt(toHour) + 12;
		  }
		  var cronExp = '';
			cronData.recoveryEnabled = AJS.$('input[name=recoveryEnabled]:checked').val(),
			cronData.mode = this.syncUpData.mode;
			switch (this.syncUpData.mode) {
				case 'daily':
					if(this.syncUpData.intervalValue === '0') {
						cronData.onceHours = AJS.$('#syncUpRunOnceHours').val();
						cronData.onceMinutes = AJS.$('#syncUpRunOnceMinutes').val();
						cronData.onceMeridian = atMeridian;
						cronData.interval = this.syncUpData.intervalValue;
						var cronExp = '0 ' + atMinutes + ' ' + atHours + ' * * ?';
		        // return cronExp;
					} else {
						if(fromHour > toHour) {
							//error
							AJS.$('.syncUpErrorDialog').html('You must select a from time that is before the to time.');
							setTimeout(function() {
								AJS.$('.syncUpErrorDialog').html('');
								AJS.$('.zrunCronJob').removeAttr('disabled');
							}, 2000);
							return cronExp;
						}
						//since return is written above so starting without if
						cronData.fromHours = AJS.$('#syncUpRunFromHours').val();
						cronData.fromMeridian = AJS.$('#syncUpRunFromMeridian').val()
						cronData.toHours = AJS.$('#syncUpRunToHours').val();
						cronData.toMeridian = AJS.$('#syncUpRunToMeridian').val();
						cronData.interval = this.syncUpData.intervalValue;
						if(parseInt(this.syncUpData.intervalValue) >= 60) {
		          var interval = parseInt(this.syncUpData.intervalValue) / 60;
		          cronExp = '0 0 ' + fromHour + '-' + toHour + '/' + interval + ' * * ?';
		          // return cronExp;
		        } else {
		          cronExp = '0 ' + '0/' + this.syncUpData.intervalValue + ' ' +  fromHour + '-' + toHour + ' * * ?';
		          // return cronExp;
		        }
					}
					break;
				case 'daysOfWeek':
					if(this.syncUpData.daysOfWeek.length) {
						cronData.weekdays = [];
						cronData.interval = this.syncUpData.intervalValue;
						this.syncUpData.daysOfWeek.forEach(function(option) {
							cronData.weekdays.push(option);
						}.bind(this));
						if(this.syncUpData.intervalValue === '0') {
							cronData.onceHours = AJS.$('#syncUpRunOnceHours').val();
							cronData.onceMinutes = AJS.$('#syncUpRunOnceMinutes').val();
							cronData.onceMeridian = atMeridian;
							cronData.interval = this.syncUpData.intervalValue;
							cronExp = '0 ' + atMinutes + ' ' + atHours + ' ? * ';
		          this.syncUpData.daysOfWeek.forEach(function(option) {
		            cronExp += option + ',';
		          }.bind(this));
		          cronExp = cronExp.substring(0, cronExp.length - 1);
		          // return cronExp;
						} else {
							if(fromHour > toHour) {
								//error
								AJS.$('.syncUpErrorDialog').html('You must select a from time that is before the to time.');
								setTimeout(function() {
									AJS.$('.syncUpErrorDialog').html('');
									AJS.$('.zrunCronJob').removeAttr('disabled');
								}, 2000);
								return cronExp;
							}
							//since return is written above so starting without if
							cronData.fromHours = AJS.$('#syncUpRunFromHours').val();
							cronData.fromMeridian = AJS.$('#syncUpRunFromMeridian').val()
							cronData.toHours = AJS.$('#syncUpRunToHours').val();
							cronData.toMeridian = AJS.$('#syncUpRunToMeridian').val();
							cronData.interval = this.syncUpData.intervalValue;
							if(parseInt(this.syncUpData.intervalValue) >= 60) {
		            var interval = parseInt(this.syncUpData.intervalValue) / 60;
		            cronExp = '0 0 ' + fromHour + '-' + toHour + '/' + interval + ' ? * ';
		            // return cronExp;
		          } else {
		            cronExp = '0 ' + '0/' + this.syncUpData.intervalValue + ' ' +  fromHour + '-' + toHour + ' ? * ';
		            // return cronExp;
		          }
		          this.syncUpData.daysOfWeek.forEach(function(option) {
		            cronExp += option + ',';
		          }.bind(this));
		          cronExp = cronExp.substring(0, cronExp.length - 1);
		          // return cronExp;
						}
					} else {
						AJS.$('.syncUpErrorDialog').html('You must select one or more days of the week for the Days per Week mode.');
						setTimeout(function() {
							AJS.$('.syncUpErrorDialog').html('');
							AJS.$('.zrunCronJob').removeAttr('disabled');
						}, 2000);
						return cronExp;
					}
					break;
				case 'daysOfMonth':
					cronData.daysOfMonth = AJS.$('input[name=syncUpDaysOfMonthOpt]:checked').val();
					if(AJS.$('input[name=syncUpDaysOfMonthOpt]:checked').val() === 'dayOfMonth') {
						cronData.monthDay = AJS.$('#syncUpMonthDay').val();
					} else {
						cronData.week = AJS.$('#syncUpWeek').val();
						cronData.day = AJS.$('#syncUpDay').val();
					}
					cronData.onceHours = AJS.$('#syncUpRunOnceHours').val();
					cronData.onceMinutes = AJS.$('#syncUpRunOnceMinutes').val();
					cronData.onceMeridian = atMeridian;
					if(AJS.$('input[name=syncUpDaysOfMonthOpt]:checked').val() === 'dayOfMonth') {
		        var dayOfMonth = AJS.$('#syncUpMonthDay').val();
		        cronExp = '0 ' + atMinutes + ' ' + atHours + ' ' + dayOfMonth + ' * ?';
		      } else {
		        var weekOfMonth = AJS.$('#syncUpWeek').val();
		        var dayofWeek = AJS.$('#syncUpDay').val();
		        if(weekOfMonth === 'L') {
		          cronExp = '0 ' + atMinutes + ' ' + atHours + ' ?' + ' * ' + dayofWeek + weekOfMonth;
		        } else {
		          cronExp = '0 ' + atMinutes + ' ' + atHours + ' ?' + ' * ' + dayofWeek + '#' + weekOfMonth;
		        }
		      }
		      // return cronExp;
					break;
				case 'advanced':
					if(AJS.$('#syncUpCronString').val()) {
						cronData.cronString = AJS.$('#syncUpCronString').val();
						cronExp = AJS.$('#syncUpCronString').val();
					} else {
						AJS.$('.syncUpErrorDialog').html('You must enter valid cron expression');
						setTimeout(function() {
							AJS.$('.syncUpErrorDialog').html('');
							AJS.$('.zrunCronJob').removeAttr('disabled');
						}, 2000);
						return cronExp;
					}
					break;
			}
			this.formData = cronData;
			return cronExp;
		}

		this.runCronSyncJob = function() {
			AJS.$('#index-recovery-file-name').val('');
			AJS.$('.syncUpError').html('');
			var cronJobExp = '';
			cronJobExp = this.calculateCronExp();
			if(cronJobExp === '' && AJS.$('input[name=recoveryEnabled]:checked').val() === 'true') {
				return;
			}
			that = this;
			var queryParam = '';
			queryParam += 'flag=' + AJS.$('input[name=recoveryEnabled]:checked').val();
			queryParam += '&expression=' + cronJobExp;

			var progressBarContainer = '';
			jQuery.ajax({
					url : contextPath + "/rest/zephyr/latest/datacenter/backupIndex?" + queryParam,
					type : "PUT",
					accept: "PUT",
					complete : function(jqXHR, textStatus) {
						if (jqXHR.status == 200) {
							if (jQuery.parseJSON(jqXHR.responseText).response =="success") {
							this.displayScheduleStatus();
							AJS.$('#general-config-aui-message-bar').show().empty();
							if(this.formData["recoveryEnabled"] == "true") {
								AJS.$('#general-config-aui-message-bar').append('<span><strong style="color:green">' + AJS.I18n.getText('Job Scheduled Successfully') + '</strong></span>');
							} else {
								AJS.$('#general-config-aui-message-bar').append('<span><strong style="color:#c00">' + AJS.I18n.getText('Job Scheduled Switched Off') + '</strong></span>');
							}

							dialog.hide();

							AJS.$('.syncUpErrorDialog').html('Backup Job Scheduling successful');
							setTimeout(function(){
								AJS.$('#general-config-aui-message-bar').hide();
								AJS.$('.syncUpErrorDialog').html('');
								AJS.$('.zrunCronJob').removeAttr('disabled');
								}, 3000);

							} else {
								AJS.$('.syncUpErrorDialog').html('Invalid Cron Expression');
								setTimeout(function() {
									AJS.$('.syncUpErrorDialog').html('');
									AJS.$('.zrunCronJob').removeAttr('disabled');
								}, 2000);
							}
						}else{
							AJS.$("#newnodesyncup-job-tab .overlay-icon").addClass('hidden');
							AJS.$('#general-config-aui-message-bar').show().empty();
							AJS.$("#general-config-aui-message-bar").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr.common.internal.server.error') + '</strong></span>');
							setTimeout(function(){
								AJS.$('#general-config-aui-message-bar').hide();
								}, 3000);
						}
						if (jqXHR.status == 401) {
							window.location = contextPath
									+ "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
						}
						AJS.$("#newnodesyncup-job-tab .overlay-icon").addClass('hidden');
					}.bind(this)
				});
		}

		this.generateHtmlContentForIntegrityChecker = function(tableId, data, columnName, tabName, tabActive, totalCount) {
			if(data && data.length > 0) {
				Zephyr.Datacenter.Info.addOrRemoveExportButton(true);
				var paginationContent = ZEPHYR.Datacenter.integritycheckPaginationNew({
					currentIndex : 1,
					totalCount : totalCount,
					entriesCount : 10,
					totalPages : (parseInt(totalCount % 10) == 0) ? parseInt(totalCount / 10) : parseInt(totalCount / 10) + 1
				});
				var liHtml = '<li class="menu-item' + (tabActive == tableId ? ' active-tab' : '') + '">'
				var startHtmlContent = '<div class="tabs-pane' + (tabActive == tableId ? ' active-pane' : '') + '" id="' + tableId + 'tab"><table class="aui aui-table-rowhover" id="';
				var theadHtmlContent = '<thead><tr><th>' + columnName + '</th><th>Count</th>';
				var tbodyHtmlContent = '</tr></thead><tbody>';
				var endHtmlContent = '</tbody><table/>' + paginationContent + '</div>';
				startHtmlContent += tableId + 'Res">'
				for(var i = 0; i < data.length; i++) {
					tbodyHtmlContent += '<tr><td>' + data[i].id + '</td>';
					tbodyHtmlContent += '<td>' + data[i].count + '</td></tr>';
				}
				// tbodyHtmlContent += paginationContent;
				liHtml += '<a href="#' + tableId + 'tab">' + tabName + '</li>';
				AJS.$('#integrity-checker-tab .aui-tabs .tabs-menu').append(liHtml);
				AJS.$('#integrity-checker-tab .aui-tabs .tabs-menu').after(startHtmlContent + theadHtmlContent + tbodyHtmlContent + endHtmlContent);
			}else{
				if(tableId=="zicExecutionCountByCycle" && data && data.length <= 0){
					AJS.$('#general-config-aui-message-bar3').show().empty();
					AJS.$("#general-config-aui-message-bar3").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.zcyclecountzero') + '</strong></span>');
					setTimeout(function(){
						AJS.$('#general-config-aui-message-bar3').hide();
						}, 3000);
					AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
			    }
				if(tableId=="zicExecutionCountByFolder" && data && data.length <= 0){
					AJS.$('#general-config-aui-message-bar4').show().empty();
					AJS.$("#general-config-aui-message-bar4").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.zfoldercountzero') + '</strong></span>');
					setTimeout(function(){
						AJS.$('#general-config-aui-message-bar4').hide();
						}, 3000);
					AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
			    }
				if(tableId=="zicIssueCountByProject" && data && data.length <= 0){
					AJS.$('#general-config-aui-message-bar5').show().empty();
					AJS.$("#general-config-aui-message-bar5").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.zissuecountbyprojectzero') + '</strong></span>');
					setTimeout(function(){
						AJS.$('#general-config-aui-message-bar5').hide();
						}, 3000);
					AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
			    }
				if(tableId=="zicTeststepResultCountByExecution" && data && data.length <= 0){
					AJS.$('#general-config-aui-message-bar6').show().empty();
					AJS.$("#general-config-aui-message-bar6").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.zTeststepResultCountByExecution') + '</strong></span>');
					setTimeout(function(){
						AJS.$('#general-config-aui-message-bar6').hide();
						}, 3000);
					AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
			    }
				if(tableId=="zicTeststepCountByIssue" && data && data.length <= 0){
					AJS.$('#general-config-aui-message-bar7').show().empty();
					AJS.$("#general-config-aui-message-bar7").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.zTeststepCountByIssue') + '</strong></span>');
					setTimeout(function(){
						AJS.$('#general-config-aui-message-bar7').hide();
						}, 3000);
					AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
			    }

			}
		}

		this.integrityCheckerPaginationTrigger = function(target) {
			var tabId = 	AJS.$(target.currentTarget).closest('.tabs-pane').attr('id');
			if(target.currentTarget.id === 'prev-page-pagination') {
				this.integrityCheckerPaginationObject[tabId] = parseInt(this.integrityCheckerPaginationObject[tabId]) - 10;
			} else if(target.currentTarget.id === 'next-page-pagination'){
				this.integrityCheckerPaginationObject[tabId] = parseInt(this.integrityCheckerPaginationObject[tabId]) + 10;
			} else {
				this.integrityCheckerPaginationObject[tabId] = AJS.$(target.currentTarget).attr('data-offset') === '0' ? 0 : (AJS.$(target.currentTarget).attr('data-offset') - 1) * 10;
			}
			AJS.$("#prev-page-pagination, #next-page-pagination, .goToPage").css('pointer-events', 'none');
			var zicTotalExecutionCount = false;
			var zicTotalCycleCount = false;	//no pagination on first 2 tabs
			var zicExecutionCountByCycle = tabId.indexOf('zicExecutionCountByCycle') !== -1;
			var zicExecutionCountByFolder = tabId.indexOf('zicExecutionCountByFolder') !== -1;
			var zicIssueCountByProject = tabId.indexOf('zicIssueCountByProject') !== -1;
			var zicTeststepResultCountByExecution = tabId.indexOf('zicTeststepResultCountByExecution') !== -1;
			var zicTeststepCountByIssue = tabId.indexOf('zicTeststepCountByIssue') !== -1;
			var queryParam = "zicTotalExecutionCount=" + zicTotalExecutionCount +"&";
			queryParam +=  "zicTotalCycleCount=" + zicTotalCycleCount +"&";
			queryParam +=  "zicExecutionCountByCycle=" + zicExecutionCountByCycle +"&";
			queryParam +=  "zicExecutionCountByFolder=" + zicExecutionCountByFolder +"&";
			queryParam +=  "zicIssueCountByProject=" + zicIssueCountByProject +"&";
			queryParam +=  "zicTeststepResultCountByExecution=" + zicTeststepResultCountByExecution +"&";
			queryParam +=  "zicTeststepCountByIssue=" + zicTeststepCountByIssue + "&";
			queryParam += 'offset=' + this.integrityCheckerPaginationObject[tabId] + '&limit=10';
			jQuery.ajax({
				url: contextPath + "/rest/zephyr/latest/datacenter/integritycheck?" + queryParam,
				type: "GET",
				success:function(data, textStatus, jqXHR) {
					var tbodyHtmlContent = '';
					var paginationContent = ZEPHYR.Datacenter.integritycheckPaginationNew({
						currentIndex : parseInt(this.integrityCheckerPaginationObject[tabId] / 10) + 1,
						totalCount : data[data.tabActive]['totalCount'],
						entriesCount : 10,
						totalPages : (parseInt(data[data.tabActive]['totalCount'] % 10) == 0) ? parseInt(data[data.tabActive]['totalCount'] / 10) : parseInt(data[data.tabActive]['totalCount'] / 10) + 1,						// totalPages : parseInt(data[data.tabActive]['totalCount'] / 10) + 1
					});
					for(var i = 0; i < data[data.tabActive]['data'].length; i++) {
						tbodyHtmlContent += '<tr><td>' + data[data.tabActive]['data'][i].id + '</td>';
						tbodyHtmlContent += '<td>' + data[data.tabActive]['data'][i].count + '</td></tr>';
					}
					// tbodyHtmlContent += paginationContent;
					AJS.$('#' + tabId).find('tbody').html(tbodyHtmlContent);
					AJS.$('#' + tabId).find('.integrityCheckerPaginationWrapper').replaceWith(paginationContent);
					AJS.$("#prev-page-pagination, #next-page-pagination, .goToPage").css('pointer-events', 'auto');
				}.bind(this),
				error : function(jqXHR, textStatus){
					if(jqXHR.status == 401) {
						var dialog = new AJS.Dialog({
							width:800,
							height:270,
							id:	"dialog-error"
						});
						dialog.addHeader(AJS.I18n.getText('zephyr.common.forbidden.error.label'));

						dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
						AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
							title: AJS.I18n.getText('zephyr.je.submit.form.error.title'),
							body: AJS.I18n.getText('zephyr.common.login.error'),
							closeable: false
						});

						dialog.addLink("Close", function (dialog) {
							dialog.hide();
						}, "#");
						dialog.show();
					}
					AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
				}
			});
		}

		this.integrityChecker = function() {
			AJS.$("#integrity-checker-tab .overlay-icon").removeClass('hidden');
			Zephyr.Datacenter.Info.addOrRemoveExportButton(false);
			AJS.$("#integrity-checker-tab .aui-tabs .tabs-pane").remove();
			AJS.$("#integrity-checker-tab .aui-tabs .tabs-menu").empty();
			var zicTotalExecutionCount = AJS.$("#zicTotalExecutionCount").is(":checked");
			var zicTotalCycleCount = AJS.$("#zicTotalCycleCount").is(":checked");
			var zicExecutionCountByCycle = AJS.$("#zicExecutionCountByCycle").is(":checked");
			var zicExecutionCountByFolder = AJS.$("#zicExecutionCountByFolder").is(":checked");
			var zicIssueCountByProject = AJS.$("#zicIssueCountByProject").is(":checked");
			var zicTeststepResultCountByExecution = AJS.$("#zicTeststepResultCountByExecution").is(":checked");
			var zicTeststepCountByIssue = AJS.$("#zicTeststepCountByIssue").is(":checked");
			var queryParam = "zicTotalExecutionCount=" + zicTotalExecutionCount +"&";
			queryParam +=  "zicTotalCycleCount=" + zicTotalCycleCount +"&";
			queryParam +=  "zicExecutionCountByCycle=" + zicExecutionCountByCycle +"&";
			queryParam +=  "zicExecutionCountByFolder=" + zicExecutionCountByFolder +"&";
			queryParam +=  "zicIssueCountByProject=" + zicIssueCountByProject +"&";
			queryParam +=  "zicTeststepResultCountByExecution=" + zicTeststepResultCountByExecution +"&";
			queryParam +=  "zicTeststepCountByIssue=" + zicTeststepCountByIssue + "&";
			queryParam += "offset=0&limit=10";
			if (zicTotalExecutionCount || zicTotalCycleCount || zicExecutionCountByCycle || zicExecutionCountByFolder || zicIssueCountByProject
					|| zicTeststepResultCountByExecution || zicTeststepCountByIssue) {
				jQuery.ajax({
					url: contextPath + "/rest/zephyr/latest/datacenter/integritycheck?" + queryParam,
					type: "GET",
					success:function(data, textStatus, jqXHR) {

						if(data.zicTotalExecutionCount<=0 && data.zicTotalCycleCount<=0 && data.zicExecutionCountByCycle && data.zicExecutionCountByCycle.length<=0 && data.zicExecutionCountByFolder && data.zicExecutionCountByFolder.length<=0 &&
								data.zicIssueCountByProject && data.zicIssueCountByProject.length<=0 && data.zicTeststepResultCountByExecution && data.zicTeststepResultCountByExecution.length<=0 && data.zicTeststepCountByIssue && data.zicTeststepCountByIssue.length<=0 ){
							AJS.$('#general-config-aui-message-bar').show().empty();
							AJS.$("#general-config-aui-message-bar").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.Allcountzero') + '</strong></span>');
							setTimeout(function(){
								AJS.$('#general-config-aui-message-bar').hide();
								}, 3000);
							AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
							Zephyr.Datacenter.Info.addOrRemoveExportButton(false);
					    }else{
					    	//Rendering compare indexed schedules count against database.
							if(data.zicTotalExecutionCount) {
								Zephyr.Datacenter.Info.addOrRemoveExportButton(true);
								var liHtml = '<li class="menu-item' + (data.tabActive == 'zicTotalExecutionCount' ? ' active-tab' : '') + '">'
								var tbodyHtmlContent = '</tr></thead><tbody>';
								var endHtmlContent = '</tbody><table/></div>';
								var startHtmlContent = '<div class="tabs-pane' + (data.tabActive == 'zicTotalExecutionCount' ? ' active-pane' : '') + '" id="zicTotalExecutionCounttab"><table class="aui aui-table-rowhover" id="zicTotalExecutionCountRes">';
								var theadHtmlContent = '<thead><tr><th>' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.thead.label1') + '</th><th>' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.thead.label2')
														+ '</th><th>' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.status.label') + '</th>';
								tbodyHtmlContent += '<tr><td>' + data.zicTotalExecutionCount + '</td>';
								tbodyHtmlContent += '<td>' + data.zicTotalExecutionCountdb + '</td>';
								tbodyHtmlContent += '<td' + (data.zicTotalExecutionCount == data.zicTotalExecutionCountdb ? ' style="color:green">Green' : ' style="color:red">Red') + '</td></tr>';
								liHtml += '<a href="#zicTotalExecutionCounttab">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.result.label1') + '</li>';
								AJS.$('#integrity-checker-tab .aui-tabs .tabs-menu').append(liHtml);
								AJS.$('#integrity-checker-tab .aui-tabs .tabs-menu').after(startHtmlContent + theadHtmlContent + tbodyHtmlContent + endHtmlContent);
						    }else{
						    	if(data.zicTotalExecutionCount<=0){
						    		AJS.$('#general-config-aui-message-bar1').show().empty();
									AJS.$("#general-config-aui-message-bar1").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.compareexecutioncountzero') + '</strong></span>');
									setTimeout(function(){
										AJS.$('#general-config-aui-message-bar1').hide();
										}, 3000);
						    	}
						    	AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
						    }
							//Rendering display cycles count.
						    if(data.zicTotalCycleCount) {
						    	Zephyr.Datacenter.Info.addOrRemoveExportButton(true);
						    	var liHtml = '<li class="menu-item' + (data.tabActive == 'zicTotalCycleCount' ? ' active-tab' : '') + '">'
								var tbodyHtmlContent = '</tr></thead><tbody>';
								var endHtmlContent = '</tbody><table/></div>';
								var startHtmlContent = '<div class="tabs-pane' + (data.tabActive == 'zicTotalCycleCount' ? ' active-pane' : '') + '" id="zicTotalCycleCounttab"><table class="aui aui-table-rowhover" id="zicTotalCycleCountRes">';
								var theadHtmlContent = '<thead><tr><th>' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.cycles.count.label') + '</th>';
								tbodyHtmlContent += '<tr><td>' + data.zicTotalCycleCount + '</td></tr>';
								liHtml += '<a href="#zicTotalCycleCounttab">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.result.label2') + '</li>';
								AJS.$('#integrity-checker-tab .aui-tabs .tabs-menu').append(liHtml);
								AJS.$('#integrity-checker-tab .aui-tabs .tabs-menu').after(startHtmlContent + theadHtmlContent + tbodyHtmlContent + endHtmlContent);
						    }else{

						    	if(data.zicTotalCycleCount<=0){
						    		AJS.$('#general-config-aui-message-bar2').show().empty();
									AJS.$("#general-config-aui-message-bar2").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.zTotalCycleZero') + '</strong></span>');
									setTimeout(function(){
										AJS.$('#general-config-aui-message-bar2').hide();
										}, 3000);
						    	}
						    	AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
						    }
							//Rendering display execution count by cycle.
						    Zephyr.Datacenter.Info.generateHtmlContentForIntegrityChecker("zicExecutionCountByCycle", data.zicExecutionCountByCycle ? data.zicExecutionCountByCycle.data : undefined,
						    		AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.cycle.id.label'),  AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.result.label3'), data.tabActive, data.zicExecutionCountByCycle ? data.zicExecutionCountByCycle.totalCount : 0);
							//Rendering display execution count by folder.
						    Zephyr.Datacenter.Info.generateHtmlContentForIntegrityChecker("zicExecutionCountByFolder", data.zicExecutionCountByFolder ? data.zicExecutionCountByFolder.data : undefined,
						    		AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.folder.id.label'),  AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.result.label4'), data.tabActive, data.zicExecutionCountByFolder ? data.zicExecutionCountByFolder.totalCount : 0);
							//Rendering display test issue count by project.
						    Zephyr.Datacenter.Info.generateHtmlContentForIntegrityChecker("zicIssueCountByProject", data.zicIssueCountByProject ? data.zicIssueCountByProject.data : undefined,
						    		AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.project.id.label'),  AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.result.label5'), data.tabActive, data.zicIssueCountByProject ? data.zicIssueCountByProject.totalCount : 0);
							//Rendering display test step result count by execution.
						    Zephyr.Datacenter.Info.generateHtmlContentForIntegrityChecker("zicTeststepResultCountByExecution", data.zicTeststepResultCountByExecution ? data.zicTeststepResultCountByExecution.data : undefined,
						    		AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.execution.id.label'),  AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.result.label6'), data.tabActive, data.zicTeststepResultCountByExecution ? data.zicTeststepResultCountByExecution.totalCount : 0);
							//Rendering display test step result count by issue.
						    Zephyr.Datacenter.Info.generateHtmlContentForIntegrityChecker("zicTeststepCountByIssue", data.zicTeststepCountByIssue ? data.zicTeststepCountByIssue.data : undefined,
						    		AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.issue.id.label'),  AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.result.label7'), data.tabActive, data.zicTeststepCountByIssue ? data.zicTeststepCountByIssue.totalCount : 0);
							AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
					    }

					},
					error : function(jqXHR, textStatus){
						if(jqXHR.status == 401) {
							var dialog = new AJS.Dialog({
								width:800,
								height:270,
								id:	"dialog-error"
							});
							dialog.addHeader(AJS.I18n.getText('zephyr.common.forbidden.error.label'));

							dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
							AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
								title: AJS.I18n.getText('zephyr.je.submit.form.error.title'),
								body: AJS.I18n.getText('zephyr.common.login.error'),
								closeable: false
							});

							dialog.addLink("Close", function (dialog) {
								dialog.hide();
							}, "#");
							dialog.show();
						}
						AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
					}
				});
			} else {
				Zephyr.Datacenter.Info.addOrRemoveExportButton(false);
				AJS.$("#integrity-checker-tab .overlay-icon").addClass('hidden');
				AJS.$('#general-config-aui-message-bar').show().empty();
				AJS.$("#general-config-aui-message-bar").append('<span><strong style="color:red">' + AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.integrity.checker.error.label') + '</strong></span>');
				setTimeout(function(){
					AJS.$('#general-config-aui-message-bar').hide();
					}, 3000);
			}
		}

		this.addOrRemoveExportButton = function(flag) {
			if(flag) {
				AJS.$("#integrity-checker-tab .zexportIntegrityChecker").removeClass('hidden');
			} else {
				AJS.$("#integrity-checker-tab .zexportIntegrityChecker").addClass('hidden');
			}
		}

		this.exportIntegrityChecker = function(e) {
			e.preventDefault();
			var zicTotalExecutionCount = AJS.$("#zicTotalExecutionCount").is(":checked");
			var zicTotalCycleCount = AJS.$("#zicTotalCycleCount").is(":checked");
			var zicExecutionCountByCycle = AJS.$("#zicExecutionCountByCycle").is(":checked");
			var zicExecutionCountByFolder = AJS.$("#zicExecutionCountByFolder").is(":checked");
			var zicIssueCountByProject = AJS.$("#zicIssueCountByProject").is(":checked");
			var zicTeststepResultCountByExecution = AJS.$("#zicTeststepResultCountByExecution").is(":checked");
			var zicTeststepCountByIssue = AJS.$("#zicTeststepCountByIssue").is(":checked");
			var queryParam = "zicTotalExecutionCount=" + zicTotalExecutionCount +"&";
			queryParam +=  "zicTotalCycleCount=" + zicTotalCycleCount +"&";
			queryParam +=  "zicExecutionCountByCycle=" + zicExecutionCountByCycle +"&";
			queryParam +=  "zicExecutionCountByFolder=" + zicExecutionCountByFolder +"&";
			queryParam +=  "zicIssueCountByProject=" + zicIssueCountByProject +"&";
			queryParam +=  "zicTeststepResultCountByExecution=" + zicTeststepResultCountByExecution +"&";
			queryParam +=  "zicTeststepCountByIssue=" + zicTeststepCountByIssue + "&";
			queryParam += "offset=0&limit=";
			jQuery.ajax({
				url: contextPath + "/rest/zephyr/latest/datacenter/exportIntegrityCheck?" + queryParam,
				type: "GET",
				success:function(data, textStatus, jqXHR) {
					if (jqXHR.status == 200) {
						var link = document.createElement("a");
						link.href = JSON.parse(jqXHR.responseText).url;
						document.body.appendChild(link);
						link.click();
						document.body.removeChild(link);
						delete link;
					}
					if (jqXHR.status == 401) {
						window.location = contextPath
								+ "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
					}
				},
				error : function(jqXHR, textStatus){
					if(jqXHR.status == 401) {
						var dialog = new AJS.Dialog({
							width:800,
							height:270,
							id:	"dialog-error"
						});
						dialog.addHeader(AJS.I18n.getText('zephyr.common.forbidden.error.label'));

						dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
						AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
							title: AJS.I18n.getText('zephyr.je.submit.form.error.title'),
							body: AJS.I18n.getText('zephyr.common.login.error'),
							closeable: false
						});

						dialog.addLink("Close", function (dialog) {
							dialog.hide();
						}, "#");
						dialog.show();
					}
				}
			});
		}


		this.redirecttoJIRASupportTool = function() {
			that = this;
			location.href = contextPath+ "/plugins/servlet/stp/view/";

		}




	}
return {Info:infoClass}
})()

window.onbeforeunload = function (e) {
	return;
}

AJS.$(document).ready(function() {

	AJS.$(document).on('click', ".zdownloadzfjlogs", Zephyr.Datacenter.Info.getsupportLogs);

	AJS.$(document).on('click', ".zdownloadzfjlogsUsingJIRA", Zephyr.Datacenter.Info.redirecttoJIRASupportTool);

	AJS.$(document).on('click', ".zcheckIntegrityChecker", Zephyr.Datacenter.Info.integrityChecker);
	AJS.$('#zexportIntegrityChecker').bind('click', Zephyr.Datacenter.Info.exportIntegrityChecker);
	AJS.$(document).on('click', "#prev-page-pagination, #next-page-pagination, .goToPage", Zephyr.Datacenter.Info.integrityCheckerPaginationTrigger.bind(Zephyr.Datacenter.Info));

	Zephyr.Datacenter.Info.initSyncupComponent();

	AJS.$('#zintegrityCheckerSelectAll').live('click',function(event) {
	    if(this.checked) {
	        AJS.$('#zintegrityCheckerTable tbody input[type="checkbox"]').each(function() {
	            this.checked = true;
	        });
	    } else {
	        AJS.$('#zintegrityCheckerTable tbody input[type="checkbox"]').each(function() {
	            this.checked = false;
	        });
	    }
	});

	AJS.$('#zSupporToolSelectAll').live('click',function(event) {
	    if(this.checked) {
	        AJS.$('#zSupportToolTable tbody input[type="checkbox"]').each(function() {
	            this.checked = true;
	        });
	    } else {
	        AJS.$('#zSupportToolTable tbody input[type="checkbox"]').each(function() {
	            this.checked = false;
	        });
	    }
	});
});
