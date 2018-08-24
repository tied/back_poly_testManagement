"use strict";

AJS.$.namespace("ZEPHYR.Templates.WalkThroughTour");

var element = [];
var elementCounter = -1;
var appendingId = '';
var proceedWalkthrough = true;
var currentPage = '';
var pagesUrlDetails = [];
var dropdownStatus = false;
var skipFirstTime = false;

AJS.$(function () {
    // var walkThroughTourIcon = document.createElement('div');
    // walkThroughTourIcon.id = 'walk-through-initializer';
    // walkThroughTourIcon.className = 'walk-through-initializer-container walk-through-normal';
    // walkThroughTourIcon.addEventListener('click', takeTourClicked.bind(this));

    // let currentUrl = window.location.href;
    // if (currentUrl.search("#test-cycles-tab") >= 0) {
    //     AJS.$('#content').append(walkThroughTourIcon);
    // }


    // var tourOptionsContainer = document.createElement('div');
    // tourOptionsContainer.id = 'walk-through-options-outer-container';
    // tourOptionsContainer.className = 'walk-through-options-outer-container';
    // document.body.appendChild(tourOptionsContainer);
    // AJS.$('#walk-through-initializer').append();
});

function takeTourClicked() {
    if (dropdownStatus) {
        // AJS.$('#walk-through-toggle-container').remove();
        AJS.$('#dark-background').remove();
        AJS.$('#walk-through-initializer').removeClass('walk-through-selected');
        AJS.$('#walk-through-initializer').addClass('walk-through-normal');
        AJS.$('#tour-options-outer-container').remove();
        dropdownStatus = false;
    } else {
        var walkThroughOptionsHtml = ZEPHYR.Templates.WalkThroughTour.walkThroughToggleOptions();
        AJS.$('#walk-through-options-outer-container').append(walkThroughOptionsHtml);
        AJS.$(appendingId).append('<div class="darkBackground" onClick="takeTourClicked()" id="dark-background"></div>');
        AJS.$('#walk-through-initializer').removeClass('walk-through-normal');
        AJS.$('#walk-through-initializer').addClass('walk-through-selected');
        dropdownStatus = true;
    }
}

function startTour() {
    takeTourClicked();
    element = [];
    elementCounter = -1;
    appendingId = '';
    proceedWalkthrough = true;
    pagesUrlDetails = [];

    if (currentPage == 'cycleSummary') {
        testSummaryCallWalkThrough('walkThrough', true);
    } else if (currentPage == 'customField') {
        // customFieldsCallWalkThrough('walkThrough', true);
    } else if (currentPage == 'executionPage') {
        zephyrZqlCallWalkthrough('walkThrough', true);
    }
}

function walkThroughCall(XPaths, containerId, page, nextPagesUrlDetails, isNotFirstTime) {
    var knowledgeFlag = arguments.length > 5 && arguments[5] !== undefined ? arguments[5] : true;

    if (knowledgeFlag) {
        var walkThroughTourIcon = document.createElement('div');
        walkThroughTourIcon.id = 'walk-through-initializer';
        walkThroughTourIcon.className = 'walk-through-initializer-container walk-through-normal';
        walkThroughTourIcon.addEventListener('click', takeTourClicked.bind(this));

        if (AJS.$("#walk-through-initializer")) {
            AJS.$("#walk-through-initializer").remove();
        }
        // AJS.$('#content').append(walkThroughTourIcon);
        var tourOptionsContainer = document.createElement('div');
        tourOptionsContainer.id = 'walk-through-options-outer-container';
        tourOptionsContainer.className = 'walk-through-options-outer-container';

        if (AJS.$("#walk-through-options-outer-container")) {
            AJS.$("#walk-through-options-outer-container").remove();
        }

        if (page == 'issueView') {
            walkThroughTourIcon.classList.add("walkthrough-issueView");
            AJS.$('#project-config-panel-versions').append(walkThroughTourIcon);
            tourOptionsContainer.classList.add("walk-through-issueView");
            AJS.$('#walk-through-initializer').append(tourOptionsContainer);
        } else{
            AJS.$(window).scrollTop(0);
            AJS.$('#content').append(walkThroughTourIcon);
            document.body.appendChild(tourOptionsContainer);
        }

    }

    element = XPaths;
    currentPage = page;
    pagesUrlDetails = nextPagesUrlDetails;
    appendingId = containerId;
    elementCounter = -1;

    if (isNotFirstTime) {
        proceedWalkthrough = false;
        AJS.$('body').addClass('preventscroll');
        walkThroughBodyclick();
    } else {
        jQuery.ajax({
            url: getRestURL() + "/preference/walkThroughPreference",
            type: "PUT",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({}),
            success: function success(response) {
                skipFirstTime = response.skip;
                var updateResponse = false;
                if (proceedWalkthrough && response[page]) {
                    response[page] = false;
                    updateResponse = true;
                }
                if (response.skip) {
                    response.skip = false;
                    updateResponse = true;
                }
                if (updateResponse) {
                    jQuery.ajax({
                        url: getRestURL() + "/preference/walkThroughPreference?isUpdate=true",
                        type: "PUT",
                        contentType: "application/json",
                        dataType: "json",
                        data: JSON.stringify(response),
                        success: function success(updatedResponse) {
                            proceedWalkthrough = false;
                            AJS.$('body').addClass('preventscroll');
                            //AJS.$('body').css("top", 35);
                            walkThroughBodyclick();
                        }
                    });
                }
            }
        });
    }
}

function walkThroughBodyclick() {
    elementCounter += 1;
    var parent = document.getElementById(appendingId.slice(1, appendingId.length));
    var child = document.getElementById("walk-through-tour");

    // if (parent && child && parent.contains(child)) {
    //     child.remove();
    //     let tempElement = document.evaluate(element[elementCounter - 1].path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    //     tempElement.style.visibility = null;
    // }
    if (child) {
        child.parentNode.removeChild(child);
        var tempElement = void 0;
        if (!element[elementCounter - 1].element) {
            tempElement = document.evaluate(element[elementCounter - 1].path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        } else {
            tempElement = element[elementCounter - 1].element;
        }
        if (tempElement) {
            tempElement.style.visibility = "visible";
        }
    }

    if (element[elementCounter] != undefined) {

        var isLast = false;
        if (!element[elementCounter + 1]) {
            if (!element[elementCounter].link) {
                isLast = true;
            }
            proceedWalkthrough = true;
        }

        var showMoreOptionsFlag = true;
        if (pagesUrlDetails.length == 0) {
            showMoreOptionsFlag = false;
        }

        var walkThroughHtml = ZEPHYR.Templates.WalkThroughTour.walkThroughTourContainer({ elementData: element[elementCounter], isLast: isLast, skipFirstTime: skipFirstTime, showMoreOptionsFlag: showMoreOptionsFlag });
        AJS.$(appendingId).append(walkThroughHtml);

        AJS.$('#element-description').html(element[elementCounter].description);

        var mainElement = void 0;
        if (!element[elementCounter].element) {
            mainElement = document.evaluate(element[elementCounter].path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        } else {
            mainElement = element[elementCounter].element;
        }

        if (mainElement && AJS.$(mainElement).is(":visible")) {
            // mainElement.scrollIntoView();
            AJS.$('body').css("top", 0);
    
            var windowRect = {
                width: window.innerWidth,
                height: window.innerHeight
            },
                bodyRect = document.body.getBoundingClientRect(),
                elemRect = mainElement.getBoundingClientRect();

            var pageScrollCount = 0;
            var scrollUp = false;
            for (pageScrollCount = 0; pageScrollCount < bodyRect.height / windowRect.height + 1; pageScrollCount += 1) {
                if (elemRect.top < 0) {
                    if (windowRect.height * pageScrollCount > -elemRect.top) {
                        pageScrollCount -= 1;
                        scrollUp = true;
                        break;
                    }
                } else {
                    if (windowRect.height * pageScrollCount > elemRect.top) {
                        pageScrollCount -= 1;
                        break;
                    }
                }
            }
    
            // window.scrollTo(0, pageScrollCount * windowRect.height);
            var scrollingHeight = 0;
            if (pageScrollCount == 0 && scrollUp) {
                scrollingHeight = -1 * pageScrollCount * windowRect.height;
                AJS.$('body').css("top", -1 * pageScrollCount * windowRect.height);
            } else if (elemRect.top < 0) {
                scrollingHeight = -1 * pageScrollCount * windowRect.height;
                AJS.$('body').css("top", -1 * pageScrollCount * windowRect.height);
            } else if (pageScrollCount != 0) {
                scrollingHeight = -1 * pageScrollCount * windowRect.height;
                AJS.$('body').css("top", -1 * pageScrollCount * windowRect.height);
            }

            if (elemRect.bottom + 100 >= Math.abs(scrollingHeight) + windowRect.height) {
                scrollingHeight -= 100;
                // scrollingHeight -= windowRect.height / 10;
                AJS.$('body').css("top", scrollingHeight);
            } else if (elemRect.top < Math.abs(scrollingHeight) + 100 && scrollingHeight != 0) {
                scrollingHeight += 100;
                AJS.$('body').css("top", scrollingHeight);
            }

            elemRect = mainElement.getBoundingClientRect();
    
            // if (window.innerHeight < elemRect.top - 30) {
            //     window.scrollTo(0, 50);
            // }
    
            var fontSize = parseFloat(window.getComputedStyle(mainElement, null).getPropertyValue('font-size'));
            var textAlign = window.getComputedStyle(mainElement, null).getPropertyValue('text-align');
            var fontColor = window.getComputedStyle(mainElement, null).getPropertyValue('color');
            // let backgroundColor = window.getComputedStyle(mainElement, null).getPropertyValue('background-color');
    
            AJS.$('#element-to-show').css("width", elemRect.width);
            AJS.$('#element-to-show').css("height", elemRect.height);
            AJS.$('#element-to-show').css("top", elemRect.top - 2);
            AJS.$('#element-to-show').css("left", elemRect.left - 2);
            AJS.$('#element-to-show').css("font-size", fontSize - 2);
            AJS.$('#element-to-show').css("text-align", textAlign);
            AJS.$('#element-to-show').css("color", fontColor);
            AJS.$('#element-to-show').addClass(mainElement.className);
            AJS.$('#element-to-show').html(mainElement.innerHTML);
    
            var isLeftRight = false;
    
            if (elemRect.right + AJS.$('#walk-through-data-catd-container').width() + 30 <= windowRect.width) {
                AJS.$('#walk-through-data-catd-container').css("left", elemRect.right + 20);
                AJS.$('#square-pointer').css("left", elemRect.right + 14.14);
                isLeftRight = true;
            } else if (AJS.$('#walk-through-data-catd-container').width() + 30 <= elemRect.left) {
                AJS.$('#walk-through-data-catd-container').css("left", elemRect.left - AJS.$('#walk-through-data-catd-container').width() - 100);
                AJS.$('#square-pointer').css("left", elemRect.left - 27.07);
                isLeftRight = true;
            } else {
                AJS.$('#walk-through-data-catd-container').css("left", elemRect.left + elemRect.width / 2 - AJS.$('#walk-through-data-catd-container').width() / 2);
                AJS.$('#square-pointer').css("left", elemRect.left + elemRect.width / 2 - 7.07);
            }
    
            if (isLeftRight) {
                if (elemRect.top + AJS.$('#walk-through-data-catd-container').height() + 30 < windowRect.height) {
                    AJS.$('#walk-through-data-catd-container').css("top", elemRect.top + elemRect.height / 2 - 15);
                    AJS.$('#square-pointer').css("top", elemRect.top + elemRect.height / 2 - 5);
                } else {
                    AJS.$('#walk-through-data-catd-container').css("top", elemRect.top + elemRect.height - AJS.$('#walk-through-data-catd-container').height());
                    AJS.$('#square-pointer').css("top", elemRect.top + elemRect.height / 2 - 5);
                }
            } else {
                if (elemRect.bottom + AJS.$('#walk-through-data-catd-container').height() + 30 < windowRect.height) {
                    AJS.$('#walk-through-data-catd-container').css("top", elemRect.bottom + 15);
                    AJS.$('#square-pointer').css("top", elemRect.bottom + 11);
                } else {
                    AJS.$('#walk-through-data-catd-container').css("top", elemRect.top - AJS.$('#walk-through-data-catd-container').height() - 46);
                    AJS.$('#square-pointer').css("top", elemRect.top - 24);
                }
            }
    
            // if (asdf)
    
            // if (elemRect.width + AJS.$('#walk-through-data-catd-container').width() + 30 < windowRect.width) {
            //     if (elemRect.right + AJS.$('#walk-through-data-catd-container').width() + 30 <= windowRect.width) {
            //         AJS.$('#walk-through-data-catd-container').css("left", elemRect.right + 20);
            //         AJS.$('#square-pointer').css("left", elemRect.right + 14.14);
            //     } else {
            //         AJS.$('#walk-through-data-catd-container').css("left", elemRect.left - AJS.$('#walk-through-data-catd-container').width() - 49);
            //         AJS.$('#square-pointer').css("left", elemRect.left - 27.07);
            //     }
    
            //     if (elemRect.top + AJS.$('#walk-through-data-catd-container').height() + 30 < windowRect.height) {
            //         AJS.$('#walk-through-data-catd-container').css("top", (elemRect.top + (elemRect.height / 2) - 15));
            //         AJS.$('#square-pointer').css("top", (elemRect.top + (elemRect.height / 2) - 5));
            //     } else {
            //         AJS.$('#walk-through-data-catd-container').css("top", elemRect.top + elemRect.height - AJS.$('#walk-through-data-catd-container').height());
            //         AJS.$('#square-pointer').css("top", (elemRect.top + (elemRect.height / 2) - 5));
            //     }
            // } else {
            //     AJS.$('#walk-through-data-catd-container').css("left", (elemRect.left + (elemRect.width / 2) - (AJS.$('#walk-through-data-catd-container').width() / 2)));
            //     AJS.$('#square-pointer').css("left", (elemRect.left + (elemRect.width / 2) - 7.07));
    
            //     if (elemRect.bottom + AJS.$('#walk-through-data-catd-container').height() + 30 < windowRect.height) {
            //         AJS.$('#walk-through-data-catd-container').css("top", (elemRect.bottom + 15));
            //         AJS.$('#square-pointer').css("top", (elemRect.bottom + 11));
            //     } else {
            //         AJS.$('#walk-through-data-catd-container').css("top", elemRect.top - AJS.$('#walk-through-data-catd-container').height() - 46);
            //         AJS.$('#square-pointer').css("top", (elemRect.top - 24));
            //     }
            // }
    
    
            mainElement.style.visibility = "hidden";
        } else {
            walkThroughBodyclick();
        }
    } else {
        AJS.$('body').removeClass('preventscroll');
    }
}

function linkToPopup() {
    closeWalkThroughTour();
    var walkThroughHtmlPopup = ZEPHYR.Templates.WalkThroughTour.changePagePopup({ linkDetails: element[elementCounter] });
    AJS.$(appendingId).append(walkThroughHtmlPopup);
}

function linkTo() {
    var mainElement = void 0;
    if (!element[elementCounter].element) {
        mainElement = document.evaluate(element[elementCounter].path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    } else {
        mainElement = element[elementCounter].element;
    }
    if (element[elementCounter].linkFunction) {
        var clickingFunction = document.evaluate(element[elementCounter].linkFunction, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        AJS.$(clickingFunction).click();
    } else {
        AJS.$(mainElement).click();
    }
    if (element[elementCounter + 1]) {
        var waitLoadTime = 1000;
        if (element[elementCounter].delayTime) {
            waitLoadTime = element[elementCounter].delayTime;
        }

        var tempElement = void 0;
        if (!element[elementCounter].element) {
            tempElement = document.evaluate(element[elementCounter].path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        } else {
            tempElement = element[elementCounter].element;
        }
        tempElement.style.visibility = "visible";
        var child = document.getElementById("walk-through-tour");
        child.parentNode.removeChild(child);

        setTimeout(function () {
            walkThroughBodyclick();
        }, waitLoadTime);
    } else {
        closeWalkThroughTour();
    }
}

function navigatePage() {
    closeWalkThroughTour();
    var walkThroughHtml = ZEPHYR.Templates.WalkThroughTour.walkThroughPageNavigation({ pagesUrlDetails: pagesUrlDetails });
    AJS.$(appendingId).append(walkThroughHtml);
}

function changePage(url, toPage) {

    jQuery.ajax({
        url: getRestURL() + "/preference/walkThroughPreference",
        type: "PUT",
        contentType: "application/json",
        dataType: "json",
        data: JSON.stringify({}),
        success: function success(response) {
            response[toPage] = true;
            
            jQuery.ajax({
                url: getRestURL() + "/preference/walkThroughPreference?isUpdate=true",
                type: "PUT",
                contentType: "application/json",
                dataType: "json",
                data: JSON.stringify(response),
                success: function success(updatedResponse) {
                    window.location = url;
                }
            });
        }
    });

}

function manualStartTourPopup() {
    closeWalkThroughTour();
    var manualStartTourPopupHtml = ZEPHYR.Templates.WalkThroughTour.manualStartTourPopup();
    AJS.$(appendingId).append(manualStartTourPopupHtml);
}

function closeClicked() {
    proceedWalkthrough = true;
    closeWalkThroughTour();
}

function closeWalkThroughTour() {
    var tempElement = void 0;
    if (!element[elementCounter].element) {
        tempElement = document.evaluate(element[elementCounter].path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    } else {
        tempElement = element[elementCounter].element;
    }
    if (tempElement) {
        tempElement.style.visibility = "visible";
    }
    var child = document.getElementById("walk-through-tour");
    if (child) {
        child.parentNode.removeChild(child);
    }
    AJS.$('body').removeClass('preventscroll');
}

function newPageLayoutStart() {
    takeTourClicked();

    if (currentPage == 'cycleSummary') {
        testSummaryCallWalkThrough('newPageLayout', true);
    } else if (currentPage == 'customField') {
        // customFieldsCallWalkThrough('newPageLayout', true);
    } else if (currentPage == 'issueView') {
        stepsCallWalkthrough('newPageLayout', true);
    } else if (currentPage == 'executionPage') {
        zephyrZqlCallWalkthrough('newPageLayout', true);
    }
}

function newPageLayout(XPaths, containerId, page) {
    proceedWalkthrough = false;
    element = XPaths;
    currentPage = page;
    appendingId = containerId;

    var child = document.getElementById("walk-through-tour");
    if (child) {
        child.parentNode.removeChild(child);
    }

    var walkThroughHtml = ZEPHYR.Templates.WalkThroughTour.newPageLayout();
    AJS.$(appendingId).append(walkThroughHtml);

    var windowRect = {
        width: window.innerWidth,
        height: window.innerHeight
    };
    var bodyRect = document.body.getBoundingClientRect();

    // AJS.$('#new-page-layout-background').css("width", bodyRect.width);
    // AJS.$('#new-page-layout-background').css("height", bodyRect.height);

    for (var _elementCounter = 0; _elementCounter < element.length; _elementCounter += 1) {
        
        var mainElement = void 0;
        if (!element[_elementCounter].element) {
            mainElement = document.evaluate(element[_elementCounter].path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        } else {
            mainElement = element[_elementCounter].element;
        }
        
        if (mainElement) {
            var walkThroughElementHtml = ZEPHYR.Templates.WalkThroughTour.newPageLayoutEmenent({ counter: _elementCounter, element: element[_elementCounter] });
            AJS.$('#new-page-layout-background').append(walkThroughElementHtml);

            AJS.$('#new-page-layout-element-' + _elementCounter).html(mainElement.innerHTML);
            AJS.$('#new-page-layout-element-description-' + _elementCounter).html(element[_elementCounter].description);
            mainElement.style.visibility = "hidden";
    
            var elemRect = mainElement.getBoundingClientRect();
            var descritionRect = {
                height: AJS.$('#new-page-layout-element-description-' + _elementCounter).height(),
                width: AJS.$('#new-page-layout-element-description-' + _elementCounter).width()
            };
            var fontSize = parseFloat(window.getComputedStyle(mainElement, null).getPropertyValue('font-size'));
    
            AJS.$('#new-page-layout-element-' + _elementCounter).css("top", elemRect.top - 40);
            AJS.$('#new-page-layout-element-' + _elementCounter).css("left", elemRect.left);
            AJS.$('#new-page-layout-element-' + _elementCounter).css("width", elemRect.width);
            AJS.$('#new-page-layout-element-' + _elementCounter).css("height", elemRect.height);
            AJS.$('#new-page-layout-element-' + _elementCounter).css("padding", 5);
            AJS.$('#new-page-layout-element-' + _elementCounter).css("border-radius", 5);
            AJS.$('#new-page-layout-element-' + _elementCounter).css("font-size", fontSize - 1);
    
            if (element[_elementCounter].direction == 'down-left') {
                AJS.$('#new-page-layout-element-arrow-' + _elementCounter).css("top", elemRect.top + elemRect.height - 24);
                AJS.$('#new-page-layout-element-arrow-' + _elementCounter).css("left", elemRect.left + elemRect.width / 2 - 16);
    
                AJS.$('#new-page-layout-element-description-' + _elementCounter).css("top", elemRect.top + elemRect.height - descritionRect.height / 2);
                AJS.$('#new-page-layout-element-description-' + _elementCounter).css("left", elemRect.left + elemRect.width / 2 - descritionRect.width - 16);
            } else if (element[_elementCounter].direction == 'down-right') {
                AJS.$('#new-page-layout-element-arrow-' + _elementCounter).css("top", elemRect.top + elemRect.height - 24);
                AJS.$('#new-page-layout-element-arrow-' + _elementCounter).css("left", elemRect.left + elemRect.width / 2 - 16);
    
                AJS.$('#new-page-layout-element-description-' + _elementCounter).css("top", elemRect.top + elemRect.height - descritionRect.height / 2);
                AJS.$('#new-page-layout-element-description-' + _elementCounter).css("left", elemRect.left + elemRect.width / 2 + 20);
            } else if (element[_elementCounter].direction == 'top-left') {
                AJS.$('#new-page-layout-element-arrow-' + _elementCounter).css("top", elemRect.top - AJS.$('#new-page-layout-element-arrow-' + _elementCounter).height() / 2 - 60);
                AJS.$('#new-page-layout-element-arrow-' + _elementCounter).css("left", elemRect.left + elemRect.width / 2 - AJS.$('#new-page-layout-element-arrow-' + _elementCounter).width() / 2 - 5);
    
                AJS.$('#new-page-layout-element-description-' + _elementCounter).css("top", elemRect.top - 78 - descritionRect.height / 2);
                AJS.$('#new-page-layout-element-description-' + _elementCounter).css("left", elemRect.left + elemRect.width / 2 - descritionRect.width - 35);
            } else if (element[_elementCounter].direction == 'top-right') {
                AJS.$('#new-page-layout-element-arrow-' + _elementCounter).css("top", elemRect.top - AJS.$('#new-page-layout-element-arrow-' + _elementCounter).height() / 2 - 60);
                AJS.$('#new-page-layout-element-arrow-' + _elementCounter).css("left", elemRect.left + elemRect.width / 2 - AJS.$('#new-page-layout-element-arrow-' + _elementCounter).width() / 2 + 10);
    
                AJS.$('#new-page-layout-element-description-' + _elementCounter).css("top", elemRect.top - 78 - descritionRect.height / 2);
                AJS.$('#new-page-layout-element-description-' + _elementCounter).css("left", elemRect.left + elemRect.width / 2 + 27);
            }
        }
    }
}

function closeNewPageLayout() {
    proceedWalkthrough = true;

    for (var _elementCounter2 = 0; _elementCounter2 < element.length; _elementCounter2 += 1) {
        var mainElement = void 0;
        if (!element[_elementCounter2].element) {
            mainElement = document.evaluate(element[_elementCounter2].path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        } else {
            mainElement = element[_elementCounter2].element;
        }
        if (mainElement) {
            mainElement.style.visibility = "visible";
        }
    }

    if (AJS.$('#new-page-layout-background')) {
        AJS.$('#new-page-layout-background').remove();
    }
}

AJS.$('#walk-through-tour').on('click', function () {
    event.preventDefault();
    event.stopPropagation();
});
