/**
 * @namespace ZEPHYR.ZQL.MANAGE
 */
AJS.$.namespace("ZEPHYR.ZQL.MANAGE");
ZEPHYR.ZQL.MANAGE.LOGGED_IN_USER;
ZEPHYR.ZQL.MANAGE.ZQLFilters = Backbone.Model.extend();
ZEPHYR.ZQL.MANAGE.ZQLFiltersCollection = Backbone.Collection.extend({
    model:ZEPHYR.ZQL.MANAGE.ZQLFilters,
    parse: function(resp, xhr){
    	return resp
    }
});

window.onbeforeunload = function (e) {
    return ;
}

ZEPHYR.ZQL.MANAGE.ZQLFiltersListView = Backbone.View.extend({
	tagName: 'div',
    id:'zql-mng-filters-content-id',
    events:{
    	"click [id^='mzf-tools-dropdown-']"	: "showFilterDropdown",
    	"click [id^='mzf-filter-tooglefav-id-']" : "toogleZqlFav",
    	"click [id^='executionFilterQuery']" : "executionFilterZQLQuery",
        "click [id^='numberOfExecutionFilterQuery']" : "executionFilterZQLQuery"
    },
    initialize:function () {
        this.model.bind("reset", this.render, this, this.showFilterDropdown, this, this.showZQLSaveMsg, this,
        		this.toogleZqlFav, this, this.removeZql, this, this.updateView, this);
        if(this.model) {
            this.model.on('change',this.updateView,this);
           }
    },
    render:function (eventName) {
    	// html template is ZEPHYR.MANAGE.FILTERS.showZQLFilter
    	AJS.$(this.el).html(ZEPHYR.MANAGE.FILTERS.showZQLFilters({tabTitle:this.tabTitle, tabLongDesc:this.tabLongDesc, tabId:this.tabId, loggedInUser:ZEPHYR.ZQL.MANAGE.LOGGED_IN_USER, filters:this.model.toJSON()}));
        return this;
    },
    updateView:function (eventName) {
    	AJS.$(this.el).html("");
    	AJS.$(this.el).append(ZEPHYR.MANAGE.FILTERS.showZQLFilters({tabTitle:this.tabTitle, tabLongDesc:this.tabLongDesc, tabId:this.tabId, loggedInUser:ZEPHYR.ZQL.MANAGE.LOGGED_IN_USER, filters:this.model.toJSON()}));
        var instance = this;
    	return this;
    },
    showFilterDropdown : function(e) {
    	var dropDown = new AJS.Dropdown({
            trigger: AJS.$(e.currentTarget),
            content: ZEPHYR.MANAGE.FILTERS.showFilterDropDown({ filterId:AJS.$(e.currentTarget).data("id"),
                                                                filterName:AJS.$(e.currentTarget).data("fname"),
            	                                                filterDesc:AJS.$(e.currentTarget).data("fdesc"),
                                                                isFavorite:AJS.$(e.currentTarget).data("isfav"),
                                                                sharePerm:AJS.$(e.currentTarget).data("fperm")
                    })
        });
    	dropDown.show();
    	AJS.$(dropDown).bind('hideLayer', function(){ AJS.$(e.currentTarget).next('.ajs-layer-placeholder').remove() })
    	e.preventDefault();
    },
	showZQLSaveMsg : function(response){
		var cxt = AJS.$("#mzf-message-bar");
		cxt.empty();
		var title="";
		if(response && response.status && response.status != 200){
			title = "Error:";
			var msg = jQuery.parseJSON(response.responseText);
			var errorMessage = "";
			for(var propertyName in msg) {
				errorMessage += msg[propertyName];
				errorMessage += "\n";
			}
			AJS.messages.error(cxt, {
				title: title,
			    body: errorMessage,
			    closeable: true
			});
		} else {
			AJS.messages.success(cxt, {
				title: title,
			    body: response.success,
			    closeable: true
			});
		}
		setTimeout(function(){
			AJS.$(".aui-message").fadeOut(1000, function(){
				AJS.$(".aui-message").remove();
			});
		}, 2000);
	},
	toogleZqlFav : function (e){
		e.preventDefault();
		var filterId = AJS.$(e.currentTarget).data("id");
		var isFav = AJS.$(e.currentTarget).data("isfav");
		var tabId = this.tabId;

		//toggle isFav status
		if(isFav == false)
			isFav = true;
		else
			isFav = false;

		jQuery.ajax({
			url: getRestURL() + "/zql/executionFilter/toggleFav",
			type : "PUT",
			accept : "PUT",
			contentType :"application/json",
			dataType: "json",
			data: JSON.stringify({
				  'isFavorite' : isFav,
				  'id' : filterId
			}),
			success : function(response) {
				if(isFav == false)
					parent.zqlManageFiltersView.showZQLSaveMsg(response);
				// Update the view with latest attributes.
				parent.zqlManageFiltersView.hideFromFiltersModel(filterId, isFav, tabId);
			},
			error : function(response, msg, errorType) {
				if(isFav == false)
					parent.zqlManageFiltersView.showZQLSaveMsg(response);
				// Update the view with latest attributes.
				parent.zqlManageFiltersView.hideFromFiltersModel(filterId, isFav, tabId);
			}
		});
	},
	removeZql : function (filterId, filterName){
		 var dialog = new JIRA.FormDialog({
	        id: "execution-filter-delete-dialog",
	        content: function (callback) {
	        	var innerHtmlStr = ZEPHYR.ZQLFilter.Save.deleteZQLFilterConfirmationDialog({filterName: filterName});
	            callback(innerHtmlStr);
	        },
	        submitHandler: function (e) {
	        	e.preventDefault();
	        	jQuery.ajax({
	    			url: getRestURL() + "/zql/executionFilter/"+filterId,
	    			type : "DELETE",
	    			accept : "DELETE",
	    			contentType :"application/json",
	    			dataType: "json",
	    			success : function(response) {
	    				parent.zqlManageFiltersView.showZQLSaveMsg(response);
	    				// Update the view with latest attributes.
	    				parent.zqlManageFiltersView.deleteFromFiltersModel(filterId);
	    			},
	    			error : function(response) {
	    				parent.zqlManageFiltersView.showZQLSaveMsg(response);
	    				// Update the view with latest attributes.
	    				parent.zqlManageFiltersView.deleteFromFiltersModel(filterId);
	    			}
	    		});
	        	dialog.hide();
	        }
	    });
	    dialog.show();
	},
	hideFromFiltersModel : function(filterId, isFav, tabId){
		// Updates the filter collection after "favorite" OR "unfavorite" event and update the view accordingly.
		var modelData = this.model.models;
		for(var indx = 0; indx < modelData.length; indx++){
			var modelToBeUpdated = modelData[indx];
			var popularityCount = modelToBeUpdated.get('popularity');
			if(modelToBeUpdated && modelToBeUpdated.get("id") === filterId){
				// check if it's marking ZQL filter unfavorite.
				// If isFav == false, remove the filter from model
				// else keep the filter in the model and change the favorite color
				if(isFav == false && tabId == AJS.I18n.getText('common.favourites.favourite')){
					//remove from model
					modelData.splice(indx,1);
					// fire model onchange event
					modelToBeUpdated.clear();
					window.filtersPaginationView.updateTotalCount();
				}
				else if(isFav == true)
					modelToBeUpdated.set({'isFavorite': isFav, 'popularity': ++popularityCount});
				else
					modelToBeUpdated.set({'isFavorite': isFav, 'popularity': --popularityCount});
				break;
			}
		}
	},
	deleteFromFiltersModel : function(filterId){
		// Updates the filter collection after "remove" event and update the view accordingly.
		var modelData = this.model.models;
		for(var indx = 0; indx < modelData.length; indx++){
			var modelToBeUpdated = modelData[indx];
			if(modelToBeUpdated && modelToBeUpdated.get("id") === filterId){
				//remove from model
				modelData.splice(indx,1);
				// fire model onchange event
				modelToBeUpdated.clear();
				window.filtersPaginationView.updateTotalCount();
				break;
			}
		}
	},
	executionFilterZQLQuery : function (e) {
		var filterId = AJS.$(e.currentTarget).attr('data-filterId');
		e.currentTarget.href =  "enav/#?filter=" + filterId;
	}
});

ZEPHYR.ZQL.MANAGE.ZQLFiltersSearchView = Backbone.View.extend({
	tagName: 'div',
    id:'zql-mng-filters-search-content-id',
    events : {
    	"click [id^='mzf-search-btn']"	: "searchForFilters",
    	"click [id^='executionFilterQuerySearch']" : "executionFilterZQLQuerySearch"
    },
    initialize:function () {
        //this.model.bind("reset", this.render, this);
    	 _.bindAll(this, 'render');
    },
    render:function (eventName) {
    	// html template is ZEPHYR.MANAGE.FILTERS.searchZQLFilters
    	AJS.$(this.el).html("");
    	AJS.$(this.el).append(ZEPHYR.MANAGE.FILTERS.searchZQLFilters());
        var instance = this;
    	return this;
    },
    searchForFilters : function(e) {
    	var filterName = AJS.$("#filterName").val();
    	var owner = AJS.$("#searchOwnerUserName :selected").val();
    	var sharePerm = AJS.$("#mzf-share_type_selector").val();
    	var srhUrl = getRestURL() + "/zql/executionFilter/search?";
    	if(filterName)
    		srhUrl = srhUrl+"filterName="+filterName+"&";
    	if(owner)
    		srhUrl = srhUrl+"owner="+owner+"&";
    	if(sharePerm)
    		srhUrl = srhUrl+"sharePerm="+sharePerm;
    	AJS.$('#mng-srh-filters-content').html("");
    	ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults = new ZEPHYR.ZQL.MANAGE.ZQLFiltersCollection()
    	ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.url = function(){
        	return srhUrl;
        }
    	ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.fetch({contentType:'application/json',
    		success:function(response,jqXHR){
    		if(ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models != null &&
        			ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models.length > 0 &&
        			ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models[0].attributes != null){
    			if(window.zqlManageSearchContentView == null) {
    				window.zqlManageSearchContentView = new ZEPHYR.ZQL.MANAGE.ZQLFiltersSearchContentView({model:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults});
    	    		AJS.$('#mng-srh-filters-content').append(window.zqlManageSearchContentView.render().el);
    			} else {
    				window.zqlManageSearchContentView.remove();
    				window.zqlManageSearchContentView = new ZEPHYR.ZQL.MANAGE.ZQLFiltersSearchContentView({model:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults});
    				AJS.$('#mng-srh-filters-content').append(window.zqlManageSearchContentView.render().el);
    			}
    		} else{
    			//Show message
    	    	var cxt = AJS.$("#mzf-srh-message-bar");
    			cxt.empty();
    			var message=AJS.I18n.getText('filters.no.search.results');
    			var title="";
    			AJS.messages.info(cxt, {
    				title: title,
    				body: message,
    				closeable: true
    			});
    			setTimeout(function(){
    				AJS.$(".aui-message").fadeOut(1000, function(){
    					AJS.$(".aui-message").remove();
    				});
    			}, 2000);
    		}
    	}, error: function(response,jqXHR) {

        }})
    	e.preventDefault();
    },
	executionFilterZQLQuerySearch : function (e) {
		var filterId = AJS.$(e.currentTarget).attr('data-filterId');
		e.currentTarget.href =  "enav/#?filter=" + filterId;
	}
});

ZEPHYR.ZQL.MANAGE.FiltersPaginationView = Backbone.View.extend({
    tagName:'div',
    events:{
    	"click [id^='mzf-pagination-']" : 'executePaginatedFetch',
    	"click [id^='refreshZQLId']"	: "refreshFiltersView"
    },
    initialize:function () {
        this.model.bind("reset", this.render, this, this.executePaginatedFetch, this);
        if(this.model) {
            this.model.on('change',this.updateView,this);
           }
    },
    render:function (eventName) {
    	if(ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models != null &&
    			ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models.length > 0 &&
    			ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models[0].attributes != null) {
        	AJS.$(this.el).html("");
    		AJS.$(this.el).append(ZEPHYR.MANAGE.FILTERS.addFiltersPaginationFooter({
	    		totalCount:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models[0].attributes.totalCount,
	    		currentIndex:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models[0].attributes.currentIndex,
	    		maxAllowed:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models[0].attributes.maxResultAllowed,
	    		linksNew:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models[0].attributes.linksNew}));
    		return this;
    	} else {
    		AJS.$(this.el).append();
    		return this;
    	}
    },
    executePaginatedFetch : function(event) {
    	event.preventDefault();
    	var offset = eval(event.target.attributes['page-id'].value);
		ManageZQLFilters.findFilters(window.zqlManageFiltersView.tabTitle, window.zqlManageFiltersView.tabLongDesc, window.zqlManageFiltersView.tabId, window.zqlManageFiltersView.url, offset);
    }
    ,updateTotalCount : function(){
		// Update the view with latest attributes.
		var modelData = parent.zqlManageFiltersView.model.models;
		for(var indx = 0; indx < modelData.length; indx++){
			var modelToBeUpdated = modelData[indx];
			var prevTotalCount = modelToBeUpdated.attributes.totalCount;
			modelToBeUpdated.set({'totalCount': --prevTotalCount});
		}
    },
    updateView:function (eventName) {
    	AJS.$(this.el).html("");
		AJS.$(this.el).append(ZEPHYR.MANAGE.FILTERS.addFiltersPaginationFooter({
    		totalCount:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models[0].attributes.totalCount,
    		currentIndex:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models[0].attributes.currentIndex,
    		maxAllowed:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models[0].attributes.maxResultAllowed,
    		linksNew:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.models[0].attributes.linksNew}));
        var instance = this;
    	return this;
    },
    refreshFiltersView: function(ev) {
    	ev.preventDefault();
    	ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.fetch({reset: true});
    }
});

ZEPHYR.ZQL.MANAGE.ZQLFiltersSearchContentView = Backbone.View.extend({
	tagName: 'div',
    id:'mng-srh-filters-content-id',
    events:{
    	"click [id^='mzf-srh-filter-tooglefav-id-']" : 'toogleZqlFav'
    },
    initialize:function () {
        this.model.bind("reset", this.render, this);
        if(this.model) {
            this.model.on('change',this.updateView,this);
           }
    },
    render:function (eventName) {
    	// html template is ZEPHYR.MANAGE.FILTERS.zqlSearchContent
    	AJS.$(this.el).html(ZEPHYR.MANAGE.FILTERS.zqlSearchContent({filters:this.model.toJSON()}));
        var instance = this;
    	return this;
    },
	toogleZqlFav : function (e){
		e.preventDefault();
		var filterId = AJS.$(e.currentTarget).data("id");
		var isFav = AJS.$(e.currentTarget).data("isfav");
		//toggle isFav status
		if(isFav == false)
			isFav = true;
		else
			isFav = false;

		jQuery.ajax({
			url: getRestURL() + "/zql/executionFilter/toggleFav",
			type : "PUT",
			accept : "PUT",
			contentType :"application/json",
			dataType: "json",
			data: JSON.stringify({
				  'isFavorite' : isFav,
				  'id' : filterId
			}),
			success : function(response) {
				// Update the view with latest attributes.
				parent.zqlManageSearchContentView.hideFromFiltersModel(filterId, isFav);
			}
		});
	},
	hideFromFiltersModel : function(filterId, isFav, tabId){
		// Updates the filter collection after "favorite" OR "unfavorite" event and update the view accordingly.
		var modelData = this.model.models;
		for(var indx = 0; indx < modelData.length; indx++){
			var modelToBeUpdated = modelData[indx];
			var popularityCount = modelToBeUpdated.get('popularity');
			if(modelToBeUpdated && modelToBeUpdated.get("id") === filterId){
				//modelToBeUpdated.set({'isFavorite': isFav, 'popularity': ++popularityCount});
				if(isFav == true)
					modelToBeUpdated.set({'isFavorite': isFav, 'popularity': ++popularityCount});
				else
					modelToBeUpdated.set({'isFavorite': isFav, 'popularity': --popularityCount});
				break;
			}
		}
	},
    updateView:function (eventName) {
    	AJS.$(this.el).html("");
    	AJS.$(this.el).append(ZEPHYR.MANAGE.FILTERS.zqlSearchContent({filters:this.model.toJSON()}));
        var instance = this;
    	return this;
    }
});

var ManageZQLFilters = new function(){
	this.findFilters = function(title, tabLongDesc, tabId, filterUrl, startIndx){
		AJS.$('#zql-mng-filters-content-id').html("");
		ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults = new ZEPHYR.ZQL.MANAGE.ZQLFiltersCollection()
		ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.url = function(){
	    	return getRestURL() + filterUrl + startIndx;
	    }
		ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults.fetch({contentType:'application/json',success:function(tid){return function(){
			var activeTab = AJS.$(".active-tab a").attr('title');
            if(activeTab != tid)
                return;
			if(window.zqlManageFiltersView == null) {
    			window.zqlManageFiltersView = new ZEPHYR.ZQL.MANAGE.ZQLFiltersListView({model:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults});
    			window.zqlManageFiltersView.tabTitle = title;
    			window.zqlManageFiltersView.url = filterUrl;
    			window.zqlManageFiltersView.tabId = tabId;
    			window.zqlManageFiltersView.tabLongDesc = tabLongDesc;
        		AJS.$('#zql-mng-filters-content').append(window.zqlManageFiltersView.render().el);
        		if(null == window.filtersPaginationView)
        			window.filtersPaginationView = new ZEPHYR.ZQL.MANAGE.FiltersPaginationView({model:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults});
        		AJS.$('#zql-mng-filters-content').append(window.filtersPaginationView.render().el);
    		} else {
    			window.zqlManageFiltersView.remove();
    			window.filtersPaginationView.remove();
    			window.zqlManageFiltersView = new ZEPHYR.ZQL.MANAGE.ZQLFiltersListView({model:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults});
    			window.zqlManageFiltersView.tabTitle = title;
    			window.zqlManageFiltersView.url = filterUrl;
    			window.zqlManageFiltersView.tabId = tabId;
    			window.zqlManageFiltersView.tabLongDesc = tabLongDesc;
    			AJS.$('#zql-mng-filters-content').append(window.zqlManageFiltersView.render().el);
           		window.filtersPaginationView = new ZEPHYR.ZQL.MANAGE.FiltersPaginationView({model:ZEPHYR.ZQL.MANAGE.ZQLFilters.data.searchResults});
    			AJS.$('#zql-mng-filters-content').append(window.filtersPaginationView.render().el);
    		}
    	}}(tabId), error: function(response,jqXHR) {
    		showZQLError(jqXHR);
        }})
	},
	this.searchFilters = function(){
		AJS.$('#zql-mng-filters-content-id').html("");
			if(window.zqlManageFiltersSearchView == null) {
    			window.zqlManageFiltersSearchView = new ZEPHYR.ZQL.MANAGE.ZQLFiltersSearchView();
    			AJS.$('#zql-mng-filters-content').append(window.zqlManageFiltersSearchView.render().el);
    		} else {
    			window.zqlManageFiltersSearchView.remove();
    			window.zqlManageFiltersSearchView = new ZEPHYR.ZQL.MANAGE.ZQLFiltersSearchView();
    			AJS.$('#zql-mng-filters-content').append(window.zqlManageFiltersSearchView.render().el);
    		}
			this.createUserPicker();
    },
	this.createUserPicker = function(){
		AJS.$(document.body).find('.aui-field-filterpicker').each(function () {
	        new AJS.SingleSelect({
		           element: AJS.$("#searchOwnerUserName"),
	           itemAttrDisplayed: "label",
	           maxInlineResultsDisplayed: 15,
	           maxWidth:200,
	           showDropdownButton: false,
	           submitInputVal: true,
	           overlabel: AJS.I18n.getText("user.picker.ajax.short.desc"),
               errorMessage: AJS.I18n.getText("admin.errors.invalid.user"),
	           ajaxOptions: {
	        	   url:contextPath + "/rest/api/1.0/users/picker",
	        	   query:true,
	        	   minQueryLength: 2,
	        	   formatResponse: function (response) {
	        		   var ret = [];
	        		   AJS.$(response).each(function(i, category) {
	    	                var groupDescriptor = new AJS.GroupDescriptor();

	    	                AJS.$(category.users).each(function(){
	    	                    groupDescriptor.addItem(new AJS.ItemDescriptor({
	    	                        value: this.name, // value of
	    	                        label: this.displayName, // title
	    	                        html: this.html,
	    	                        highlighted: true
	    	                    }));
	    	                });
	    	                ret.push(groupDescriptor);
	    	            });
	    	            return ret;
	        	   }
	           }
	        });
		});
	}
}

//handles edit ZQL filter event
AJS.$("#[id^='mzf-filter-edit-id-']").live("click", function(e) {
	e.preventDefault();
	var filterName = AJS.$(e.currentTarget).data("fname");
	var filterId = AJS.$(e.currentTarget).data("id");
	var filterDesc;
	var isFav;
	var sharePerm;
	var dialog = new JIRA.FormDialog({
        id: "edit-zql-filter-id",
        content: function (callback) {
        	/*Short cut of creating view, move it to Backbone View and do it in render() */
			var htmlTemplate = AJS.$(ZEPHYR.ZQLFilter.Save.saveZQLFilter({method:"PUT", fName:AJS.$(e.currentTarget).data("fname"), fDesc:AJS.$(e.currentTarget).data("fdesc"),
				isFav:AJS.$(e.currentTarget).data("isfav"), sharePerm:AJS.$(e.currentTarget).data("fperm"), dialogTitle: AJS.I18n.getText("zql.filter.edit.dialog.title")}));
            callback(htmlTemplate);
        },
        submitHandler: function (e) {
            e.preventDefault();
        	filterName = e.currentTarget.filterName.value;
        	filterDesc = e.currentTarget.filterDescription.value;
        	isFav = AJS.$('#isFavFilter').is(':checked');
        	sharePerm = AJS.$('input[name=sharePerm]:radio:checked').val();
    		jQuery.ajax({
    			url: getRestURL() + "/zql/executionFilter/update",
    			type : "PUT",
    			contentType :"application/json",
    			dataType: "json",
    			data: JSON.stringify({
    				  'id' : parseInt(filterId),
    				  'filterName': filterName,
    				  'description': filterDesc,
    				  'isFavorite': isFav,
    				  'sharePerm' : sharePerm
    			}),
    			success : function(response) {
                    // Update the view with latest attributes.
                    var modelData = parent.zqlManageFiltersView.model.models;
                    for(var indx = 0; indx < modelData.length; indx++){
                        var modelToBeUpdated = modelData[indx];
                        if(modelToBeUpdated && modelToBeUpdated.get("id") === filterId){
                            // if filter is marked as unfavorite while updating, remove it from view
                            if(isFav == false && parent.zqlManageFiltersView.tabId == AJS.I18n.getText('common.favourites.favourite')){
                                //remove from model
                                modelData.splice(indx,1);
                                // fire model onchange event
                                modelToBeUpdated.clear();
                            }
                            else
                                modelToBeUpdated.set({'filterName': filterName, 'description': filterDesc, 'isFavorite': isFav, 'sharePerm': sharePerm });
                            break;
                        }
                    }
                    // show success message
                    parent.zqlManageFiltersView.showZQLSaveMsg(response);
                },
    			error : function(response) {
    				parent.zqlManageFiltersView.showZQLSaveMsg(response);
    			}
    		});

    		 dialog.hide();
        }
    });
    dialog.show();
});

//handles remove ZQL filter event
AJS.$("#[id^='mzf-filter-delete-id-']").live("click", function(e) {
	e.preventDefault();
	var filterId = AJS.$(e.currentTarget).data("id");
	var filterName = AJS.$(e.currentTarget).data('fname');
	parent.zqlManageFiltersView.removeZql(filterId, filterName);
});

//Handles onclick tab link event
AJS.$("a#fav-filters-tab, a#my-filters-tab, a#popular-filters-tab, a#search-filters-tab").live("click", function(e) {
	e.preventDefault();
	ZEPHYR.ZQL.manageZQLRouter.navigate("?tab="+AJS.$(".active-tab a").attr('title'), {trigger:true});
});

var ManageFiltersViewController = new function(){
	this.disposeAllView = function(){
		if(window.zqlManageFiltersView != null)
			window.zqlManageFiltersView.close();
		if(window.zqlManageFiltersSearchView != null)
			window.zqlManageFiltersSearchView.close();
		if(window.filtersPaginationView != null)
			window.filtersPaginationView.close();
		if(window.zqlManageSearchContentView != null)
			window.zqlManageSearchContentView.close();
	}
}

var ManageZQLRouter = Backbone.Router.extend({
	routes: {
		"?tab=*tab" : "filtersTab",
		"" : "favTab"
	},
	filtersTab : function(tab){
		// In language locales like 'Russian' the tab values are encoded.
		tab = decodeURIComponent(tab);
		AJS.$('li.menu-item, .active-tab').removeClass('active-tab');
		if(tab && tab == AJS.I18n.getText('common.favourites.favourite')){
			AJS.$('#mzf_fav_li').addClass("active-tab");
			ManageFiltersViewController.disposeAllView();
			ManageZQLFilters.findFilters(AJS.I18n.getText('zql.filter.manage.favorite.desc'), AJS.I18n.getText('zql.filter.manage.favorite.long.desc'), AJS.I18n.getText('common.favourites.favourite'), "/zql/executionFilter/?byUser=true&fav=true&offset=",0);
		}
		if (tab && tab == AJS.I18n.getText('managefilters.my')){
			AJS.$('#mzf_my_li').addClass("active-tab");
			ManageFiltersViewController.disposeAllView();
			ManageZQLFilters.findFilters(AJS.I18n.getText('zql.filter.manage.my.desc'), AJS.I18n.getText('zql.filter.manage.my.long.desc'), AJS.I18n.getText('managefilters.my'), "/zql/executionFilter/?byUser=true&offset=",0);
		}
		if (tab && tab == AJS.I18n.getText('common.concepts.popular')){
			AJS.$('#mzf_pop_li').addClass("active-tab");
			ManageFiltersViewController.disposeAllView();
			ManageZQLFilters.findFilters(AJS.I18n.getText('zql.filter.manage.popular.desc'), AJS.I18n.getText('zql.filter.manage.popular.long.desc'), AJS.I18n.getText('common.concepts.popular'), "/zql/executionFilter/?fav=true&offset=",0);
		}
		if( tab && tab == AJS.I18n.getText('common.concepts.search')){
			AJS.$('#mzf_search_li').addClass("active-tab");
			ManageFiltersViewController.disposeAllView();
			ManageZQLFilters.searchFilters();
		}
	},
	favTab : function(){
		AJS.$('li.menu-item, .active-tab').removeClass('active-tab');
		AJS.$('#mzf_fav_li').addClass("active-tab");
		ManageZQLFilters.findFilters(AJS.I18n.getText('zql.filter.manage.favorite.desc'), AJS.I18n.getText('zql.filter.manage.favorite.long.desc'), AJS.I18n.getText('common.favourites.favourite'), "/zql/executionFilter/?byUser=true&fav=true&offset=",0);
	}
});

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
	// adding prototypal function to close Backbone view
	Backbone.View.prototype.close = function () {
	  this.$el.empty();
	  this.remove();
	  this.unbind();
	};

	jQuery.ajax({
		url: getRestURL() + "/zql/executionFilter/user",
		type : "GET",
		contentType :"application/json",
		dataType: "json",
		success : function(response) {
			ZEPHYR.ZQL.MANAGE.LOGGED_IN_USER = response.LOGGED_IN_USER;
		}
	});
	ZEPHYR.ZQL.MANAGE.ZQLFilters.data = {}
	ZEPHYR.ZQL.manageZQLRouter = new ManageZQLRouter();
	Backbone.history.start();
});
