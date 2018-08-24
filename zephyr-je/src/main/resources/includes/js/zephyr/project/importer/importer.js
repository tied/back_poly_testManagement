/**
 * Zephyr Test Importer
 */

AJS.$.namespace("Zephyr.Importer.Config")
var selectedProjectIds = [];
var selectedProjectId;
var selectedFileType;
var issueFields = [];
var testStepFields = ['stepOrder', 'stepAction', 'stepExpectedResults', 'stepData', 'issueKey'];
var fieldsFilter = ['issuetype', 'project', 'reporter'];
var issueFieldsFilter = ['issuetype', 'project', 'reporter', 'issuelinks', 'attachment'];
var issueFieldsNamesFilter = ['Zephyr Teststep'];
var links = [];
var savedPreferences = {};
var selectedLinkType;
var fileTypeSelected;
var intervalId;

Zephyr.Importer = (function () {
	var configClass = new function () {
		this.init = function () {
			AJS.$("body").addClass("scrollbody");
			AJS.$("#savedPrefRow").hide();
			Zephyr.Importer.Config.initProjectConfig();
			Zephyr.Importer.Config.initLinkTypes();
			//Zephyr.Importer.Config.initLinkIssueTypes();
			Zephyr.Importer.Config.initFileTypes();
			Zephyr.Importer.Config.initFileFieldsMappingTypes();
			Zephyr.Importer.Config.populateFileTypePreferences();
			Zephyr.Importer.Config.calc();
		}
		this.initProjectConfig = function () {
			Zephyr.Importer.Config.populateProjects();
			var projectDD = new AJS.SingleSelect({
				element: AJS.$("#zephyr-importer-project"),
				itemAttrDisplayed: "label",
				maxInlineResultsDisplayed: 15,
				maxWidth: 200,
				showDropdownButton: true,
				submitInputVal: true,
				title: "Select a product",
				overlabel: AJS.I18n.getText("zephyr-je.testboard.select.project.label"),
				errorMessage: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.disable.testmenu.project.nomatching"),
				ajaxOptions: {
					url: contextPath + "/rest/api/latest/project/",
					query: true,
					minQueryLength: 2,
					formatResponse: function (response) {
						var ret = [];
						var allValues = [];
						var query = AJS.$("#zephyr-importer-project").val();
						AJS.$(response).each(function () {
							var itemDescriptor = new AJS.ItemDescriptor({
								value: this.id.toString(), // value of
								label: this.name, // title
								title: this.projectTypeKey,
								key: this.key,
								highlighted: false
							});
							ret.push(itemDescriptor);
							allValues.push(itemDescriptor);
						});
						return ret;
					}
				}
			});

			AJS.$('#zephyr-importer-project-field').attr('placeholder', AJS.I18n.getText('zephyr-je.pdb.importer.project.placeholder'));

			// On select of a project, populate issue types
			AJS.$(projectDD.model.$element).change(function (event, descriptor) {
				selectedProjectId = descriptor.properties.value;
				Zephyr.Importer.Config.populateIssueTypes();
				/*Zephyr.Importer.Config.populateIssueTypeFieldsContainter(descriptor);
				Zephyr.Importer.Config.populateTestIssuesByProject();
				Zephyr.Importer.Config.initDNDContainers();
				Zephyr.Importer.Config.enableSubmitButtons();
				*/
			});

			validateIssueType();

			AJS.$('#zephyr-importer-project-field').on('blur', function () {
				if (AJS.$('#zephyr-importer-project option').length > 0) {
					document.getElementById('errorForDropDown').innerHTML = "";
					//		validateIssueType();

					var isIssueType = false;
					AJS.$('#issueTypes-dd option').each(function () {
						if (AJS.$(this).attr("selected") == "selected") {
							isIssueType = true;
						}
					});

					var file = AJS.$('#zephyr-importer-filepref')[0].files[0];
					Zephyr.Importer.Config.toShowButtons(isIssueType && (file != undefined));

				}
				else {
					document.getElementById('errorForDropDown').innerHTML = AJS.I18n.getText('zephyr-je.pdb.importer.mandatory.field.msg');
					Zephyr.Importer.Config.toShowButtons(false);
				}
			});



			function validateIssueType() {
				AJS.$("#zephyr-importer-issuetypes").on('blur', '#issueTypes-dd-field', function () {
					var checkingForSelect = false;
					AJS.$('#issueTypes-dd option').each(function () {
						if (AJS.$(this).attr("selected") == "selected") {
							checkingForSelect = true;
						}
					});

					if (checkingForSelect) {
						document.getElementById('errorForDropDown2').innerHTML = "";
						var file = AJS.$('#zephyr-importer-filepref')[0].files[0];

						Zephyr.Importer.Config.toShowButtons((AJS.$('#zephyr-importer-project option').length > 0) && (file != undefined));
					}
					else {
						document.getElementById('errorForDropDown2').innerHTML = AJS.I18n.getText('zephyr-je.pdb.importer.mandatory.field.msg');
						Zephyr.Importer.Config.toShowButtons(false);
					}
				});
			}





		}

		// document.getElementById("fieldsDiv").on("drag", function (e, ui) {
		// 	console.log("yess");
		// 	var h = AJS.$(window).height(); var mousePosition = e.pageY - AJS.$(window).scrollTop();
		// 	var topRegion = 220;
		// 	var bottomRegion = h - 220;
		// 	if (e.which == 1 && (mousePosition < topRegion || mousePosition > bottomRegion)) {    // e.wich = 1 => click down !
		// 		var distance = e.clientY - h / 2;
		// 		distance = distance * 0.1; // <- velocity
		// 		AJS.$(document).scrollTop(distance + AJS.$(document).scrollTop());
		// 	}
		// });



		this.initDNDContainers = function () {
			var clicked = false, clickY;

			AJS.$(".dragable-object").draggable({
				scroll: true,
				revert: "invalid"
			});


			// AJS.$(".importer-field-item").draggable({
			// 	scroll: true,
			// 	revert: "invalid"
			// });

			AJS.$("#dndTbl div").droppable({
				accept: "div.importer-field-item, div.dragable-object",
				greedy: true,
				tolerance: "pointer",
				scroll: true,
				classes: {
					"ui-droppable-hover": "highlight"
				},
				drop: function (e, ui) {
					var idd = ui.draggable.attr("id");
					var targettxt = AJS.$(e.target).text();

					if (!idd) {
						AJS.$(this).html(ui.draggable.html()).attr("title", ui.draggable.html());
					}
					else {
						var draggedElement = document.getElementById(idd);
						var dropElement = document.getElementById(e.target.id);
						AJS.$(this).html(ui.draggable.html());
						// dropElement.style.border = "1px solid gray";
						document.getElementById(idd).innerHTML = targettxt;
						document.getElementById(idd).style.left = '0px';
						document.getElementById(idd).style.border = "1px solid gray";
						document.getElementById(idd).style.top = '0px';
						document.getElementById(idd).style.position = 'relative';
					}

      			/*ui.draggable.remove();
    			if(targettxt != '') {
    				var fieldtbl = AJS.$('#fieldsDiv');
    				fieldtbl.append('<div class="importer-field-item">'+targettxt+'</div>');
    				Zephyr.Importer.Config.refreshFileFields();
    			}*/
				}
			});

			// AJS.$("#fieldsDiv").sortable({
			// 	scroll: true,
			// 	start: function() {
			// 		console.log('st');
			// 	},
			// 	stop: function() {
			// 		console.log('op');
			// 	}
			// });
			AJS.$('#mappingContainersRow').show();
		}



		this.calc = function () {
			if (document.getElementById('zephyr-importer-importallsheets').checked) {
				document.getElementById('fieldSheetId').value = ".*";
				document.getElementById('fieldSheetId').style.display = 'table-row';
			} else {
				document.getElementById('fieldSheetId').value = "";
				document.getElementById('fieldSheetId').style.display = 'none';
			}
		}

		this.showMsgForHelp = function () {
			document.getElementById('helpMsg').style.display = "table-row";
		}

		this.checkTextFieldRow = function (field) {
			document.getElementById("errorForStatingRow").innerText = (field.value === "") ? AJS.I18n.getText('zephyr-je.pdb.importer.mandatory.field.msg') : "";
		}

		this.refreshDNDTable = function () {
			AJS.$(".dragable-object").draggable({
				scroll: true,
				revert: "invalid"
			});



			AJS.$("#dndTbl div").droppable({
				accept: "div.importer-field-item, div.dragable-object",
				greedy: true,
				scroll: true,
				tolerance: "pointer",
				classes: {
					"ui-droppable-hover": "highlight"
				},
				drop: function (e, ui) {

					var idd = ui.draggable.attr("id");
					if (!idd) {
						var targettxt = AJS.$(e.target).text();
						AJS.$(this).html(ui.draggable.html());
					}
					else {
						var draggedElement = document.getElementById(idd);
						var dropElement = document.getElementById(e.target.id);
						var targettxt = AJS.$(e.target).text();
						AJS.$(this).html(ui.draggable.html());
						// "1px solid gray";
						document.getElementById(idd).innerHTML = targettxt;
						document.getElementById(idd).style.border = "1px solid gray";
						document.getElementById(idd).style = "position: relative; left: 0px; top: 0px;";
					}

      			/*ui.draggable.remove();
    			if(targettxt != '') {
    				var fieldtbl = AJS.$('#fieldsDiv');
    				fieldtbl.append('<div class="importer-field-item">'+targettxt+'</div>');
    				Zephyr.Importer.Config.refreshFileFields();
    			}*/
				}
			});
		}

		// AJS.$(".importer-field-item").on("drag", function (e, ui) {
		// 	var h = AJS.$(window).height(); var mousePosition = e.pageY - AJS.$(window).scrollTop();
		// 	var topRegion = 220;
		// 	var bottomRegion = h - 220;
		// 	if (e.which == 1 && (mousePosition < topRegion || mousePosition > bottomRegion)) {    // e.wich = 1 => click down !
		// 		var distance = e.clientY - h / 2;
		// 		distance = distance * 0.1; // <- velocity
		// 		AJS.$(document).scrollTop(distance + AJS.$(document).scrollTop());
		// 	}
		// });


		this.refreshFileFields = function () {
			AJS.$("#fieldsDiv").sortable({
				start: function() {
					AJS.$('#savedMappingContainer')[0].scrollIntoView(true);
				},
				stop: function (ev, ui) {
				},
				scroll: true,
				start: function() {
					AJS.$('.draggable-fields-container').removeClass('addheight');
				},
				stop: function() {
					AJS.$('.draggable-fields-container').addClass('addheight');
				}
			});

			AJS.$("#fieldsDiv").droppable({
				accept: "div.dragable-object", greedy: true,
				tolerance: "pointer",
				scroll: true,
				classes: {
					"ui-droppable-hover": "highlight"
				},
				drop: function (e, ui) {
					var dropped = ui.draggable;
					var droppedOn = AJS.$(this);
					// AJS.$(this).append(dropped.clone().removeAttr('style').addClass("importer-field-item"));
					dropped.css({ 'position': '', 'left': '', 'top': '' }).html('');
				}
			});
		}


		this.enableSubmitButtons = function () {
			if (AJS.$("#zephyr-importer-project").val() != null) {
				AJS.$("#zephyr-importer-importjob").attr("disabled", false);
			} else {
				AJS.$("#zephyr-importer-importjob").attr("disabled", true);
			}
		}

		this.initLinkTypes = function () {
			var opHtml = "<option value=select>" + AJS.I18n.getText('zephyr-je.pdb.importer.linkType.placeholder') + "</option>";;
			var linkTypes = Zephyr.Importer.Config.getLinkTypes();
			var linkTypeId = linkTypes[0].properties.value;
			for (var i = 0; i < linkTypes.length; i++) {
				if (linkTypeId == linkTypes[i].value) {
					opHtml += "<option value=" + linkTypes[i].properties.value + ">" + linkTypes[i].properties.label + "</option>";
				} else {
					opHtml += "<option value=" + linkTypes[i].properties.value + ">" + linkTypes[i].properties.label + "</option>";
				}
			}
			AJS.$("#zephyr-importer-linkType").html(opHtml);
		}

		this.initFileTypes = function () {
			var opHtml = "";
			opHtml += "<option value=Excel selected>" + AJS.I18n.getText('enav.export.excel.schedule.label') + "</option>";
			opHtml += "<option value=XML>" + AJS.I18n.getText('enav.export.xml.schedule.label') + "</option>";
			AJS.$("#zephyr-importer-filetype").html(opHtml);

			//AJS.$("#xmlPropDiv").hide();
			AJS.$("#filetypeprefrow").show();
			AJS.$("#zephyr-importer-filepref").attr("accept", ".xls,.xlsx");
			AJS.$("#zephyr-importer-folderpref").attr("accept", ".xls,.xlsx");
			AJS.$('#filesettingrow').show();
		}

		this.initFileFieldsMappingTypes = function () {
			var opHtml = "";
			opHtml += "<option value=savedPref>" + AJS.I18n.getText('zephyr-je.pdb.importer.fileOrSaved.saved') + "</option>";
			opHtml += "<option value=savedPref selected>" + AJS.I18n.getText('zephyr-je.pdb.importer.fileOrSaved.file') + "</option>";

			AJS.$("#zephyr-importer-filefieldssmappingtype").html(opHtml);
			Zephyr.Importer.Config.populateSavedMappingPreferences();
		}

		this.populateProjects = function () {
			jQuery.ajax({
				url: contextPath + "/rest/api/latest/project/",
				type: "get",
				async: false,
				success: function (response, status, jqXHR) {
					var ret = [];
					var allValues = [];
					AJS.$(response).each(
						function () {
							var itemDescriptor = new AJS.ItemDescriptor({
								value: this.id.toString(), // value of
								label: this.name, // title
								title: this.projectTypeKey,
								key: this.key,
								highlighted: true
							});
							if (_.contains(selectedProjectIds, this.id.toString())) {
								ret.push(itemDescriptor);
							}
							allValues.push(itemDescriptor);
						}
					);
					//allProjects = allValues;
					if (ret && ret.length > 0) {
						//projectList = ret;
					}
					return;
				},
				error: showError
			});
		}

		this.populateIssueTypes = function () {


			var that = this,
				formatState = function (state) {
					if (!state.id) {
						return state.text;
					}
					var iconUrl = jQuery(state.element).data('iconurl'),
						$state = AJS.$('<span><img src="' + iconUrl + '" class="select-img-icon" /> ' + state.text + '</span>');
					return $state;
				};
			var opHtml = "";
			if (selectedProjectId != undefined) {
				var issueTypes = Zephyr.Importer.Config.getIssueTypesByProjectId();
				var issueTypesModified = [];
				var j = 0;
				for (var i = 0; i < issueTypes.length; i++) {
					if (issueTypes[i].subtask != true) {
						issueTypesModified[j++] = issueTypes[i];
					}
				}
				var issueTypesHTML = ZEPHYR.Templates.Project.Traceability.renderIssueTypes({
					issueTypes: issueTypesModified
				});
			}
			AJS.$('#zephyr-importer-issuetypes').html(issueTypesHTML);
			AJS.$('#zephyr-importer-issuetypes').append('<br>');
			AJS.$('#zephyr-importer-issuetypes').append('<p style="display : inline;color : red;" id="errorForDropDown2"></p>');

			if (AJS.$.auiSelect2) {
				AJS.$('#issueTypes-dd').auiSelect2();
				AJS.$('.select2-container').css({ 'width': '200px' });
				AJS.$('.issueTypes-dd span.select2-chosen').html(AJS.I18n.getText('zephyr-je.pdb.traceability.select.issuetype.label'));
			} else {
				if (that.issueType) {
					if (that.issueTypeId)
						AJS.$('#issueTypes-dd').find('option[value="' + that.issueTypeId + '"]').attr('selected', 'selected');
					else
						AJS.$('#issueTypes-dd').find('option[title="' + that.issueType + '"]').attr('selected', 'selected');
				}
				var issueSelect = new AJS.SingleSelect({
					element: AJS.$('#issueTypes-dd'),
					maxInlineResultsDisplayed: 15,
					maxWidth: 400,
					submitInputVal: true,
					matchingStrategy: '(^|.*?(\\s*|\\(\))({0})(.*)' // Fix for ZFJ-1462
				});
				if (!that.issueType) {
					AJS.$('#issueTypes-dd-field').attr('placeholder', AJS.I18n.getText('zephyr-je.pdb.traceability.select.issuetype.label'));
					if (issueSelect.$container)
						issueSelect.clear();
				}
				AJS.$('#issueTypes-dd-field').unbind('keydown');
				AJS.$('#issueTypes-dd-field').bind('keydown', function (ev) {
					var _keyCode = ev.keyCode || ev.which;

					if (_keyCode == 13)
						ev.preventDefault();
				});
			}

		}

		this.populateIssueTypeFieldsContainter = function () {
			//var projectId = descriptor.properties.value;
			//console.log("projectid---------->"+projectId);
			AJS.$('#mappingContainersRow').show();
			var issueType = 'Test';
			Zephyr.Importer.Config.getIssueFieldsMetaData();
			var tblHtml = "";
			var issueFlds = JSON.parse(sessionStorage.getItem('issue_fields'));
			var keys = Object.keys(issueFlds);
			AJS.$.each(keys, function (index, key) {
				var displayName = issueFlds[key].displayName && issueFlds[key].displayName.length >= 15 ? issueFlds[key].displayName.substring(0, 15) + '...' : issueFlds[key].displayName;
				if (issueFlds[key].mandatory == true) {
					tblHtml += '<div class="container"><div class="required" title="' + issueFlds[key].displayName + '" style="text-overflow: ellipsis; overflow: hidden; white-space: nowrap;"><label>' + displayName + '</label></div></div>';
				} else {
					tblHtml += '<div><div class="container" title="' + issueFlds[key].displayName + '" style="text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">' + displayName + '</div></div>';
				}
			});
			AJS.$("#zephyr-importer-issuefields").html(tblHtml);
			Zephyr.Importer.Config.populateDNDContainer();
			Zephyr.Importer.Config.refreshDNDTable();
		}

		this.populateDNDContainer = function () {
			var issueFieldsConfig = JSON.parse(sessionStorage.getItem('issue_fields'));
			if (issueFieldsConfig != null) {
				var length = Object.keys(issueFieldsConfig).length;
				var tblObj = AJS.$('#dndTbl');
				tblObj.empty();
				var counterForMapped = 0;
				for (var n = 0; n < length; ++n) {
					counterForMapped = counterForMapped + 1;
					var mappedCounter = "mapped" + "" + counterForMapped;
					tblObj.append(' <tr style="line-height:30px"><td class="container" style="padding: 2px;"><div  id=' + mappedCounter + ' style="border: 1px gray solid;" class="position dragable-object" ></div></td> </tr>');

				}

			}
		}


		// this.drag = function (event) {
		//     selectedIdForDrag = event.target.id;
		// 	console.log(1);
		// }

		// this.allowDrop = function (event) {
		// 	console.log(2);
		// 	event.preventDefault();
		// }

		// this.drop = function (event) {
		// 	event.preventDefault();
		// 	console.log(3);

		// 	var drop_target = event.target;
		// 	var data = selectedIdForDrag;
		// 	var drag_target = document.getElementById(data);
		// 	console.log(data);
		// 	if(data){
		// 	var tmp = document.createElement('span');
		// 	tmp.className='hide';
		// 	drop_target.before(tmp);
		// 	drag_target.before(drop_target);
		// 	tmp.replaceWith(drag_target);
		// 	}

		//     // if (event.target.id == "drag-item") {
		// 	// 	console.log("hi vinu");
		//     //     var data = selectedIdForDrag;
		//     //     document.getElementById(data).style.height = "30px";
		//     //     // document.getElementById(data).style.margin = "-1px";
		//     //     document.getElementById(data).style.width = "100%";
		//     //     event.target.appendChild(document.getElementById(data));
		//     //     Zephyr.Importer.Config.refreshFileFields();
		//     // }
		// }

		this.populateLinkingIssues = function () {
			//var linkType = AJS.$('#zephyr-importer-issuetypes option:selected').text();
			var issueTypes = Zephyr.Importer.Config.getIssueTypesByProjectId();
			var typeFilter = "(";
			for (var i = 0; i < issueTypes.length; i++) {
				typeFilter += "type=" + issueTypes[i].name;
				if (i != issueTypes.length - 1) {
					typeFilter += " or ";
				}
			}
			typeFilter += ")";
			var linksDD = new AJS.SingleSelect({
				element: AJS.$("#zephyr-importer-links"),
				itemAttrDisplayed: "label",
				maxInlineResultsDisplayed: 15,
				maxWidth: 200,
				showDropdownButton: true,
				submitInputVal: true,
				overlabel: AJS.I18n.getText("zephyr-je.testboard.select.project.label"),
				errorMessage: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.disable.testmenu.project.nomatching"),
				ajaxOptions: {
					url: contextPath + "/rest/api/2/issue/picker?currentJQL=" + typeFilter + "&currentProjectId=" + selectedProjectId + "&showSubTasks=true&showSubTaskParent=true&appId=",
					query: true,
					cache: false,
					minQueryLength: 2,
					formatResponse: function (response) {
						var ret = [];
						var allValues = [];
						var query = AJS.$("#zephyr-importer-links").val();
						var sections = response.sections;
						var csGroupDescriptor;
						var hsGroupDescriptor;
						var csItemFlag;
						if (sections != null) {
							AJS.$(sections).each(function (index, value) {
								if (value.label == 'Current Search') {
									var issues = value.issues;
									csGroupDescriptor = new AJS.GroupDescriptor({
										weight: value.id, // order or groups in
										label: value.label + ' (' + value.sub + ' )'
									});
									if (issues != null && issues.length > 0) {
										//alert('contextPath-->'+contextPath);
										csItemFlag = 'true';
										AJS.$(issues).each(function () {
											csGroupDescriptor.addItem(new AJS.ItemDescriptor({
												value: this.key.toString(), // value of
												icon: contextPath + this.img,
												label: this.key + " - " + this.summaryText, // title
												title: this.summaryText,
												key: this.key,
												highlighted: true
											}));

										});
										//ret.push(groupDescriptor);
									}

								}
								if (value.label == 'History Search') {
									var issues = value.issues;
									hsGroupDescriptor = new AJS.GroupDescriptor({
										weight: value.id, // order or groups in
										label: value.label + ' (' + value.sub + ' )'
									});
									if (issues != null && issues.length > 0) {
										csItemFlag = 'true';
										AJS.$(issues).each(function () {
											hsGroupDescriptor.addItem(new AJS.ItemDescriptor({
												value: this.key.toString(), // value of
												label: this.key + " - " + this.summaryText, // title
												title: this.summaryText,
												key: this.key,
												icon: contextPath + this.img,
												highlighted: true
											}));

										});
										//ret.push(groupDescriptor);
									}
								}
							});
							if (csItemFlag == 'true') {
								ret.push(csGroupDescriptor);
								ret.push(hsGroupDescriptor);
							}
						}



						return ret;
					}
				}
			});
		}


		this.populateTestIssuesByProject = function () {
			AJS.$('#zephyr-importer-test-issues-single-select').remove();
			var issueType = AJS.$('#zephyr-importer-issuetypes option:selected').text();
			var testIssuesDD = new AJS.SingleSelect({
				element: AJS.$("#zephyr-importer-test-issues"),
				itemAttrDisplayed: "label",
				maxInlineResultsDisplayed: 15,
				maxWidth: 200,
				showDropdownButton: true,
				submitInputVal: true,
				//overlabel: AJS.I18n.getText("zephyr-je.testboard.select.project.label"),
				errorMessage: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.disable.testmenu.project.nomatching"),
				ajaxOptions: {
					url: contextPath + "/rest/api/2/issue/picker?currentJQL=type=" + issueType + "&currentProjectId=" + selectedProjectId + "&showSubTasks=true&showSubTaskParent=true&appId=",
					query: true,
					cache: false,
					minQueryLength: 2,
					formatResponse: function (response) {
						var ret = [];
						var allValues = [];
						var query = AJS.$("#zephyr-importer-test-issues").val();
						var sections = response.sections;
						var csGroupDescriptor;
						var hsGroupDescriptor;
						var csItemFlag;
						if (sections != null) {
							AJS.$(sections).each(function (index, value) {
								if (value.label == 'Current Search') {
									var issues = value.issues;
									csGroupDescriptor = new AJS.GroupDescriptor({
										weight: value.id, // order or groups in
										label: value.label + ' (' + value.sub + ' )'
									});
									if (issues != null && issues.length > 0) {
										//alert('contextPath-->'+contextPath);
										csItemFlag = 'true';
										AJS.$(issues).each(function () {
											csGroupDescriptor.addItem(new AJS.ItemDescriptor({
												value: this.key.toString(), // value of
												icon: contextPath + this.img,
												label: this.key + " - " + this.summaryText, // title
												title: this.summaryText,
												key: this.key,
												highlighted: true
											}));

										});
										//ret.push(groupDescriptor);
									}

								}
								if (value.label == 'History Search') {
									var issues = value.issues;
									hsGroupDescriptor = new AJS.GroupDescriptor({
										weight: value.id, // order or groups in
										label: value.label + ' (' + value.sub + ' )'
									});
									if (issues != null && issues.length > 0) {
										AJS.$(issues).each(function () {
											hsGroupDescriptor.addItem(new AJS.ItemDescriptor({
												value: this.key.toString(), // value of
												label: this.key + " - " + this.summaryText, // title
												title: this.summaryText,
												key: this.key,
												icon: contextPath + this.img,
												highlighted: true
											}));

										});
										//ret.push(groupDescriptor);
									}
								}
							});
							if (csItemFlag == 'true') {
								ret.push(csGroupDescriptor);
								ret.push(hsGroupDescriptor);
							}
						}



						return ret;
					}
				}
			});
		}

		this.populateFileTypePreferences = function () {
			var opHtml = "";
			opHtml += "<option value=sheet selected>" + AJS.I18n.getText('zephyr-je.pdb.importer.discriminator.sheet') + "</option>";
			opHtml += "<option value=emptyrow>" + AJS.I18n.getText('zephyr-je.pdb.importer.discriminator.emptyRow') + "</option>";
			opHtml += "<option value=idchange>" + AJS.I18n.getText('zephyr-je.pdb.importer.discriminator.idChange') + "</option>";
			opHtml += "<option value=testname>" + AJS.I18n.getText('zephyr-je.pdb.importer.discriminator.nameChange') + "</option>";
			AJS.$("#zephyr-importer-discriminator").html(opHtml);
		}

		this.populateFileFields = function () {
			var file = AJS.$('#zephyr-importer-filepref')[0].files[0];
			// if(file == undefined || file != )
			if (file != undefined) {

				var fileName = AJS.$('#zephyr-importer-filepref')[0].files[0].name;
				var ext = fileName.split('.').pop();
				var fileType = AJS.$('#zephyr-importer-filetype option:selected').text();
				if (ext != 'xls' && ext != 'xml' && ext != 'xlsx') {
					document.getElementById('error-file-select').innerHTML = AJS.I18n.getText('zephyr-je.pdb.importer.file.type.invalid.msg');
				}
				else if (((ext == 'xls' || ext == 'xlsx') && fileType != 'Excel') || (ext == 'xml' && fileType != 'XML')) {
					document.getElementById('error-file-select').innerHTML = AJS.I18n.getText('zephyr-je.pdb.importer.file.type.relevant.msg');
				}
				else {
					document.getElementById('error-file-select').innerHTML = "";
					var jsonResponse = Zephyr.Importer.Config.getFileFields(file, fileType);
					if (jsonResponse != null) {
						sessionStorage.setItem('file_fields_mapping', JSON.stringify(jsonResponse));
						var fieldtbl = AJS.$('#fieldsDiv');
						fieldtbl.empty();
						AJS.$.each(jsonResponse, function (key, value) {
							fieldtbl.append('<div class="importer-field-item" style="border: 1px solid gray;" title="' + key + '">' + key + '</div>');
						});
						//AJS.$('.importer-field-item').sortable({ connectWith: ".con" }).disableSelection();
					}
				}
			}
			Zephyr.Importer.Config.refreshFileFields();
		}

		this.HandleHelpButtonClick = function () {
			var dialog = new JIRA.FormDialog({
				id: "help-popup",
				content: function (callback) {
					var opHtml = '';
					opHtml += '<h2 class="dialog-title">';
					opHtml += AJS.I18n.getText('zephyr-je.pdb.instruction.label');
					opHtml += '</h2><div class="form-body">';
					opHtml += '<div id="cycle-aui-message-bar">';
					opHtml += '<div class="aui-message info" id="">';
					opHtml += '<span class="aui-icon icon-info"></span>';
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section1');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section1.0');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section1.1');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section1.2');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section1.3');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section1.4');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section1.5');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section1.6');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section1.7');
					opHtml += "<br/><br/>";
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section2.0');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section2.1');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section2.2');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section2.3');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section2.4');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section2.5');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section2.6');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section2.7');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section2.8');

					opHtml += "<br/><br/>";
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section3');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section3.0');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section3.1');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section3.2');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section3.3');

					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section4.0');
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section4.1');

					opHtml += "<br/><br/>";
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section5');
					var sampleFilePath = contextPath + '/download/resources/com.thed.zephyr.je:zephyr-je-tests-importer/zephyr-importer-xml-sample.xml';
					opHtml += AJS.I18n.getText('zephyr-je.pdb.importer.instruction.section5.0', sampleFilePath);

					opHtml += '</div></div></div>';
					callback(opHtml);
				}
			});
			dialog.show();
		}

		this.populateFolderFields = function () {
			var isSame = true;
			var mappings = [];
			var file;
			var fileType = AJS.$('#zephyr-importer-filetype option:selected').text();
			var files = AJS.$('#zephyr-importer-folderpref')[0].files;
			AJS.$.each(files, function (index, file) {
				if (!file.name.startsWith('.')) {
					var mapping = Zephyr.Importer.Config.getFileFields(file, fileType);
					if(mapping){
						mappings.push(mapping);
					}
				}
			});
			if (mappings != null && mappings != undefined) {
				var firstMapping = {};
				if (mappings.length > 1) {
					firstMapping = mappings[0];
					AJS.$.each(mappings, function (index, mapping) {
						isSame = Zephyr.Importer.Config.compareFileFieldMappings(firstMapping, mapping);
						if (!isSame) {
							var dialog = new AJS.Dialog({
								width: 800,
								height: 270,
								id: "dialog-error"
							});
							dialog.addHeader(AJS.I18n.getText('zephyr.je.submit.form.error.title'));

							dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
							AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
								title: AJS.I18n.getText('zephyr.je.submit.form.error.title'),
								body: AJS.I18n.getText('zephyr-je.pdb.importer.folder.fields.nomatching'),
								closeable: false
							});

							dialog.addLink("Close", function (dialog) {
								dialog.hide();
							}, "#");
							dialog.show();
							//alert("Headers are not same in the selected folder files")
							return false;
						}
					});
				}
				if (isSame) {
					sessionStorage.setItem('file_fields_mapping', JSON.stringify(firstMapping));
					var fieldtbl = AJS.$('#fieldsDiv');
					fieldtbl.empty();
					AJS.$.each(firstMapping, function (key, value) {
						fieldtbl.append('<div class="importer-field-item">' + key + '</div>');
					});
					//AJS.$('.importer-field-item').draggable();
					//AJS.$('.importer-field-item').sortable({ connectWith: ".con" }).disableSelection();
				}

			}
			Zephyr.Importer.Config.refreshFileFields();
		}

		this.populateSavedMappingPreferences = function () {
			var jsonResponse = Zephyr.Importer.Config.getSavedPreferencesByFileType();
			if (jsonResponse != null) {
				var opHtml = "";
				var mappingArr = jsonResponse.mappings;
				opHtml += "<option selected> Select </option>";
				if (mappingArr != null && mappingArr != undefined) {
					AJS.$.each(mappingArr, function (index, val) {
						savedPreferences[val.id] = val.mappingSet;
						opHtml += "<option value='" + val.id + "'>" + val.id + '</option>';
					});
				}
				AJS.$("#zephyr-importer-savedprefs").html(opHtml);
			}
		}

		this.populateDNDContainersFromSavedPreference = function () {
			var selectedPref = AJS.$('#zephyr-importer-savedprefs option:selected').text();
			var mappingSet = savedPreferences[selectedPref];
			var issueFlds = JSON.parse(sessionStorage.getItem('issue_fields'));
			var issueFldsKeys = Object.keys(JSON.parse(sessionStorage.getItem('issue_fields')));
			if (mappingSet != null) {
				var mappingMap = {};
				AJS.$.each(mappingSet, function (index, mapping) {
					mappingMap[mapping["zephyrField"]] = mapping["mappedField"];
				});
				var issueTbl = AJS.$("#zephyr-importer-issuefields");
				var dndTbl = AJS.$('#dndTbl');
				issueTbl.html('');
				dndTbl.empty();
				var counterForMappedForSave = 0;
				AJS.$.each(issueFldsKeys, function (index, value) {
					var issueFld = value["zephyrField"];
					//var dndFld = value["mappedField"];

					var dndFld = mappingMap[issueFlds[value].displayName];
					if (issueFlds[value].mandatory == true) {
						issueTbl.append('<div style="line-height:30px"><div class="container" title="' + issueFlds[value].displayName + '" style="padding : 4.9px 0px;" ><div class="required" style="border: 0px; height:29px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;"><label>' + issueFlds[value].displayName + '</label></div></div></div>');
					} else {
						issueTbl.append('<div style="line-height:30px"><div class="container" title="' + issueFlds[value].displayName + '" style="padding : 4.9px 0px; height:29px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;" >' + issueFlds[value].displayName + '</div></div>');
					}
					counterForMappedForSave = counterForMappedForSave + 1;
					var mappedCounterSave = "mappedSave" + "" + counterForMappedForSave;
					if (dndFld != undefined) {
						dndTbl.append(' <tr style="line-height:30px"><td class="container" style="padding : 2px;"><div id=' + mappedCounterSave + ' style="border: 1px gray solid;" class="position dragable-object">' + dndFld + '</div></td> </tr>');
					} else {
						dndTbl.append(' <tr style="line-height:30px"><td class="container" style="padding : 2px;"><div id=' + mappedCounterSave + ' style="border: 1px gray solid;" class="position dragable-object"></div></td> </tr>');
					}
				});
				Zephyr.Importer.Config.refreshDNDTable();
			}
		}

		this.enablePrefButtons = function () {
			var selectedPref = AJS.$('#zephyr-importer-savedprefs option:selected').text().trim();
			if (selectedPref != 'Select' && selectedPref != '') {
				AJS.$('#zephyr-importer-savedprefs-update').show();
				AJS.$('#zephyr-importer-savedprefs-delete').show();
			}
		}

		this.getLinkTypes = function () {
			var linkTypes = [];
			jQuery.ajax({
				url: contextPath + "/rest/api/latest/issueLinkType",
				type: "get",
				dataType: "json",
				async: false,
				success: function (response) {
					issueTypes = [];
					AJS.$(response.issueLinkTypes).each(function () {
						var inwardItemDesc = new AJS.ItemDescriptor({
							value: this.id.toString(), // value of
							label: this.inward // title
						});
						linkTypes.push(inwardItemDesc);
						if (this.inward != this.outward) {
							var outwardItemDesc = new AJS.ItemDescriptor({
								value: this.id.toString(), // value of
								label: this.outward // title
							});
							linkTypes.push(outwardItemDesc);
						}
					});
				},
				error: showError
			});
			return linkTypes;
		}
		this.getIssueTypesByProjectId = function () {
			var issueTypes;
			AJS.$.ajax({
				type: "get",
				url: contextPath + "/rest/api/latest/issue/createmeta?projectIds=" + selectedProjectId + "&expand=projects.issuetypes.fields",
				dataType: "json",
				async: false,
				success: function (response) {
					//console.log("success response-->" + response);
					issueTypes = response.projects[0].issuetypes;
				},
				error: showError
			});
			return issueTypes;

		}

		this.getIssueTypes = function () {
			var issueTypes;
			jQuery.ajax({
				url: contextPath + "/rest/api/latest/issuetype",
				type: "get",
				dataType: "json",
				async: false,
				success: function (response) {
					issueTypes = response;
				},
				error: showError
			});
			return issueTypes;
		}

		this.getIssueFieldsMetaData = function () {
			var issueType = AJS.$('#zephyr-importer-issuetypes option:selected').text();
			AJS.$.ajax({
				url: (contextPath + "/rest/api/latest/issue/createmeta?projectIds=" + selectedProjectId + "&issuetypeNames=" + encodeURIComponent(issueType) + "&expand=projects.issuetypes.fields"),
				type: "get",
				dataType: "json",
				async: false,
				success: function (response) {
					var fieldConfigMap = {};
					var fieldConfigArray = [];
					var fields = response.projects[0].issuetypes[0].fields;
					if (fields != null && fields != undefined) {
						var fieldsKeys = Object.keys(fields);
						issueFields = [];
						AJS.$(fieldsKeys).each(function (index, value) {
							var fieldConfig = {};
							var schema = {};
							var allowedValues = [];
							//console.log(fields[value].name);
							if ((AJS.$.inArray(value, issueFieldsFilter) < 0) && (AJS.$.inArray(fields[value].name, issueFieldsNamesFilter) < 0)) {
								if (fields[value].name == 'Summary') {
									fieldConfig.displayName = 'Name';
								} else {
									fieldConfig.displayName = fields[value].name;
								}
								fieldConfig.id = value;
								fieldConfig.mandatory = fields[value].required;
								if (fields[value].required) {
									fieldConfig.priority = 1;
								} else {
									fieldConfig.priority = 3;
								}
								if (fields[value].schema.type != undefined) {
									schema.type = fields[value].schema.type;
								}
								if (fields[value].schema.items != undefined) {
									schema.items = fields[value].schema.items;
								}
								if (fields[value].schema.custom != undefined) {
									schema.custom = fields[value].schema.custom;
								}
								fieldConfig.fieldSchema = schema;
								//console.log(fields[value].allowedValues);
								if (fields[value].allowedValues != undefined) {
									allowedValues = [];
									AJS.$.each(fields[value].allowedValues, function (i, val) {
										if (val.name != undefined) {
											allowedValues.push(val.name + ":" + val.id);
										} else if (val.value != undefined) {
											allowedValues.push(val.value + ":" + val.id);
										}
									});
									fieldConfig.allowedValues = allowedValues;
								}
								issueFields.sort(function (a, b) { return fields[value].required == true });
								issueFields.push(fields[value].name);
								//fieldConfigMap[value] = fieldConfig;
								fieldConfigArray.push(fieldConfig);
							}
						});
					}
					// filter fields

					// added test step related custom fields

					/*var fieldConfig = {};
				  var fieldSchema={};
				  fieldSchema.type="string";
				  fieldConfig.displayName="Test Step Order";
				  fieldConfig.id="stepOrder";
				  fieldConfig.mandatory=false;
				  fieldConfig.fieldSchema=fieldSchema;
				  issueFields.push("Test Step Order");
				  fieldConfigMap["stepOrder"] = fieldConfig;*/
					if (issueType == 'Test') {
						var fieldConfig = {};
						var fieldSchema = {};
						fieldSchema.type = "string";
						fieldConfig.displayName = "Step";
						fieldConfig.id = "stepAction";
						fieldConfig.mandatory = false;
						fieldConfig.priority = 2;
						fieldConfig.fieldSchema = fieldSchema;
						issueFields.push("Test Step Action");
						//fieldConfigMap["stepAction"] = fieldConfig;
						fieldConfigArray.push(fieldConfig);

						var fieldConfig = {};
						var fieldSchema = {};
						fieldSchema.type = "string";
						fieldConfig.displayName = "Result";
						fieldConfig.id = "stepExpectedResults";
						fieldConfig.mandatory = false;
						fieldConfig.priority = 2;
						fieldConfig.fieldSchema = fieldSchema;
						issueFields.push("Result");
						//fieldConfigMap["stepExpectedResults"] = fieldConfig;
						fieldConfigArray.push(fieldConfig);

						var fieldConfig = {};
						var fieldSchema = {};
						fieldSchema.type = "string";
						fieldConfig.displayName = "Testdata";
						fieldConfig.id = "stepData";
						fieldConfig.mandatory = false;
						fieldConfig.priority = 2;
						fieldConfig.fieldSchema = fieldSchema;
						issueFields.push("Testdata");
						//fieldConfigMap["stepData"] = fieldConfig;
						fieldConfigArray.push(fieldConfig);

						var fieldConfig = {};
						var fieldSchema = {};
						fieldSchema.type = "string";
						fieldConfig.displayName = "Issue Key [To add steps]";
						fieldConfig.id = "issueKey";
						fieldConfig.mandatory = false;
						fieldConfig.priority = 2;
						fieldConfig.fieldSchema = fieldSchema;
						issueFields.push("IssueKey");
						fieldConfigArray.push(fieldConfig);
					}

					var fileDiscriminator = AJS.$('#zephyr-importer-discriminator option:selected').text();
					var fieldConfig = {};
					var fieldSchema = {};
					fieldSchema.type = "string";
					fieldConfig.displayName = "External ID";
					fieldConfig.id = "externalid";
					if (fileDiscriminator == 'By ID Change') {
						fieldConfig.mandatory = true;
						fieldConfig.priority = 1;
					} else {
						fieldConfig.mandatory = false;
						fieldConfig.priority = 3;
					}
					fieldConfig.fieldSchema = fieldSchema;
					issueFields.push("External ID");
					//fieldConfigMap["externalid"] = fieldConfig;
					fieldConfigArray.push(fieldConfig);

					var fieldConfig = {};
					var fieldSchema = {};
					fieldSchema.type = "array";
					fieldSchema.items = "string";
					fieldConfig.displayName = "Comments";
					fieldConfig.id = "comments";
					fieldConfig.mandatory = false;
					fieldConfig.priority = 3;
					fieldConfig.fieldSchema = fieldSchema;
					issueFields.push("Comments");
					//fieldConfigMap["comments"] = fieldConfig;
					fieldConfigArray.push(fieldConfig);

					fieldConfigArray.sort(function (a, b) { return (a.priority > b.priority) ? 1 : ((b.priority > a.priority) ? -1 : 0); });
					for (var idx = 0; idx < fieldConfigArray.length; idx++) {
						fieldConfigMap[fieldConfigArray[idx].id] = fieldConfigArray[idx];
					}
					sessionStorage.setItem('issue_fields', JSON.stringify(fieldConfigMap));
				},
				error: showError
			});
		}

		this.updateIssueFields = function () {
			var fileDiscriminator = AJS.$('#zephyr-importer-discriminator option:selected').text();
			var issueFields = JSON.parse(sessionStorage.getItem('issue_fields'));
			var fieldConfig = issueFields["externalid"];
			if (fileDiscriminator == 'By ID Change') {
				fieldConfig.mandatory = true;
			} else {
				fieldConfig.mandatory = false;
			}
			issueFields["externalid"] = fieldConfig;
			sessionStorage.setItem('issue_fields', JSON.stringify(issueFields));
			issueFields = JSON.parse(sessionStorage.getItem('issue_fields'));
			fieldConfig = issueFields["externalid"];
			//console.log("fieldConfig.mandatory-->"+fieldConfig.mandatory );
			Zephyr.Importer.Config.populateIssueTypeFieldsContainter();

		}

		this.getFileFields = function (file, fileType) {
			var jsonResponse;
			//console.log("file--->"+file);
			var data = new FormData();
			data.append('file', file);
			//data.append('fileType', fileType);
			var importData = Zephyr.Importer.Config.getImportData();
			data.append('importjob', JSON.stringify(importData));

			AJS.$.ajax({
				type: "POST",
				url: contextPath + '/rest/zephyr/latest/importer/fieldMapping',
				enctype: "multipart/form-data",
				data: data,
				processData: false,
				contentType: false,
				dataType: "json",
				async: false,
				success: function (response) {
					jsonResponse = response;
				},
				error: function (e) {
					var responseJson;
					try {
						responseJson = jQuery.parseJSON(e.responseText);
					  } catch (e) {
					  }
					if(responseJson)
						AJS.$("#error-file-select").text(responseJson.errorDesc).show().delay(5000).hide(1000);
					else
						AJS.$("#error-file-select").text(e.responseText).show().delay(5000).hide(1000);
				}
			});
			return jsonResponse;
		}

		this.compareFileFieldMappings = function (mappings1, mappings2) {
			var isSame = true;
			var testVal;
			AJS.$.each(mappings1, function (key, val) {
				testVal = mappings2[key];
				//console.log('testval-->'+testVal);
				if (testVal != val || testVal == undefined) {
					isSame = false;
					return false;
				}
			});
			return isSame;
		}

		this.getMappedFields = function () {
			var mappedFlds = [];
			var children = jQuery('#item-list1').sortable('refreshPositions').children();
			//console.log('Positions: ');
			AJS.$('#dndTbl tr td').each(function () {
				mappedFlds.push(AJS.$(this).text());
			});

			return mappedFlds;
		}

		this.getMappedMappings = function () {
			var mappings = [];
			var mapping = {};
			var i = 0;
			var issueFlds = JSON.parse(sessionStorage.getItem('issue_fields'));
			var issueFldsKeys = Object.keys(JSON.parse(sessionStorage.getItem('issue_fields')));
			var fileFlds = Zephyr.Importer.Config.getMappedFields();
			var fileFieldMappings = JSON.parse(sessionStorage.getItem('file_fields_mapping'));
			AJS.$.each(issueFldsKeys, function (index, value) {
				if (AJS.$.inArray(value, issueFieldsFilter) < 0 && AJS.$.inArray(issueFlds[value].displayName, issueFieldsNamesFilter) < 0) {
					//console.log("zephyr field-->"+value);
					//console.log("mapped field-->"+fileFieldMappings[fileFlds[index]]);
					mapping = {};
					mapping.zephyrField = value;
					//mapping.mappedField = fileFieldMappings[fileFlds[index]];
					mapping.mappedField = fileFlds[index];
					mappings[i] = mapping;
					//mappings.push(mapping);
					i++;
				}
				//index++;
			});
			return mappings;
		}

		this.getTestMappedMappings = function () {
			var mappings = [];
			var mapping = {};
			var issueFlds = Object.keys(JSON.parse(sessionStorage.getItem('issue_fields')));
			var fileFlds = Zephyr.Importer.Config.getMappedFields();
			var fileFieldMappings = JSON.parse(sessionStorage.getItem('file_fields_mapping'));
			AJS.$.each(issueFlds, function (index, value) {
				if (AJS.$.inArray(value, testStepFields) > -1) {
					//console.log("zephyr field-->"+value);
					//console.log("fileFlds[index]-->"+fileFlds[index]);
					//console.log("fileFieldMappings[fileFlds[index]]-->"+fileFieldMappings[fileFlds[index]]);
					mapping = {};
					mapping.zephyrField = value;
					//mapping.mappedField = fileFieldMappings[fileFlds[index]];
					mapping.mappedField = fileFlds[index];
					mappings.push(mapping);
				}
				index++;
			});
			return mappings;
		}

		this.getMappedMappingsForPref = function () {
			var mappings = [];
			var mapping = {};
			var i = 0;
			var issueFlds = JSON.parse(sessionStorage.getItem('issue_fields'));
			var keys = Object.keys(issueFlds);
			var fileFlds = Zephyr.Importer.Config.getMappedFields();

			AJS.$.each(keys, function (index, key) {
				if (AJS.$.inArray(key, issueFieldsFilter) < 0 && AJS.$.inArray(issueFlds[key].displayName, issueFieldsNamesFilter) < 0) {
					mapping = {};
					mapping.zephyrField = issueFlds[key].displayName;
					mapping.mappedField = fileFlds[index];
					mappings[i] = mapping;
					i++;
					//mappings.push(mapping);
				}
				//index++;
			});
			return mappings;
		}

		/*this.getLinks = function(linkType) {
				AJS.$.ajax({
					url: contextPath + "/rest/api/latest/issue/createmeta?projectIds="+selectedProjectId + '&issuetypeNames=' + issueType + '&expand=projects.issuetypes.fields',
					type : "get",
					dataType: "json",
					async : false,
					success: function(response){
					    var fieldConfigMap = {};
					    var fields = response.projects[0].issuetypes[0].fields;
					    var fieldsKeys = Object.keys(fields);
					    issueFields =[];
						AJS.$(fieldsKeys).each(function(index, value) {
	        			  	var fieldConfig = {};
	        			  	var schema = {};
	        			  	var allowedValues =[];

	        			  	fieldConfig.id = fields[value].name;
   							fieldConfig.mandatory = fields[value].required;
   							if (fields[value].schema.type != undefined) {
   				 					schema.type = fields[value].schema.type;
   							}
   							if (fields[value].schema.items != undefined) {
    								schema.items = fields[value].schema.items;
  	 						}
   							if (fields[value].schema.custom != undefined) {
    								schema.custom = fields[value].schema.custom;
   							}
   							fieldConfig.fieldSchema = schema;

   							if (fields[value].allowedValues != undefined) {
    							jQuery.each(fields[value].allowedValues, function(index, value) {
     							allowedValues.push = value.name + ":" + value.id;
    						});
    						fieldConfig.allowedValues = allowedValues;
   							}
   							issueFields.push(fields[value].name);
   							fieldConfigMap[value] = fieldConfig;
	    	            });
	    	          sessionStorage.setItem('issue_fields', JSON.stringify(fieldConfigMap));
					},
					error : showError
				});
		}*/

		this.getSavedPreferencesByFileType = function () {
			var jsonResponse;
			var selectedFileType = AJS.$('#zephyr-importer-filetype option:selected').text()
			AJS.$.ajax({
				url: contextPath + '/rest/zephyr/latest/preference/getImportMappingPreference?fileType=' + selectedFileType,
				type: "get",
				dataType: "json",
				async: false,
				success: function (response) {
					jsonResponse = response;
				},
				error: showError
			});
			return jsonResponse;
		}

		AJS.$('#inlineDi').live('click', function (event) {
			var htmlContent = AJS.$(this).next('#more-details-help').html();
			var descriptionInlineDialog = AJS.InlineDialog(AJS.$("#inlineDi"), "description",
				function (content, trigger, showPopup) {
					content.css({ "padding": "5px" }).html(htmlContent);
					showPopup();
					return false;
				}
			);
			descriptionInlineDialog.show(event);
		});

		AJS.$('#helpMsgForExcel').live('click', function (event) {
			var htmlContent = AJS.$(this).next('#more-details-help-2').html();
			var descriptionInlineDialog = AJS.InlineDialog(AJS.$("#helpMsgForExcel"), "description",
				function (content, trigger, showPopup) {
					content.css({ "padding": "5px" }).html(htmlContent);
					showPopup();
					return false;
				}
			);
			descriptionInlineDialog.show(event);
		});

		this.saveMappingPreference = function () {
			var pref = {};
			var formBody = '<form class="aui"><div class="field-group"><label for="pref_name_lbl">Preference Name</label>'
				+ '<input id="pref_name_txt" class="text" type="text" >'
				+ '<div id="validation_error"></div></div></form>';
			var prefFlag = false;
			var dialog = new AJS.Dialog({
				width: 600,
				height: 250,
				id: "reset-link-dialog",
				closeOnOutsideClick: false
			});

			dialog.addHeader(AJS.I18n.getText("zephyr.je.admin.plugin.show.zephyr.test.issue.type.save"));
			dialog.addPanel("Panel", formBody, "panel-body");
			dialog.addButton("Save", function (dialog) {
				prefFlag = false;
				var prefName = dialog.getCurrentPanel().body.find('#pref_name_txt').val();
				//console.log('prefName'+prefName);
				if (prefName == '' || prefName == null || prefName == undefined) {
					dialog.getCurrentPanel().body.find('#validation_error').html(AJS.I18n.getText('zephyr-je.pdb.importer.required.valid.preference'));
					return false;
				}
				var prefNamesResponse = Zephyr.Importer.Config.getSavedPreferencesByFileType();
				if (prefNamesResponse != null) {
					var prefArr = prefNamesResponse.mappings;
					if (prefArr != null && prefArr != undefined) {
						AJS.$.each(prefArr, function (index, val) {
							if (prefName == val.id) {
								dialog.getCurrentPanel().body.find('#validation_error').html(AJS.I18n.getText('zephyr-je.pdb.importer.preference.already.exist'));
								prefFlag = true;
								return;
							}
						});

					}
				}
				if (!prefFlag) {
					//console.log('preference is saved');
					dialog.hide();
					var fileType = AJS.$('#zephyr-importer-filetype option:selected').text();
					pref.id = prefName;
					pref.fileType = fileType;
					pref.mappingSet = Zephyr.Importer.Config.getMappedMappingsForPref();
					AJS.$.ajax({
						url: contextPath + '/rest/zephyr/latest/preference/setImportMappingPreference',
						type: "POST",
						data: JSON.stringify(pref),
						contentType: "application/json",
						dataType: "json",
						async: false,
						success: function (response) {
							jsonResponse = response;
							//console.log('preference is saved successfully');
						},
						error: showError
					});
				}

			});
			dialog.addButton("Cancel", function (dialog) {
				dialog.hide();

			});
			dialog.show();
		}

		this.deleteMappingPreference = function () {
			var fileType = AJS.$('#zephyr-importer-filetype option:selected').text();
			var prefName = AJS.$('#zephyr-importer-savedprefs option:selected').text().trim();
			var pref = {};
			pref.id = prefName;
			pref.fileType = fileType;
			AJS.$.ajax({
				url: contextPath + '/rest/zephyr/latest/preference/deleteImportMappingPreference',
				type: "POST",
				data: JSON.stringify(pref),
				contentType: "application/json",
				dataType: "json",
				async: false,
				success: function (response) {
					jsonResponse = response;
					//console.log('preference is deleted successfully');
					Zephyr.Importer.Config.populateSavedMappingPreferences();
				},
				error: showError
			});
			Zephyr.Importer.Config.populateDNDContainer();
			Zephyr.Importer.Config.populateFileFields();

		}

		this.updateMappingPreference = function () {
			var fileType = AJS.$('#zephyr-importer-filetype option:selected').text();
			var prefName = AJS.$('#zephyr-importer-savedprefs option:selected').text().trim();
			if (prefName != 'Select' && prefName != '') {
				var pref = {};
				pref.id = prefName;
				pref.fileType = fileType;
				pref.mappingSet = Zephyr.Importer.Config.getMappedMappingsForPref();
				AJS.$.ajax({
					url: contextPath + '/rest/zephyr/latest/preference/setImportMappingPreference',
					type: "POST",
					data: JSON.stringify(pref),
					contentType: "application/json",
					dataType: "json",
					async: false,
					success: function (response) {
						jsonResponse = response;
						Zephyr.Importer.Config.populateSavedMappingPreferences();
						//console.log('preference is updated successfully');
					},
					error: showError
				});
				AJS.$('#zephyr-importer-savedprefs').val(prefName);
			}
		}
		this.processImport = function () {
			var isValid = Zephyr.Importer.Config.isValidImportData();
			if (isValid == true) {
				var importJobStr = "";
				var file = AJS.$('#zephyr-importer-filepref')[0].files[0];
				var importData = Zephyr.Importer.Config.getImportData();
				var data = new FormData();
				data.append('file', file);
				data.append('importjob', JSON.stringify(importData));
				AJS.$.ajax({
					type: "POST",
					url: contextPath + '/rest/zephyr/latest/importer/issueImport',
					enctype: "multipart/form-data",
					data: data,
					processData: false,
					contentType: false,
					dataType: "json",
					async: false,
					success: function (response) {

						jsonResponse = response;
						if (response != null) {
							var jobProgressToken = response.jobProgressToken;
							//console.log("jobProgressToken response-->" + jobProgressToken);
							var msgDlg = new JIRA.FormDialog({
								id: "warning-message-dialog",
								content: function (callback) {
									var innerHtmlStr = ZEPHYR.Project.Cycle.warningDialogContent({
										warningMsg: AJS.I18n.getText('zephyr-je.pdb.importer.in.progress'),
										progress: 0,
										percent: 0,
										timeTaken: 0,
										compSteps:0,
										title: AJS.I18n.getText('zephyr-je.pdb.importer.issueimport.heading')
									});
									callback(innerHtmlStr);
								},
							});
							msgDlg.show();
							var cancelImportButton = "<div class='buttons' id='cancelImport'><span class='icon throbber'></span><button class='aui-button aui-button-link cancelImport cancel'>Cancel Import</button></div>";
							AJS.$("#warning-message-dialog .jira-dialog-content .buttons-container").append(cancelImportButton);
							AJS.$('#cancelImport').on("click", function() {
								// Zephyr.Admin.Config.stopJobPregress();

								// msgDlg.hide();

								var jobProgressKey = jobProgressToken;
								if(jobProgressKey == null || jobProgressKey == ''){
									AJS.$("#cycle-aui-message-bar").text(AJS.I18n.getText('zephyr-je.pdb.importer.required.job.progress.key')).css({'color': '#ff0000'}).show().delay(5000).hide(1000);
								}else{
									jQuery.ajax({
										url: contextPath + "/rest/zephyr/latest/jobProgress/" + jobProgressKey + "?status=STOP",
										type : "put",
										complete : function(jqXHR, textStatus){
											var errorDesc;
											var jobStoppedBy;
											if (jqXHR.responseText != "") {
												errorDesc = jqXHR.responseText;
												try{
													var responseJson = jQuery.parseJSON(jqXHR.responseText);
													if (responseJson && responseJson.errorDesc)
														errorDesc = responseJson.errorDesc;
													if (responseJson && responseJson.jobStoppedBy){
														jobStoppedBy=responseJson.jobStoppedBy;
													}
												}catch(e){}
											}
											if(jqXHR.status == 200){
												//progressBarContainer.append(AJS.$('<label>' +AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.configuration.sprintCleanup.was.successful', '<strong>', '</strong>', '<strong>'+ jQuery.parseJSON(jqXHR.responseText).took + '</strong>') + '</label>'));
												var msg = AJS.I18n.getText("zephyr-je.pdb.importer.init.job.stopped.successfully")+'<br />'+AJS.I18n.getText("zephyr-je.pdb.importer.init.createdBy");
												AJS.$("#cycle-aui-message-bar").html( msg + ' : ' + jobStoppedBy).css({'color': '#ff0000'}).show();
												// AJS.$("#cycle-aui-message-bar").text("Job Stopped successfully by:" + jobStoppedBy).css({'color': '#008000'}).show().delay(5000).hide(1000);
                                                if(intervalId){Zephyr.Importer.Config.clearProgress(intervalId);}
											}else if(jqXHR.status == 404 || jqXHR.status == 400){
												AJS.$("#cycle-aui-message-bar").text(AJS.I18n.getText("zephyr-je.pdb.importer.init.complete")).css({'color': '#ff0000'}).show();
												// AJS.$("#cycle-aui-message-bar").text('Job has already been completed.').css({'color': '#ff0000'}).show().delay(5000).hide(1000);
                                                if(intervalId){Zephyr.Importer.Config.clearProgress(intervalId);}
											}
											AJS.$('#cancelImport').hide();
										}
									});
								}
							})

							// msgDlg.addSubmit("Cancel Import", Zephyr.Importer.Config.cancelImport);

							//Zephyr.Importer.Config.resetData();

							 intervalId = setInterval(function () {
								jQuery.ajax({
									url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + jobProgressToken,
									data: { 'type': "import_create_issues_job_progress" }, complete: function (jqXHR, textStatus) {
										var data = jQuery.parseJSON(jqXHR.responseText);
										var errMsg = ((data.errorMessage != undefined && data.errorMessage.length > 0) ? data.errorMessage : null);

										//job in progress
										if(data.completedSteps == 0 && data.totalSteps == 1 && data.message == AJS.I18n.getText('zephyr-je.pdb.importer.in.progress')){
                                            AJS.$(".aui-progress-indicator").remove();
                                            AJS.$(".timeTaken").remove();
                                            AJS.$(".cancelImport").remove();
											AJS.$("#cycle-aui-message-bar .aui-message").html(data.message);
                                            Zephyr.Importer.Config.clearProgress(intervalId);
										}else {
                                            AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
                                            AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
                                            AJS.$(".timeTaken").html("Time Taken: " + data.timeTaken);
                                            if (errMsg != null) {
                                                AJS.$("#cycle-aui-message-bar .aui-message").html(errMsg);
                                                Zephyr.Importer.Config.clearProgress(intervalId);
                                            }
                                        }
										if (data.completedSteps != 0 && errMsg == null) {
											// console.log(data.message);
											if (data.message != null && data.message != undefined) {
												var dataHtml = "";
												var datamessage = jQuery.parseJSON(data.message);

												if (data.jobStoppedBy != null && data.jobStoppedBy != undefined) {
													dataHtml += "<br/><b>"+datamessage+"</b>";
													dataHtml += "<br/><b>Job Stopped By:</b>" + data.jobStoppedBy;
                                                    Zephyr.Importer.Config.clearProgress(intervalId);
												}
												if (datamessage != null && datamessage != '') {
													var jobStatuses = AJS.$.parseJSON(datamessage["jobstatus"]);
													//console.log("jobStatuses-->"+jobStatuses)
													if (jobStatuses != null && jobStatuses != undefined) {
														//console.log("jobStatuses-->not null");
														AJS.$.each(jobStatuses, function (index, jobStatus) {
															AJS.$("#cycle-aui-message-bar .aui-message .compSteps").remove();
															//console.log("jobStatus-->"+JSON.stringify(jobStatus));
															//console.log("jobStatus-->"+jobStatus);
															//console.log("jobStatus['status'] -->"+jobStatus['status'] );
															if (jobStatus['status'] != null && jobStatus['status'] != undefined) {
																dataHtml += "<b>Status : </b>" + jobStatus['status'];
															}
															if (jobStatus['fileName'] != null && jobStatus['fileName'] != undefined) {
																dataHtml += "<br/><b>File : </b>" + jobStatus['fileName'];
															}
															if (jobStatus['issuesCount'] != null && jobStatus['issuesCount'] != undefined) {
																dataHtml += "<br/><b>Issues Created : </b>" + jobStatus['issuesCount'];
															}
															if (jobStatus['issuesCountForSteps'] != null && jobStatus['issuesCountForSteps'] != undefined) {
																dataHtml += "<br/><b>Issues Updated : </b>" + jobStatus['issuesCountForSteps'];
															}
															if (jobStatus['errorMsg'] != null && jobStatus['errorMsg'] != undefined) {
																dataHtml += "<br/><b>Error : </b>" + jobStatus['errorMsg'];
															}
														});
													}
													AJS.$("#cycle-aui-message-bar .aui-message").html(dataHtml);
                                                    Zephyr.Importer.Config.clearProgress(intervalId);
												}
												//selectedVersionChanged();
												//triggerTreeRefresh();
											}

                                             //    AJS.$("#cycle-aui-message-bar .aui-message").html(data.message);

                                            // }
										}
									}, error : function () {
                                        Zephyr.Importer.Config.clearProgress(intervalId);
                                    }
								})
							}, 1000);
						}
					},
					error: function (jqXHR) {
                        Zephyr.Importer.Config.clearProgress(intervalId);
						console.log("error response-->" , jqXHR);
                        if (jqXHR.responseText != "") {
                            var errorDesc = jqXHR.responseText;
                            var responseJson = jQuery.parseJSON(jqXHR.responseText);
                            var errText = responseJson.errorDesc || responseJson.error;
                            if (responseJson && errText)
                                errorDesc = errText;
                            document.getElementById("error").innerHTML = errorDesc;
                        }
					}
				});
			}

		};
		this.clearProgress = function (intervalId) {
            AJS.$(".aui-progress-indicator").attr("data-value", 1);
            AJS.$(".aui-progress-indicator-value").css("width", 100 + "%");
			clearInterval(intervalId);
        };
		this.validateMappings = function () {
			var fieldsArr = [];
			var issueFlds = JSON.parse(sessionStorage.getItem('issue_fields'));
			var issueFldsKeys = Object.keys(JSON.parse(sessionStorage.getItem('issue_fields')));
			var fileFlds = Zephyr.Importer.Config.getMappedFields();
			//var fileFieldMappings = JSON.parse(sessionStorage.getItem('file_fields_mapping'));
			AJS.$.each(issueFldsKeys, function (index, value) {
				if (AJS.$.inArray(value, issueFieldsFilter) < 0 && AJS.$.inArray(issueFlds[value].displayName, issueFieldsNamesFilter) < 0) {
					var isMandatory = issueFlds[value].mandatory;
					var mappedFieldValue = fileFlds[index];
					if ((isMandatory == true) && (mappedFieldValue == '' || mappedFieldValue == null || mappedFieldValue == undefined)) {
						fieldsArr.push(issueFlds[value].displayName);
					}

				}
				index++;
			});
			return fieldsArr;
		}

		this.isValidImportData = function () {
			var fieldsArr = Zephyr.Importer.Config.validateMappings();
			var file = AJS.$('#zephyr-importer-filepref')[0].files[0];
			if (file == undefined) {
				fieldsArr.push("file");
			}
			var fields = '';
			if (fieldsArr != null && fieldsArr != undefined && fieldsArr.length > 0) {

				AJS.$.each(fieldsArr, function (index, value) {
					if (index != fieldsArr.length - 1) fields += value + ', ';
					else {
						fields += value + '.';
					}

				});

				// var errorFields = "";
				// for(var i=0 ; i < feilds.length ; i++){
				// 	errorFields = errorFields + " " + feilds[i] ;
				// }

				document.getElementById("error").innerHTML = AJS.I18n.getText('zephyr-je.pdb.importer.required.field.mapping.missing', fields);






				// 	fields = fields.slice(0, -1);
				// 	/*var cxt = AJS.$("#general-config-aui-message-bar");
				// cxt.empty();
				// 	AJS.messages.error(cxt, {
				// 	title: AJS.I18n.getText("zephyr-je.pdb.importer.init.error"),
				// 	body: 'Following required mappings are missing <br/>'+fields,
				// 	closeable: true
				// });
				// cxt.show();*/
				// 	var dialog = new AJS.Dialog({
				// 		width: 800,
				// 		height: 270,
				// 		id: "dialog-error"
				// 	});
				// 	dialog.addHeader(AJS.I18n.getText('zephyr.common.error.label'));
				// 	dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
				// 	AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
				// 		title: AJS.I18n.getText('zephyr.je.submit.form.error.title'),
				// 		body: AJS.I18n.getText('Following required mappings are missing <br/>' + fields),
				// 		closeable: false
				// 	});
				// 	dialog.addLink("Close", function (dialog) {
				// 		dialog.hide();
				// 	}, "#");
				// 	dialog.show();
				// 	return false;

			}
			else {
				document.getElementById("error").innerHTML = "";
			}
			return true;
		}


		this.getImportData = function () {
			var importJob = {};
			var importDetails = {}
			importJob.folder = "";
			importJob.projectId = selectedProjectId;
			importJob.issueType = AJS.$('#zephyr-importer-issuetypes option:selected').val();
			importJob.fileType = AJS.$('#zephyr-importer-filetype option:selected').text();
			importJob.linkTypeId = AJS.$('#zephyr-importer-linkType').val();
			importJob.linkingIssueKey = AJS.$('#zephyr-importer-links option:selected').val()
			//var issueKey = AJS.$('#zephyr-importer-test-issues option:selected').val();
			//importJob.issueKey = issueKey;
			var issueFields = JSON.parse(sessionStorage.getItem('issue_fields'));
			/*if (issueKey != null && issueKey != '' && issueKey != undefined) {
				var testIssueFields = {};
				AJS.$.each(testStepFields, function (index, value) {
					testIssueFields[value] = (issueFields[value]);
				});
				//console.log("test issue fields keys-->"+Object.keys(testIssueFields));
				importJob.fieldConfigMap = testIssueFields;
				importDetails.fieldMappingSet = Zephyr.Importer.Config.getTestMappedMappings();
				importJob.importDetails = importDetails;

			} else {*/
			AJS.$.each(issueFieldsFilter, function (index, value) { delete issueFields[value]; });
			//console.log(Object.keys(issueFields));
			importJob.fieldConfigMap = issueFields;
			importDetails.fieldMappingSet = Zephyr.Importer.Config.getMappedMappings();
			//}
			if (importJob.fileType == 'Excel') {
				importDetails.discriminator = AJS.$('#zephyr-importer-discriminator option:selected').text() != '' ? AJS.$('#zephyr-importer-discriminator option:selected').text() : 'By Empty Row';
				importDetails.startingRowNumber = AJS.$('#txtfile2').val() != '' ? AJS.$('#txtfile2').val() : "2";
				importDetails.importAllSheetsFlag = AJS.$('#zephyr-importer-importallsheets').is(":checked") ? true : false;
				if (AJS.$('#zephyr-importer-importallsheets:checked').val()) {
					importDetails.sheetFilter = AJS.$('#fieldSheetId').val() != '' ? AJS.$('#fieldSheetId').val() : "";
				}
			}
			importJob.importDetails = importDetails;
			return importJob;
		}

		this.toShowButtons = function (condition) {
			if (condition) {
				AJS.$('#zephyr-importer-filefieldsmapping-retrieve').show();
				AJS.$("#zephyr-importer-filefieldsmapping-save").show();
			}
			else {
				AJS.$('#zephyr-importer-filefieldsmapping-retrieve').hide();
				AJS.$("#zephyr-importer-filefieldsmapping-save").hide();
			}

		}

		this.resetData = function () {

			AJS.$('#dndTbl tr td div').each(function () {
				AJS.$(this).empty();
			});
			// AJS.$('#zephyr-importer-project').empty();
			// AJS.$('#zephyr-importer-project-field').val('').trigger('change');
			// AJS.$('#zephyr-importer-issuetypes').html('<select id="zephyr-importer-issueTypesSelect" name="issueTypeSelection" type="single" class="select"></select>');
			// AJS.$('#linkscolumn').html('<select id="zephyr-importer-links" name="linksSelection" type="single" class="select"></select>');
			// AJS.$('#zephyr-importer-links-field').val('').trigger('change');
			// //AJS.$('#zephyr-importer-filetype').empty();
			// AJS.$('#filesettingrow').hide();
			// //AJS.$('#zephyr-importer-filefieldssmappingtype').empty();
			// AJS.$('#zephyr-importer-savedprefs').empty();
			// AJS.$('#zephyr-importer-filepref').val('');
			// AJS.$('#zephyr-importer-startingrow').val('');
			// AJS.$('#zephyr-importer-sheetfilterrow').val('');
			// AJS.$('#txtFile').val('');
			// AJS.$('#zephyr-importer-filefieldsmapping-retrieve').hide();
			// AJS.$('#zephyr-importer-filefieldsmapping-save').hide();
			// AJS.$('#zephyr-importer-folderpref').val('');
			// AJS.$('#mappingContainersRow ').empty();

			//Zephyr.Importer.Config.populateProjects();
		}

		var showError = function (jqXHR, textStatus) {
			if (jqXHR.responseText != "") {
				var cxt = AJS.$("#general-config-aui-message-bar");
				cxt.empty();

				var errorDesc = jqXHR.responseText;
				var responseJson = jQuery.parseJSON(jqXHR.responseText);
				if (responseJson && responseJson.errorDesc)
					errorDesc = responseJson.errorDesc;

				AJS.messages.error(cxt, {
					title: AJS.I18n.getText("zephyr-je.pdb.importer.init.error"),
					body: errorDesc,
					closeable: true
				});
			}
		};
	}

	return { Config: configClass }



})();

AJS.$(document).ready(function () {


	AJS.$('#customDrop').on('click', function (event) {
		AJS.$('#zephyr-importer-linkType').trigger('change');
	});

	AJS.$('#zephyr-importer-project').live('click', function (event) {
		var searchInput = AJS.$(event.target);
		searchInput.addClass('active');
	});

	AJS.$('#zephyr-importer-links').live('click', function (event) {
		var searchInput = AJS.$(event.target);
		searchInput.addClass('active');
	});

	AJS.$("#zephyr-importer-filetype").change(function () {
		var selectedFileType = AJS.$('#zephyr-importer-filetype option:selected').text();
		Zephyr.Importer.Config.populateSavedMappingPreferences();
		if (selectedFileType == 'XML') {
			AJS.$("#fileTypeId").text(AJS.I18n.getText("zephyr-je.pdb.importer.file.fields.xml"));
			AJS.$("#filetypeprefrow").hide();
			AJS.$("#helpMsgForExcel").hide();
			AJS.$("#zephyr-importer-filepref").attr("accept", ".xml");
			AJS.$("#zephyr-importer-folderpref").attr("accept", ".xml");
		} else if (selectedFileType == 'Excel') {
			AJS.$("#fileTypeId").text(AJS.I18n.getText("zephyr-je.pdb.importer.file.fields.excel"));
			AJS.$("#filetypeprefrow").show();
			AJS.$("#helpMsgForExcel").show();
			AJS.$("#zephyr-importer-filepref").attr("accept", ".xls,.xlsx");
			AJS.$("#zephyr-importer-folderpref").attr("accept", ".xls,.xlsx");
		}

		if(fileTypeSelected != selectedFileType){
			AJS.$('#txtFile').val('');
			AJS.$('#zephyr-importer-filefieldsmapping-retrieve').hide();
			AJS.$('#zephyr-importer-filefieldsmapping-save').hide();
			Zephyr.Importer.Config.resetData();
			AJS.$('#fieldsDiv').html('');
		}

	});

	// AJS.$("#zephyr-importer-filetype").change(function () {
	// 	var selectedFileType = AJS.$('#zephyr-importer-filetype option:selected').text();
	// 	if(selectedFileType == 'XML'){

	// 	} else {
	// 		AJS.$("#helpMsgForExcel").show();
	// 	}
	// });

	AJS.$("#zephyr-importer-filefieldssmappingtype").change(function () {
		var selectedFileFieldsMappingType = AJS.$('#zephyr-importer-filefieldssmappingtype option:selected').text();
		if (selectedFileFieldsMappingType == 'Saved Preferences') {
			Zephyr.Importer.Config.populateSavedMappingPreferences();
			Zephyr.Importer.Config.populateDNDContainer();
			//Zephyr.Importer.Config.populateFileFields();
			AJS.$("#savedPrefRow").show();
			if (AJS.$('#zephyr-importer-filepref')[0] != undefined) {
				var file = AJS.$('#zephyr-importer-filepref')[0].files[0];
				if (file != undefined && AJS.$('#zephyr-importer-project option').length > 0 && document.getElementById('errorForDropDown2').innerHTML == "") {
					//AJS.$('#zephyr-importer-filefieldsmapping-retrieve').show();
					//Zephyr.Importer.Config.fields.projectNameSelected = true;
				}
			}
			document.getElementById("zephyr-importer-filefieldsmapping-save").style.visibility = "hidden";
			AJS.$('#zephyr-importer-folderfieldsmapping-retrieve').hide();
			AJS.$('#zephyr-importer-folderfieldsmapping-save').hide();

		} else if (selectedFileFieldsMappingType == 'Select File') {
			AJS.$("#savedPrefRow").hide();
			AJS.$('#fieldsDiv').html('');
			AJS.$('#dndTbl').html('');
			Zephyr.Importer.Config.populateDNDContainer();
			Zephyr.Importer.Config.initDNDContainers();
			// Zephyr.Importer.Config.refreshFileFields();

			if (AJS.$('#zephyr-importer-filepref')[0] != undefined) {
				var file = AJS.$('#zephyr-importer-filepref')[0].files[0];
				if (file != undefined && AJS.$('#zephyr-importer-project option').length > 0 && document.getElementById('errorForDropDown2').innerHTML == "") {
					AJS.$('#zephyr-importer-filefieldsmapping-retrieve').show();
					document.getElementById("zephyr-importer-filefieldsmapping-save").style.visibility = "visible";

				}
			}
			if (AJS.$('#zephyr-importer-folderpref')[0] != undefined) {
				var folder = AJS.$('#zephyr-importer-folderpref')[0].files[0];
				if (folder != undefined) {
					AJS.$('#zephyr-importer-folderfieldsmapping-retrieve').show();
					AJS.$('#zephyr-importer-folderfieldsmapping-save').show();
				}
			}
			Zephyr.Importer.Config.populateFileFields();
		}
	});
	AJS.$('#zephyr-importer-issuetypes').change(function () {
		var issueType = AJS.$('#zephyr-importer-issuetypes option:selected').val();
		if (issueType != '' && issueType != undefined) {
			Zephyr.Importer.Config.populateIssueTypeFieldsContainter();
			//Zephyr.Importer.Config.populateLinkIssueTypes();
			//Zephyr.Importer.Config.populateTestIssuesByProject();
			Zephyr.Importer.Config.initDNDContainers();
			Zephyr.Importer.Config.populateFileFields();
			Zephyr.Importer.Config.refreshFileFields();
			Zephyr.Importer.Config.enableSubmitButtons();
		}

	});

	AJS.$("#zephyr-importer-linkType").change(function () {
		AJS.$('#linkscolumn').html('<select id="zephyr-importer-links" name="linksSelection" type="single" class="select"></select> <br/><span id="selectedLinksSpan"></span>');
		Zephyr.Importer.Config.populateLinkingIssues();
	});

	AJS.$("#zephyr-importer-savedprefs").change(function () {
		Zephyr.Importer.Config.populateDNDContainersFromSavedPreference();
		Zephyr.Importer.Config.enablePrefButtons();
	});

	AJS.$("#zephyr-importer-discriminator").change(function () {
		Zephyr.Importer.Config.populateIssueTypeFieldsContainter();

	});


	AJS.$("#zephyr-importer-filefieldsmapping-retrieve").on('click', function () {
		Zephyr.Importer.Config.populateFileFields();
	});

	AJS.$("#zephyr-importer-folderfieldsmapping-retrieve").on('click', function () {
		Zephyr.Importer.Config.populateFolderFields();
	});

	AJS.$("#zephyr-importer-filefieldsmapping-save").on('click', function () {
		Zephyr.Importer.Config.saveMappingPreference();
	});

	AJS.$("#zephyr-importer-folderfieldsmapping-save").on('click', function () {
		Zephyr.Importer.Config.saveMappingPreference();
	});

	AJS.$("#zephyr-importer-savedprefs-update").on('click', function () {
		Zephyr.Importer.Config.updateMappingPreference();
	});
	AJS.$("#zephyr-importer-savedprefs-delete").on('click', function () {
		Zephyr.Importer.Config.deleteMappingPreference();
	});
	AJS.$("#zephyr-importer-importjob").on('click', function () {
		Zephyr.Importer.Config.processImport();
	});

	AJS.$("#zephyr-importer-reset").on('click', function () {
		Zephyr.Importer.Config.resetData();
	});

	AJS.$("#zephyr-importer-filepref").on('change', function () {
		var file = AJS.$('#zephyr-importer-filepref')[0].files[0];
		if (file != undefined) {

			var isIssueType = false;
			AJS.$('#issueTypes-dd option').each(function () {
				if (AJS.$(this).attr("selected") == "selected") {
					isIssueType = true;
				}
			});

			Zephyr.Importer.Config.toShowButtons((isIssueType) && (AJS.$('#zephyr-importer-project option').length > 0))


			AJS.$('#txtFile').val(file.name);
		} else {
			AJS.$('#txtFile').val('');
		}
		var selectedFileFieldsMappingType = AJS.$('#zephyr-importer-filefieldssmappingtype option:selected').text();
		if (file != undefined && selectedFileFieldsMappingType != 'Saved Preferences' && AJS.$('#zephyr-importer-project option').length > 0 && document.getElementById('errorForDropDown2').innerHTML == "") {

			// AJS.$('#zephyr-importer-filefieldsmapping-retrieve').show();
			// AJS.$('#zephyr-importer-filefieldsmapping-save').show();
			Zephyr.Importer.Config.populateDNDContainer();
			AJS.$('#fieldsDiv').html('');
			Zephyr.Importer.Config.initDNDContainers();
			Zephyr.Importer.Config.refreshFileFields();
		} else if (file != undefined && AJS.$('#zephyr-importer-project option').length > 0 && document.getElementById('errorForDropDown2').innerHTML == "") {
			AJS.$('#zephyr-importer-filefieldsmapping-retrieve').show();
			AJS.$('#zephyr-importer-filefieldsmapping-save').hide();
		} else {
			AJS.$('#zephyr-importer-filefieldsmapping-retrieve').hide();
			AJS.$('#zephyr-importer-filefieldsmapping-save').hide();
		}
	});

	AJS.$("#zephyr-importer-folderpref").on('change', function () {
		var file = AJS.$('#zephyr-importer-folderpref')[0].files[0];
		if (file != undefined) {
			var path = AJS.$('#zephyr-importer-folderpref')[0].files[0].webkitRelativePath;
			var folder = path.split("/");
			AJS.$('#txtFolder').val(folder[0]);
		} else {
			AJS.$('#txtFolder').val('');
		}
		var selectedFileFieldsMappingType = AJS.$('#zephyr-importer-filefieldssmappingtype option:selected').text();
		if (file != undefined && selectedFileFieldsMappingType != 'Saved Preferences') {
			AJS.$('#zephyr-importer-folderfieldsmapping-retrieve').show();
			AJS.$('#zephyr-importer-folderfieldsmapping-save').show();
			Zephyr.Importer.Config.populateDNDContainer();
			AJS.$('#fieldsDiv').html('');
			Zephyr.Importer.Config.initDNDContainers();
			Zephyr.Importer.Config.refreshFileFields();
		} else {
			AJS.$('#zephyr-importer-folderfieldsmapping-retrieve').hide();
			AJS.$('#zephyr-importer-folderfieldsmapping-save').hide();
		}

	});

	AJS.$('#testissueimportform').on('submit', function (e) {
		e.preventDefault();

	});

	// AJS.$("#zephyr-importer-project").on('blur', function () {
	// 	if(AJS.$('#zephyr-importer-project').children().length == 0 ){
	// 		document.getElementById("errorForDropDown").innerText = "Please Select one item";
	// 	}
	// 	else{
	// 		document.getElementById("errorForDropDown").innerText = "";
	// 	}

	// });

	Zephyr.Importer.Config.init();

});
