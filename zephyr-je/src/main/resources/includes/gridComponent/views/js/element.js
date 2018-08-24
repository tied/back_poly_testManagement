'use strict';

var _get = function get(object, property, receiver) { if (object === null) object = Function.prototype; var desc = Object.getOwnPropertyDescriptor(object, property); if (desc === undefined) { var parent = Object.getPrototypeOf(object); if (parent === null) { return undefined; } else { return get(parent, property, receiver); } } else if ("value" in desc) { return desc.value; } else { var getter = desc.get; if (getter === undefined) { return undefined; } return getter.call(receiver); } };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var gridScrollingValue = 0;

var CustomCheckbox = function (_Polymer$Element) {
  _inherits(CustomCheckbox, _Polymer$Element);

  _createClass(CustomCheckbox, null, [{
    key: 'is',
    get: function get() {
      return "custom-checkbox";
    }
  }]);

  function CustomCheckbox() {
    _classCallCheck(this, CustomCheckbox);

    var _this = _possibleConstructorReturn(this, (CustomCheckbox.__proto__ || Object.getPrototypeOf(CustomCheckbox)).call(this));

    _this.selectedCheckboxValues = [];
    _this.selectedCheckboxContent = [];
    _this.readMode = true;
    _this.editMode = false;
    _this.value = '';
    _this.cloneOptions = '';
    _this.class = 'noInlineEdit';
    return _this;
  }

  _createClass(CustomCheckbox, [{
    key: '_optionsChanged',
    value: function _optionsChanged(newValue) {
      if (typeof newValue === 'string') {
        newValue = JSON.parse(newValue);
      }
      this.cloneOptions = JSON.parse(JSON.stringify(newValue));
      this._assignValues(newValue);
    }
  }, {
    key: '_assignValues',
    value: function _assignValues(value) {
      var that = this;
      this.selectedCheckboxValues = [];
      this.selectedCheckboxContent = [];
      value.map(function (option) {
        if (option.selected) {
          that.selectedCheckboxValues.push(option.value);
          that.selectedCheckboxContent.push(option.content);
        }
      });
      this.value = this.selectedCheckboxContent.toString();
      this.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true, detail: { index: that.rowindex, loader: that.loader == 'true' ? true : false } }));
    }
  }, {
    key: '_modeChange',
    value: function _modeChange(newValue) {
      if (this.mode === 'read') {
        this.readMode = true;
        this.editMode = false;
      } else {
        this.readMode = false;
        this.editMode = true;
      }
    }
  }, {
    key: '_changeMode',
    value: function _changeMode() {
      this.readMode = false;
      this.editMode = true;
      this.$.editMode.focus();
    }
  }, {
    key: '_handleChange',
    value: function _handleChange(e) {
      var index = this.selectedCheckboxValues.indexOf(e.target.value);
      if (e.target.checked) {
        this.selectedCheckboxValues.push(e.target.value);
        this.selectedCheckboxContent.push(e.target.content);
      } else {
        if (index >= 0) {
          this.selectedCheckboxValues.splice(index, 1);
          this.selectedCheckboxContent.splice(index, 1);
        }
      }
      this.cloneOptions.map(function (option) {
        if (option.value === e.target.value) {
          if (e.target.checked) {
            option.selected = true;
          } else {
            option.selected = false;
          }
        }
      });
      if (this.change) {
        this.dispatchEvent(new CustomEvent('submitvalue', { detail: { rowId: this.rowid, type: 'checkbox', value: this.selectedCheckboxValues, contentValue: this.selectedCheckboxContent }, bubbles: true, composed: true }));
      }
    }
  }, {
    key: '_checkblur',
    value: function _checkblur(ev) {
      if (!this.change) {
        if (ev.relatedTarget && ev.relatedTarget.localName === 'input' && !(ev.relatedTarget.type == 'submit')) {
          this.$.editMode.focus();
        } else {
          if (this.mode === 'read') {
            this.readMode = true;
            this.editMode = false;
          }
          this.options = JSON.parse(JSON.stringify(this.cloneOptions));
          this.dispatchEvent(new CustomEvent('submitvalue', { bubbles: true, composed: true }));
        }
      }
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        options: {
          type: Array,
          value: [],
          observer: '_optionsChanged'
        },
        rowid: {
          type: String,
          value: ''
        },
        cellkey: {
          type: String,
          value: ''
        },
        mode: {
          type: String,
          value: 'read',
          observer: '_modeChange'
        },
        change: {
          type: Boolean,
          value: false
        },
        rowindex: {
          type: String
        },
        loader: {
          type: String
        }
      };
    }
  }]);

  return CustomCheckbox;
}(Polymer.Element);

customElements.define(CustomCheckbox.is, CustomCheckbox);

var CustomRadio = function (_Polymer$Element2) {
  _inherits(CustomRadio, _Polymer$Element2);

  _createClass(CustomRadio, null, [{
    key: 'is',
    get: function get() {
      return "custom-radio";
    }
  }]);

  function CustomRadio() {
    _classCallCheck(this, CustomRadio);

    var _this2 = _possibleConstructorReturn(this, (CustomRadio.__proto__ || Object.getPrototypeOf(CustomRadio)).call(this));

    _this2.selectedRadioValues = '';
    _this2.selectedRadioContent = '';
    _this2.readMode = true;
    _this2.editMode = false;
    _this2.value = '';
    _this2.cloneOptions = '';
    return _this2;
  }

  _createClass(CustomRadio, [{
    key: '_optionsChanged',
    value: function _optionsChanged(newValue) {
      this.cloneOptions = JSON.parse(JSON.stringify(newValue));
      this._assignValues(newValue);
      this.addEventListener('submitValue', this._submitValue);
    }
  }, {
    key: '_assignValues',
    value: function _assignValues(value) {
      var that = this;
      this.selectedRadioContent = [];
      this.selectedRadioValues = [];
      value.map(function (option) {
        if (option.selected) {
          that.selectedRadioValues = option.value;
          that.selectedRadioContent = option.content;
        }
      });
      if (this.selectedRadioValues === '') {
        this.setProperties('options', this.options.splice(0, 0, { value: "", content: "None", selected: true }));
      } else {
        this.setProperties('options', this.options.splice(0, 0, { value: "", content: "None", selected: false }));
      }
      this.value = this.selectedRadioContent.toString();
    }
  }, {
    key: '_modeChange',
    value: function _modeChange(newValue) {
      if (this.mode === 'read') {
        this.readMode = true;
        this.editMode = false;
      } else {
        this.readMode = false;
        this.editMode = true;
      }
    }
  }, {
    key: '_changeMode',
    value: function _changeMode() {
      this.readMode = false;
      this.editMode = true;
      this.$.editMode.focus();
    }
  }, {
    key: 'handleChange',
    value: function handleChange(e) {
      if (e.target.checked) {
        this.selectedRadioValues = e.target.value;
        this.selectedRadioContent = e.target.content;
      }
      this.cloneOptions.map(function (option) {
        if (option.value === e.target.value) {
          option.selected = true;
        } else {
          option.selected = false;
        }
      });
    }
  }, {
    key: '_checkblur',
    value: function _checkblur(ev) {
      if (ev.relatedTarget && ev.relatedTarget.localName === 'input' && !(ev.relatedTarget.type == 'submit')) {
        this.$.editMode.focus();
      } else {
        if (this.mode === 'read') {
          this.readMode = true;
          this.editMode = false;
        }
        this.options = JSON.parse(JSON.stringify(this.cloneOptions));
        this.dispatchEvent(new CustomEvent('submitvalue', { detail: { type: 'RADIO_BUTTON', value: this.selectedRadioValues, contentValue: this.selectedRadioContent }, bubbles: true, composed: true }));
      }
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        options: {
          type: Array,
          value: [],
          observer: '_optionsChanged'
        },
        rowid: {
          type: String,
          value: ''
        },
        cellkey: {
          type: String,
          value: ''
        },
        mode: {
          type: String,
          value: 'read',
          observer: '_modeChange'
        }
      };
    }
  }]);

  return CustomRadio;
}(Polymer.Element);

customElements.define(CustomRadio.is, CustomRadio);

var CustomText = function (_Polymer$Element3) {
  _inherits(CustomText, _Polymer$Element3);

  _createClass(CustomText, null, [{
    key: 'is',
    get: function get() {
      return "custom-text";
    }
  }]);

  function CustomText() {
    _classCallCheck(this, CustomText);

    var _this3 = _possibleConstructorReturn(this, (CustomText.__proto__ || Object.getPrototypeOf(CustomText)).call(this));

    _this3.readMode = true;
    _this3.editMode = false;
    _this3.value = '';
    _this3.editValue = '';
    _this3.type = "text";
    _this3.typeLowerCase = "text";
    _this3.oldValue = '';
    return _this3;
  }

  _createClass(CustomText, [{
    key: '_optionsChanged',
    value: function _optionsChanged(newValue) {
      if (!newValue) {
        return;
      }

      var value = JSON.parse(JSON.stringify(newValue));
      if (value[0].isHTMLKey) {
        this.editValue = value[0].readValue;
      } else {
        this.editValue = value[0].value;
      }
      if (typeof(value[0].value) == "undefined") {
        this.value = '';
      } else{
        this.value = value[0].value;
      }
      this.type = value[0].type;
      this.typeLowerCase = this.type.toLowerCase();
      this.$.renderHTML.innerHTML = this.value;
      this.$.renderHTML.setAttribute('title', this.editValue);
      this.oldValue = this.editValue;
      if (!this.value) {
        AJS.$(this.$.editableField).addClass('empty');
      } else {
        AJS.$(this.$.editableField).removeClass('empty');
      }
    }
  }, {
    key: '_checkRender',
    value: function _checkRender(bool) {
      return bool ? "readMode" : "editMode";
    }
  }, {
    key: '_checkIsGrid',
    value: function _checkIsGrid(bool) {
      return bool ? "isGrid" : "isStandAlone";
    }
  }, {
    key: 'isInputType',
    value: function isInputType(type) {
      return type === 'TEXT' || type === 'NUMBER';
    }
  }, {
    key: '_modeChange',
    value: function _modeChange(newValue) {
      if (this.mode === 'read') {
        this.readMode = true;
        this.editMode = false;
      } else {
        this.readMode = false;
        this.editMode = true;
      }
    }
  }, {
    key: '_changeMode',
    value: function _changeMode() {
      this.readMode = false;
      this.editMode = true;
      var that = this;
      setTimeout(function () {
        AJS.$(that.$.editMode).find('input').focus();
        var tempValue = AJS.$(that.$.editMode).find('input').attr('value');
        AJS.$(that.$.editMode).find('input').attr('value', 0);
        AJS.$(that.$.editMode).find('input').attr('value', tempValue);
        var targetElement = AJS.$(that.$.editMode).find('textarea');
        AJS.$(targetElement).height('auto');
        // AJS.$(targetElement[0]).parent().height('auto');
        targetElement.focus();
        element = targetElement[0];
        if (element && element.scrollHeight > 1) {
          // this.style.height = 'auto';
          if (that.isgrid){
            if (element.scrollHeight > 200) {
              element.style.height = 165 + 'px';
            } else {
              element.style.height = element.scrollHeight - 2 + 'px';
            }
          } else {
            if (element.scrollHeight > 100) {
              element.style.height = 75 + 'px';
            } else {
              element.style.height = element.scrollHeight - 2 + 'px';
            }
          }
          // var _scrollHeight = element.scrollHeight;
          // _scrollHeight += 17;
          // AJS.$(element).parent().height(_scrollHeight);
        }
        that.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true }));
        targetElement.focus();
      }, 100);
    }
    }, {
      key: 'keyDown',
      value: function keyDown(ev) {
        ev.stopPropagation();
        ev.stopImmediatePropagation();
      }
    },{
    key: 'handleChange1',
    value: function handleChange1(ev) {
      ev.stopPropagation();
      ev.stopImmediatePropagation();
    }
  }, {
    key: 'handleChange',
    value: function handleChange(e) {
      this.editValue = e.target.value;
      if (e.target.type == 'number' && this.mode == 'edit') {
        e.target.focus();
      }
      e.stopPropagation();
      e.preventDefault();
      e.stopImmediatePropagation();
    }
  }, {
    key: '_checkblur',
    value: function _checkblur(ev) {
      ev.preventDefault();
      ev.stopImmediatePropagation();
      var that = this;
      if (ev.relatedTarget && (ev.relatedTarget.localName === 'input' || ev.relatedTarget.localName === 'textarea') && !(ev.relatedTarget.type == 'submit')) {
        this.$.editMode.focus();
      } else {
        if (this.mode === 'read') {
          this.readMode = true;
          this.editMode = false;
        }
        if (this.oldValue !== this.editValue) {
          if (ev.target.type == 'number' && this.editValue != '') {
            var key = ev.target.value;

            if (/^-?\d*\.?\d+$/.test(key)) {
              // document.formname.txt.focus();
              // return (false);
              if (!isNaN(parseFloat(key)) && isFinite(key)) {
                if (parseFloat(ev.target.value) > 100000000000000 || parseFloat(ev.target.value) < -100000000000000) {
                  showErrorMessage(ev.target.value + " is too large. </br>Maximum allowed limit is +/-10<sup>14</sup>", 3000);
                  // alert(ev.target.value + " is too large. Maximum allowed limit is 100000000000000");
                  ev.target.value = this.oldValue;
                  this.editValue = this.oldValue;
                  AJS.$(ev.target)[0].setAttribute('title', this.oldValue);
                  return;
                  // ev.preventDefault();
                }
              }
            } else {
              // alert('only numbers are allowed');
              if (ev.target.value != '') {
                showErrorMessage('only numbers are allowed', 3000);
              }
              ev.target.value = this.oldValue;
              this.editValue = this.oldValue;
              AJS.$(ev.target)[0].setAttribute('title', this.oldValue);
              return;
              ev.preventDefault();
            }
          }
            this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, type: this.type, value: this.editValue }, bubbles: true, composed: true }));
            this.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true }));
        } else if (this.oldValue === '' && this.editValue === '') {
          ev.target.value = '';
        }
      }
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        type: {
          type: String,
          value: 'text'
        },
        options: {
          type: Array,
          value: [],
          observer: '_optionsChanged'
        },
        rowid: {
          type: String,
          value: ''
        },
        cellkey: {
          type: String,
          value: ''
        },
        mode: {
          type: String,
          value: 'read',
          observer: '_modeChange'
        },
        inlineedit: {
          type: Boolean,
          value: true
        },
        isgrid: { type: Boolean, value: false }
      };
    }
  }]);

  return CustomText;
}(Polymer.Element);

customElements.define(CustomText.is, CustomText);

var CustomTextArea = function (_Polymer$Element4) {
  _inherits(CustomTextArea, _Polymer$Element4);

  _createClass(CustomTextArea, null, [{
    key: 'is',
    get: function get() {
      return "custom-textarea";
    }
  }]);

  function CustomTextArea() {
    _classCallCheck(this, CustomTextArea);

    var _this4 = _possibleConstructorReturn(this, (CustomTextArea.__proto__ || Object.getPrototypeOf(CustomTextArea)).call(this));

    _this4.readMode = true;
    _this4.editMode = false;
    _this4.showPreview = false;
    _this4.wikiBaseUrl = '';
    _this4.wikiPreview = '';
    _this4.wikiHelp = '';
    _this4.wikiHelpUrl = '';
    _this4.previewValue = '';
    _this4.editValue = '';
    _this4.readValue = '';
    _this4.key = '';
    _this4.oldValue = '';
    return _this4;
  }

  _createClass(CustomTextArea, [{
    key: '_setFocus',
    value: function _setFocus(newValue) {
      if (this.key === 'step' && this.editMode) AJS.$(this.$.editMode).find('textarea').focus();
    }
  }, {
    key: '_optionsChanged',
    value: function _optionsChanged(newValue) {
      this.showPreview = false;
      if (!newValue) {
        return;
      }
      var value = JSON.parse(JSON.stringify(newValue));
      this.key = value[0].key;
      this.editValue = value[0].editModeValue;
      this.readValue = value[0].readModeValue;
      this.wikiBaseUrl = value[0].wikiBaseUrl;
      this.wikiPreview = value[0].wikiPreview;
      this.wikiHelp = value[0].wikiHelp;
      this.wikiHelpUrl = value[0].wikiHelpUrl;
      this.$.renderHTML.innerHTML = this.readValue;

      var imgTags = this.$.renderHTML.querySelectorAll('img');
      var that = this;
      if (imgTags.length) {
        var promisesArray = [];
        imgTags.forEach(function (imgtag, index) {
          promisesArray[index] = new Promise(function (resolve, reject) {
            imgtag.onload = function () {
              resolve();
            };
            imgtag.onerror = function () {
              resolve();
            };
          });
        });

        Promise.all(promisesArray).then(function (values) {
          that.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true }));
        }).catch(function (reason) {
          that.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true }));
        });
      }

      this.oldValue = this.editValue;
      if (!this.readValue) {
        AJS.$(this.$.editableField).addClass('empty');
      } else {
        AJS.$(this.$.editableField).removeClass('empty');
      }
    }
  }, {
    key: '_checkRender',
    value: function _checkRender(bool) {
      return bool ? "readMode" : "editMode";
    }
  }, {
    key: '_checkIsGrid',
    value: function _checkIsGrid(bool) {
      return bool ? "isGrid" : "isStandAlone";
    }
  }, {
    key: '_modeChange',
    value: function _modeChange(newValue) {
      if (this.mode === 'read') {
        this.readMode = true;
        this.editMode = false;
      } else {
        this.readMode = false;
        this.editMode = true;
        this.$.editMode.style.display = 'block';
      }
    }
  }, {
    key: '_changeMode',
    value: function _changeMode() {
      this.readMode = false;
      this.editMode = true;
      this.$.editMode.style.display = 'block';
      var that = this;

      setTimeout(function () {
        var targetElement = AJS.$(that.$.editMode).find('textarea');
        AJS.$(targetElement).height('auto');
        targetElement.focus();
        element = targetElement[0];
        if (element.scrollHeight > 1) {
          // this.style.height = 'auto';
          element.style.height = element.scrollHeight - 2 + 'px';
          // var _scrollHeight = element.scrollHeight;
          // _scrollHeight += 17;
          // $(element).parent().height(_scrollHeight);
        }
        that.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true }));
        targetElement.focus();
      }, 0);
    }
  }, {
    key: '_showPreview',
    value: function _showPreview() {
      this.showPreview = !this.showPreview;
      var that = this;
      if (this.showPreview) {
        var data = {
          issueKey: "",
          rendererType: "zephyr-wiki-renderer",
          unrenderedMarkup: this.editValue
        };
        jQuery.ajax({
          url: this.wikiBaseUrl + "/util/render",
          type: "post",
          contentType: "application/json",
          data: JSON.stringify(data),
          dataType: "json",
          success: function success(response) {
            setTimeout(function () {
              if (that.shadowRoot.querySelector('.previewValues')) that.shadowRoot.querySelector('.previewValues').innerHTML = response.renderedHTML;
              that.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true }));
            }, 0);
          }
        });
      } else {
        setTimeout(function () {
          AJS.$(that.$.editMode).find('textarea').focus();
          that.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true }));
        }, 0);
      }
    }
  }, {
    key: 'handleChange1',
    value: function handleChange1(ev) {
      ev.stopPropagation();
      ev.stopImmediatePropagation();
      var that = this;
      if (ev.target.scrollHeight > 1) {
        // this.style.height = 'auto';
        ev.target.style.height = ev.target.scrollHeight - 2 + 'px';
        // var _scrollHeight = ev.target.scrollHeight;
        // _scrollHeight += 17;
        // $(ev.target).parent().height(_scrollHeight);
      }
      setTimeout(function () {
        // $(that.$.editMode).find('textarea').focus();
        that.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true }));
      }, 0);
    }
  }, {
    key: '_adjustHeight',
    value: function _adjustHeight(target) {
      if (target.scrollHeight > 1) {
        // this.style.height = 'auto';
        target.style.height = target.scrollHeight - 2 + 'px';
        // var _scrollHeight = target.scrollHeight;
        // _scrollHeight += 17;
        // $(target).parent().height(_scrollHeight);
      }
    }
  }, {
    key: 'handleChange',
    value: function handleChange(ev) {
      this.editValue = ev.target.value;
      ev.stopPropagation();
      ev.preventDefault();
      ev.stopImmediatePropagation();
    }
  }, {
    key: '_checkblur',
    value: function _checkblur(ev) {
      ev.preventDefault();

      if (ev.relatedTarget && (ev.relatedTarget.localName === 'textarea' || ev.relatedTarget.className.indexOf("stopBlur") >= 0)) {
        this.$.editMode.focus();
      } else {
        if (this.mode === 'read') {
          this.readMode = true;
          this.editMode = false;
        }
        if (this.key === 'result' && this.mode == 'edit' && ev.currentTarget.localName === 'textarea') {
          var a = AJS.$('#testDetailGrid')[0].shadowRoot.querySelectorAll('.tick-button')[0];
          AJS.$(a)[0].focus();
        }
        this._adjustHeight(ev.target);
        if (this.readMode) {
          this.$.editMode.style.display = 'none';
        }
        this.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true }));
        if (this.oldValue !== this.editValue) this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, type: 'LARGE_TEXT', value: this.editValue }, bubbles: true, composed: true }));
      }
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        options: {
          type: Array,
          value: [],
          observer: '_optionsChanged'
        },
        rowid: {
          type: String,
          value: ''
        },
        cellkey: {
          type: String,
          value: ''
        },
        mode: {
          type: String,
          value: 'read',
          observer: '_modeChange'
        },
        inlineedit: {
          type: Boolean,
          value: true
        },
        setfocus: {
          type: Boolean,
          value: false,
          observer: '_setFocus'
        }
      };
    }
  }]);

  return CustomTextArea;
}(Polymer.Element);

customElements.define(CustomTextArea.is, CustomTextArea);

var DropDown = function (_Polymer$Element5) {
  _inherits(DropDown, _Polymer$Element5);

  _createClass(DropDown, null, [{
    key: 'is',
    get: function get() {
      return "drop-down";
    }
  }]);

  function DropDown() {
    _classCallCheck(this, DropDown);

    var _this5 = _possibleConstructorReturn(this, (DropDown.__proto__ || Object.getPrototypeOf(DropDown)).call(this));

    _this5.openDropDown = false;
    _this5.value = '';
    _this5.isStatus = false;
    _this5.cloneOptions = [];
    _this5.readOnly = false;
    _this5.currentIndex = -1;
    _this5.oldValue = '';
    return _this5;
  }

  _createClass(DropDown, [{
    key: '_optionsChanged',
    value: function _optionsChanged(newValue) {
      this.cloneOptions = JSON.parse(JSON.stringify(newValue));
      this._assignValues(newValue);
    }
  }, {
    key: '_checkIsGrid',
    value: function _checkIsGrid(bool) {
      return bool ? "isGrid" : "isStandAlone";
    }
  }, {
    key: '_isReadOnly',
    value: function _isReadOnly(bool) {
      return bool ? "readOnly" : "";
    }
  }, {
    key: '_checkStatus',
    value: function _checkStatus(bool) {
      return bool ? "select-status" : "";
    }
  }, {
    key: '_assignValues',
    value: function _assignValues(value) {
      var that = this;
      this.value = '';
      value.map(function (option) {
        if (option.selected) {
          that.value = option.content;
          that.selectedValue = option.value;
          that.readOnly = option.readOnly;
          if (option.color) {
            that.isStatus = true;
            AJS.$(that.$.dropDown).css('background', option.color);
          }
        }
      });
      this.oldValue = this.value;
    }
  }, {
    key: '_triggerDropDown',
    value: function _triggerDropDown(ev) {
      this.openDropDown = !this.openDropDown;
      this.trigger = ev.currentTarget;
      if (this.isgrid) {
        var triggerElement = ev.currentTarget.getBoundingClientRect();
        var viewportHeight = window.innerHeight;
        var height = this.shadowRoot.querySelector('.dropDown-container').clientHeight;
        var topHeight = triggerElement.top + triggerElement.height + 2;
        var parentWidth = AJS.$(ev.currentTarget).parents('.cell-wrapper')[0].clientWidth;
        if (viewportHeight > topHeight + height) AJS.$(this.shadowRoot.querySelector('.dropDown-container')).css({ 'top': topHeight, 'left': triggerElement.left - parentWidth + triggerElement.width, 'width': parentWidth }); else {
          AJS.$(this.shadowRoot.querySelector('.dropDown-container')).css({ 'top': topHeight - triggerElement.height - height - 8, 'left': triggerElement.left - parentWidth + triggerElement.width, 'width': parentWidth });
        }
      }
    }
  }, {
    key: '_dropDownOpen',
    value: function _dropDownOpen(bool) {
      return bool ? "open" : "close";
    }
  }, {
    key: '_checkOptionClass',
    value: function _checkOptionClass(bool) {
      return bool ? "active" : "";
    }
  }, {
    key: '_keydown',
    value: function _keydown(e) {
      if (e.keyCode == 38 || e.keyCode == 40 || e.keyCode == 13) {
        var newIndex = this.currentIndex;
        this.cloneOptions.forEach(function (option, index) {
          option.highlighted = false;
        });
        if (e.keyCode == 38) {
          newIndex = Math.max(this.currentIndex - 1, 0);
          if (this.cloneOptions[newIndex].selected) {
            newIndex = Math.max(this.currentIndex - 2, 0);
          }
          this.cloneOptions[newIndex].highlighted = true;
          this.shadowRoot.querySelector('.dropDown-container.open ul').scrollTop -= 23; //Hieght of li element
        } else if (e.keyCode == 40) {
          newIndex = Math.min(this.currentIndex + 1, this.cloneOptions.length - 1);
          if (this.cloneOptions[newIndex].selected) {
            newIndex = Math.min(this.currentIndex + 2, this.cloneOptions.length - 1);
          }
          this.cloneOptions[newIndex].highlighted = true;
          if (newIndex != 0) {
            this.shadowRoot.querySelector('.dropDown-container.open ul').scrollTop += 23; //Hieght of li element
          }
        } else if (e.keyCode == 13 && this.currentIndex > -1) {
          this._handleChange({
            target: {
              innerHTML: this.cloneOptions[this.currentIndex].content,
              dataValue: this.cloneOptions[this.currentIndex].value
            }
          });
          this.currentIndex = -1;
        }
        this.currentIndex = newIndex;
        this.cloneOptions = JSON.parse(JSON.stringify(this.cloneOptions));
        e.preventDefault();
      }
    }
  }, {
    key: '_highlightOption',
    value: function _highlightOption(e) {
      this.currentIndex = e.target.dataIndex;
      this.cloneOptions.forEach(function (option, index) {
        option.highlighted = false;
      });
      this.cloneOptions[this.currentIndex].highlighted = true;
      this.cloneOptions = JSON.parse(JSON.stringify(this.cloneOptions));
    }
  }, {
    key: '_handleChange',
    value: function _handleChange(e) {
      var that = this;
      this.openDropDown = false;
      this.value = e.target.innerHTML;
      this.selectedValue = e.target.dataValue;
      this.selectedContent = this.value;
      this.cloneOptions.map(function (option, index) {
        var path = 'cloneOptions.' + index.toString();
        if (option.isStatus) {
          if (that.selectedValue === option.value) {
            that.set(path, { content: option.content, value: option.value, selected: true, isStatus: option.isStatus, color: option.color, readOnly: option.readOnly });
            if (option.color) {
              AJS.$(that.$.dropDown).css('background', option.color);
            }
          } else {
            that.set(path, { content: option.content, value: option.value, selected: false, isStatus: option.isStatus, color: option.color, readOnly: option.readOnly });
          }
        } else {
          if (that.selectedValue === option.value) {
            that.set(path, { content: option.content, value: option.value, selected: true });
          } else {
            that.set(path, { content: option.content, value: option.value, selected: false });
          }
        }
      });
      if (this.oldValue !== this.selectedContent) this.dispatchEvent(new CustomEvent('submitvalue', { detail: { type: 'SINGLE_SELECT', value: e.target.dataValue, contentValue: e.target.innerHTML }, bubbles: true, composed: true }));
      this._checkblur();
    }
  }, {
    key: '_emptyData',
    value: function _emptyData(ev) {
      var that = this;
      this.openDropDown = false;
      this.value = '';
      this.selectedValue = '';
      this.selectedContent = '';
      this.cloneOptions.map(function (option, index) {
        var path = 'cloneOptions.' + index.toString();
        if (option.isStatus) {
          if (that.selectedValue === option.value) {
            that.set(path, { content: option.content, value: option.value, selected: true, isStatus: option.isStatus, color: option.color, readOnly: option.readOnly });
            if (option.color) {
              AJS.$(that.$.dropDown).css('background', option.color);
            }
          } else {
            that.set(path, { content: option.content, value: option.value, selected: false, isStatus: option.isStatus, color: option.color, readOnly: option.readOnly });
          }
        } else {
          if (that.selectedValue === option.value) {
            that.set(path, { content: option.content, value: option.value, selected: true });
          } else {
            that.set(path, { content: option.content, value: option.value, selected: false });
          }
        }
      });
      this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, type: 'SINGLE_SELECT', value: this.selectedValue }, bubbles: true, composed: true }));
    }
  }, {
    key: '_checkblur',
    value: function _checkblur(ev) {
      if (ev && ev.relatedTarget && (ev.relatedTarget.tagName == 'LI' || (ev.relatedTarget.tagName == 'DIV' && ev.relatedTarget.className.indexOf('stopBlur') > -1) )) {
        this.openDropDown = true;
        ev.target.focus();
      } else {
        this.openDropDown = false;
      }
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        options: {
          type: Array,
          value: [],
          observer: '_optionsChanged'
        },
        rowid: {
          type: String,
          value: ''
        },
        cellkey: {
          type: String,
          value: ''
        },
        isgrid: {
          type: Boolean,
          value: false
        },
        imgurl: {
          type: String,
          value: ''
        }
      };
    }
  }]);

  return DropDown;
}(Polymer.Element);

customElements.define(DropDown.is, DropDown);

var DropDownCheckbox = function (_Polymer$Element6) {
  _inherits(DropDownCheckbox, _Polymer$Element6);

  _createClass(DropDownCheckbox, null, [{
    key: 'is',
    get: function get() {
      return "drop-downcheckbox";
    }
  }]);

  function DropDownCheckbox() {
    _classCallCheck(this, DropDownCheckbox);

    var _this6 = _possibleConstructorReturn(this, (DropDownCheckbox.__proto__ || Object.getPrototypeOf(DropDownCheckbox)).call(this));

    _this6.openDropDown = false;
    _this6.value = '';
    _this6.cloneOptions = [];
    _this6.trigger;
    _this6.oldValue = '';
    return _this6;
  }

  _createClass(DropDownCheckbox, [{
    key: '_optionsChanged',
    value: function _optionsChanged(newValue) {
      if (typeof newValue === 'string') {
        newValue = JSON.parse(newValue);
      }
      this.cloneOptions = JSON.parse(JSON.stringify(newValue));
      this._assignValues(newValue);
    }
  }, {
    key: '_checkIsGrid',
    value: function _checkIsGrid(bool) {
      return bool ? "isGrid" : "isStandAlone";
    }
  }, {
    key: '_dropDownOpen',
    value: function _dropDownOpen(bool) {
      return bool ? "open" : "close";
    }
  }, {
    key: '_assignValues',
    value: function _assignValues(value) {
      var that = this;
      this.selectedCheckboxValues = [];
      this.selectedCheckboxContent = [];
      value.map(function (option) {
        if (option.selected) {
          that.selectedCheckboxValues.push(option.value);
          that.selectedCheckboxContent.push(option.content);
        }
      });
      this.value = this.selectedCheckboxContent.toString();
      this.oldValue = this.selectedCheckboxContent.sort().toString();
    }
  }, {
    key: '_triggerDropDown',
    value: function _triggerDropDown(ev) {
      this.openDropDown = !this.openDropDown;
      this.trigger = ev.currentTarget;
      if (this.isgrid) {
        var triggerElement = ev.currentTarget.getBoundingClientRect();
        var viewportHeight = window.innerHeight;
        var height = this.shadowRoot.querySelector('.dropDown-container').clientHeight;
        var topHeight = triggerElement.top + triggerElement.height + 2;
        var parentWidth = AJS.$(ev.currentTarget).parents('.cell-wrapper')[0].clientWidth;
        if (viewportHeight > topHeight + height) AJS.$(this.shadowRoot.querySelector('.dropDown-container')).css({ 'top': topHeight, 'left': triggerElement.left - parentWidth + triggerElement.width, 'width': parentWidth }); else {
          AJS.$(this.shadowRoot.querySelector('.dropDown-container')).css({ 'top': topHeight - triggerElement.height - height - 8, 'left': triggerElement.left - parentWidth + triggerElement.width, 'width': parentWidth });
        }
      }
    }
  }, {
    key: '_handleChange',
    value: function _handleChange(e) {
      var that = this;
      this.selectedValue = e.target.dataValue;
      var index = this.selectedCheckboxValues.indexOf(e.target.value);
      if (e.target.checked) {
        var position;
        this.cloneOptions.forEach(function(obj, index) {
            if(obj.value == e.target.value) {
                position = index;
            }
        });
        //this.selectedCheckboxValues.push(e.target.value);
        this.selectedCheckboxValues.splice(position, 0, e.target.value);
        //this.selectedCheckboxContent.push(e.target.content);
        this.selectedCheckboxContent.splice(position, 0, e.target.content);

        this.value = this.selectedCheckboxContent.toString();
      } else {
        if (index >= 0) {
          this.selectedCheckboxValues.splice(index, 1);
          this.selectedCheckboxContent.splice(index, 1);
          this.value = this.selectedCheckboxContent.toString();
        }
      }
      this.cloneOptions.map(function (option, index) {
        var path = 'cloneOptions.' + index.toString();
        if (option.value === e.target.value) {
          if (e.target.checked) {
            that.set(path, { content: option.content, value: option.value, selected: true });
          } else {
            that.set(path, { content: option.content, value: option.value, selected: false });
          }
        }
      });
      if (this.change) {
        this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, rowId: this.rowid, type: 'CHECKBOX', value: this.selectedCheckboxValues, contentValue: this.selectedCheckboxContent }, bubbles: true, composed: true }));
      }

      this.trigger.focus();
    }
  }, {
    key: '_emptyData',
    value: function _emptyData(ev) {
      var that = this;
      this.openDropDown = false;
      this.value = '';
      this.selectedCheckboxValues = [];
      this.selectedCheckboxContent = [];
      this.cloneOptions.map(function (option, index) {
        var path = 'cloneOptions.' + index.toString();
        that.set(path, { content: option.content, value: option.value, selected: false });
      });
      this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, type: 'CHECKBOX', value: this.selectedCheckboxValues, contentValue: this.selectedCheckboxContent }, bubbles: true, composed: true }));
    }
  }, {
    key: '_checkblur',
    value: function _checkblur(ev) {
      if (ev.relatedTarget && (ev.relatedTarget.localName === 'input' || ev.relatedTarget.className.indexOf('content') >= 0) && !(ev.relatedTarget.type == 'submit')) {
        this.trigger.focus();
        this.openDropDown = true;
      } else {
        this.openDropDown = false;
        if (this.oldValue !== this.selectedCheckboxContent.sort().toString()) this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, type: 'CHECKBOX', value: this.selectedCheckboxValues, contentValue: this.selectedCheckboxContent }, bubbles: true, composed: true }));
      }
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        options: {
          type: Array,
          value: [],
          observer: '_optionsChanged'
        },
        rowid: {
          type: String,
          value: ''
        },
        cellkey: {
          type: String,
          value: ''
        },
        isgrid: {
          type: Boolean,
          value: false
        },
        imgurl: {
          type: String,
          value: ''
        }
      };
    }
  }]);

  return DropDownCheckbox;
}(Polymer.Element);

customElements.define(DropDownCheckbox.is, DropDownCheckbox);

var DropDownRadio = function (_Polymer$Element7) {
  _inherits(DropDownRadio, _Polymer$Element7);

  _createClass(DropDownRadio, null, [{
    key: 'is',
    get: function get() {
      return "drop-downradio";
    }
  }]);

  function DropDownRadio() {
    _classCallCheck(this, DropDownRadio);

    var _this7 = _possibleConstructorReturn(this, (DropDownRadio.__proto__ || Object.getPrototypeOf(DropDownRadio)).call(this));

    _this7.selectedRadioValues = '';
    _this7.selectedRadioContent = '';
    _this7.openDropDown = false;
    _this7.value = '';
    _this7.cloneOptions = [];
    _this7.trigger;
    _this7.oldValue = '';
    return _this7;
  }

  _createClass(DropDownRadio, [{
    key: '_optionsChanged',
    value: function _optionsChanged(newValue) {
      this.cloneOptions = JSON.parse(JSON.stringify(newValue));
      this._assignValues(newValue);
    }
  }, {
    key: '_checkIsGrid',
    value: function _checkIsGrid(bool) {
      return bool ? "isGrid" : "isStandAlone";
    }
  }, {
    key: '_dropDownOpen',
    value: function _dropDownOpen(bool) {
      return bool ? "open" : "close";
    }
  }, {
    key: '_assignValues',
    value: function _assignValues(value) {
      var that = this;
      this.selectedRadioContent = '';
      this.selectedRadioValues = '';
      value.map(function (option) {
        if (option.selected) {
          that.selectedRadioValues = option.value;
          that.selectedRadioContent = option.content;
        }
      });

      this.cloneOptions = this.options;
      this.value = this.selectedRadioContent;
      this.oldValue = this.value;
    }
  }, {
    key: '_triggerDropDown',
    value: function _triggerDropDown(ev) {
      this.openDropDown = !this.openDropDown;
      this.trigger = ev.currentTarget;
      if (this.isgrid) {
        var triggerElement = ev.currentTarget.getBoundingClientRect();
        var viewportHeight = window.innerHeight;
        var height = this.shadowRoot.querySelector('.dropDown-container').clientHeight;
        var topHeight = triggerElement.top + triggerElement.height + 2;
        var parentWidth = AJS.$(ev.currentTarget).parents('.cell-wrapper')[0].clientWidth;
        if (viewportHeight > topHeight + height) AJS.$(this.shadowRoot.querySelector('.dropDown-container')).css({ 'top': topHeight, 'left': triggerElement.left - parentWidth + triggerElement.width, 'width': parentWidth }); else {
          AJS.$(this.shadowRoot.querySelector('.dropDown-container')).css({ 'top': topHeight - triggerElement.height - height - 8, 'left': triggerElement.left - parentWidth + triggerElement.width, 'width': parentWidth });
        }
      }
    }
  }, {
    key: '_handleChange',
    value: function _handleChange(e) {
      var that = this;
      if (e.target.checked) {
        this.selectedRadioValues = e.target.value;
        this.selectedRadioContent = e.target.content;
      }
      this.value = this.selectedRadioContent;
      this.cloneOptions.map(function (option, index) {
        var path = 'cloneOptions.' + index.toString();
        if (option.value === e.target.value) {
          that.set(path, { content: option.content, value: option.value, selected: true });
        } else {
          that.set(path, { content: option.content, value: option.value, selected: false });
        }
      });
      this.trigger.focus();
    }
  }, {
    key: '_emptyData',
    value: function _emptyData(ev) {
      var that = this;
      this.openDropDown = false;
      this.value = '';
      this.selectedRadioValues = '';
      this.selectedRadioContent = '';
      this.cloneOptions.map(function (option, index) {
        var path = 'cloneOptions.' + index.toString();
        that.set(path, { content: option.content, value: option.value, selected: false });
      });
      this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, type: 'RADIO_BUTTON', value: this.selectedRadioValues, contentValue: this.selectedRadioContent }, bubbles: true, composed: true }));
    }
  }, {
    key: '_checkblur',
    value: function _checkblur(ev) {
      if (ev.relatedTarget && (ev.relatedTarget.localName === 'input' || ev.relatedTarget.className.indexOf('content') >= 0) && !(ev.relatedTarget.type == 'submit')) {
        this.trigger.focus();
        this.openDropDown = true;
      } else {
        this.openDropDown = false;
        if (this.oldValue !== this.selectedRadioContent) this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, type: 'RADIO_BUTTON', value: this.selectedRadioValues, contentValue: this.selectedRadioContent }, bubbles: true, composed: true }));
      }
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        options: {
          type: Array,
          value: [],
          observer: '_optionsChanged'
        },
        rowid: {
          type: String,
          value: ''
        },
        cellkey: {
          type: String,
          value: ''
        },
        isgrid: {
          type: Boolean,
          value: false
        },
        imgurl: {
          type: String,
          value: ''
        }
      };
    }
  }]);

  return DropDownRadio;
}(Polymer.Element);

customElements.define(DropDownRadio.is, DropDownRadio);

var DropDownDate = function (_Polymer$Element8) {
  _inherits(DropDownDate, _Polymer$Element8);

  _createClass(DropDownDate, null, [{
    key: 'is',
    get: function get() {
      return "drop-downdate";
    }
  }]);

  function DropDownDate() {
    _classCallCheck(this, DropDownDate);

    var _this8 = _possibleConstructorReturn(this, (DropDownDate.__proto__ || Object.getPrototypeOf(DropDownDate)).call(this));

    _this8.value = '';
    _this8.cloneOptions = [];
    _this8.trigger;
    return _this8;
  }

  _createClass(DropDownDate, [{
    key: '_optionsChanged',
    value: function _optionsChanged(newValue) {
      this.value = newValue[0].value;
    }
  }, {
    key: '_checkIsGrid',
    value: function _checkIsGrid(bool) {
      return bool ? "isGrid" : "isStandAlone";
    }
  }, {
    key: '_emptyData',
    value: function _emptyData(ev) {
      this.value = '';
      this.dispatchEvent(new CustomEvent('triggerdatechooser', { detail: { 'onlyUpdateValue': true, 'event': ev, 'value': this.value }, bubbles: true, composed: true }));
    }
  }, {
    key: '_triggerDropDown',
    value: function _triggerDropDown(ev) {
      this.dispatchEvent(new CustomEvent('triggerdatechooser', { detail: { 'onlyUpdateValue': false, 'event': ev, 'value': this.value }, bubbles: true, composed: true }));
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        options: {
          type: Array,
          value: [],
          observer: '_optionsChanged'
        },
        rowid: {
          type: String,
          value: ''
        },
        cellkey: {
          type: String,
          value: ''
        },
        isgrid: {
          type: Boolean,
          value: false
        },
        imgurl: {
          type: String,
          value: ''
        }
      };
    }
  }]);

  return DropDownDate;
}(Polymer.Element);

customElements.define(DropDownDate.is, DropDownDate);

var DropDownMultiSelect = function (_Polymer$Element9) {
  _inherits(DropDownMultiSelect, _Polymer$Element9);

  _createClass(DropDownMultiSelect, null, [{
    key: 'is',
    get: function get() {
      return "drop-downmultiselect";
    }
  }]);

  function DropDownMultiSelect() {
    _classCallCheck(this, DropDownMultiSelect);

    var _this9 = _possibleConstructorReturn(this, (DropDownMultiSelect.__proto__ || Object.getPrototypeOf(DropDownMultiSelect)).call(this));

    _this9.openDropDown = false;
    _this9.value = '';
    _this9.selectedValue = [];
    _this9.cloneOptions = [];
    _this9.trigger;
    _this9.oldValue = '';
    return _this9;
  }

  _createClass(DropDownMultiSelect, [{
    key: '_optionsChanged',
    value: function _optionsChanged(newValue) {
      this.cloneOptions = JSON.parse(JSON.stringify(newValue));
      this._assignValues(newValue);
    }
  }, {
    key: '_checkIsGrid',
    value: function _checkIsGrid(bool) {
      return bool ? "isGrid" : "isStandAlone";
    }
  }, {
    key: '_dropDownOpen',
    value: function _dropDownOpen(bool) {
      return bool ? "open" : "close";
    }
  }, {
    key: '_assignValues',
    value: function _assignValues(value) {
      var that = this;
      this.selectedValue = [];
      this.selectedContent = [];
      value.map(function (option) {
        if (option.selected) {
          that.selectedValue.push(option.value);
          that.selectedContent.push(option.content);
        }
      });
      this.cloneOptions = this.options;
      this.value = this.selectedContent.toString();
      this.oldValue = this.selectedContent.sort().toString();
      var selectFragment = document.createDocumentFragment();
      this.cloneOptions.forEach(function (option) {
        var optionElem = document.createElement('option');
        optionElem.value = option.value;
        optionElem.selected = option.selected;
        optionElem.title = option.content;
        optionElem.text = option.content;
        selectFragment.appendChild(optionElem);
      });
      this.$.dropDownSelectElem.innerHTML = '';
      this.$.dropDownSelectElem.appendChild(selectFragment);
    }
  }, {
    key: '_triggerDropDown',
    value: function _triggerDropDown(ev) {
      this.openDropDown = !this.openDropDown;
      this.trigger = ev.currentTarget;
      if (this.isgrid) {
        var triggerElement = ev.currentTarget.getBoundingClientRect();
        var viewportHeight = window.innerHeight;
        var height = this.shadowRoot.querySelector('.dropDown-container').clientHeight;
        var topHeight = triggerElement.top + triggerElement.height + 2;
        var parentWidth = AJS.$(ev.currentTarget).parents('.cell-wrapper')[0].clientWidth;
        if (viewportHeight > topHeight + height) AJS.$(this.shadowRoot.querySelector('.dropDown-container')).css({ 'top': topHeight, 'left': triggerElement.left - parentWidth + triggerElement.width, 'width': parentWidth }); else {
          AJS.$(this.shadowRoot.querySelector('.dropDown-container')).css({ 'top': topHeight - triggerElement.height - height - 8, 'left': triggerElement.left - parentWidth + triggerElement.width, 'width': parentWidth });
        }
      }
      var that = this;
      setTimeout(function () {
        AJS.$(that.$.editMode).find('select').focus();
      }, 100);
    }
  }, {
    key: '_handleChange',
    value: function _handleChange(e) {
      var that = this;
      this.selectedValue = AJS.$(e.target).val();
      this.selectedContent = [];
      this.cloneOptions.map(function (option, index) {
        var path = 'cloneOptions.' + index.toString();
        that.selectedValue = AJS.$(e.target).val();
        if (that.selectedValue.indexOf(option.value) >= 0) {
          that.selectedContent.push(option.content);
          that.set(path, { content: option.content, value: option.value, selected: true });
          that.value = that.selectedContent.toString();
        } else {
          that.set(path, { content: option.content, value: option.value, selected: false });
        }
      });
      AJS.$(e.target).focus();
    }
  }, {
    key: '_emptyData',
    value: function _emptyData(ev) {
      var that = this;
      this.openDropDown = false;
      this.value = '';
      this.selectedValue = [];
      this.selectedContent = [];
      this.cloneOptions.map(function (option, index) {
        var path = 'cloneOptions.' + index.toString();
        that.set(path, { content: option.content, value: option.value, selected: false });
      });
      this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, type: 'MULTI_SELECT', value: this.selectedValue, contentValue: this.selectedContent }, bubbles: true, composed: true }));
    }
  }, {
    key: '_focusout',
    value: function _focusout(ev) {
      this.value = this.selectedContent.toString();
      this.openDropDown = false;
      if (this.oldValue !== this.selectedContent.sort().toString()) this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, type: 'MULTI_SELECT', value: this.selectedValue, contentValue: this.selectedContent }, bubbles: true, composed: true }));
    }
  }, {
    key: '_checkblur',
    value: function _checkblur(ev) {
      if (ev.relatedTarget && ev.relatedTarget.localName === 'select') {
        this.openDropDown = true;
      } else {
        this.openDropDown = false;
        if (this.oldValue !== this.selectedContent.sort().toString()) this.dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: this.oldValue, type: 'MULTI_SELECT', value: this.selectedValue, contentValue: this.selectedContent }, bubbles: true, composed: true }));
      }
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        options: {
          type: Array,
          value: [],
          observer: '_optionsChanged'
        },
        rowid: {
          type: String,
          value: ''
        },
        cellkey: {
          type: String,
          value: ''
        },
        isgrid: {
          type: Boolean,
          value: false
        },
        imgurl: {
          type: String,
          value: ''
        }
      };
    }
  }]);

  return DropDownMultiSelect;
}(Polymer.Element);

customElements.define(DropDownMultiSelect.is, DropDownMultiSelect);

var GridCell = function (_Polymer$Element10) {
  _inherits(GridCell, _Polymer$Element10);

  _createClass(GridCell, null, [{
    key: 'is',
    get: function get() {
      return "grid-cell";
    }
  }]);

  function GridCell() {
    _classCallCheck(this, GridCell);

    var _this10 = _possibleConstructorReturn(this, (GridCell.__proto__ || Object.getPrototypeOf(GridCell)).call(this));

    _this10.openInlineDialog = false;
    _this10.attachment = false;
    _this10.singleSelect = false;
    _this10.multiSelect = false;
    _this10.showDefectSearchResult = false;
    _this10.checkbox = false;
    _this10.openDefectSearchBox = false;
    _this10.radio = false;
    _this10.textarea = false;
    _this10.textfield = false;
    _this10.date = false;
    _this10.readMode = false;
    _this10.defect = false;
    _this10.dropDown = false;
    _this10.wikiTextarea = false;
    _this10.htmlContent = false;
    _this10.canAddAttachment = false;
    _this10.stepDefects = false;
    _this10.defectKeys = [];
    _this10.executionsDefect = [];
    _this10.stepDefect = [];
    _this10.openDefectInlineDialog = false;
    _this10.openStepDefectInlineDialog = false;
    _this10.showDefault = false;
    _this10.options = [];
    _this10.value = '';
    _this10.rowId = '';
    _this10.columnId = '';
    _this10.isEditMode = false;
    _this10.isGrid = true;
    _this10.contextPath = '';
    _this10.imgUrl = '';
    return _this10;
  }

  _createClass(GridCell, [{
    key: 'check',
    value: function check() {
      this._initializer();
    }
  }, {
    key: '_checkIsGrid',
    value: function _checkIsGrid(bool) {
      return bool ? "isGrid" : "isStandAlone";
    }
  }, {
    key: '_showImageDetail',
    value: function _showImageDetail(event) {
      event.stopImmediatePropagation();
      event.preventDefault();
      event.stopPropagation();
      var fileId = event.currentTarget.dataFileid;
      var obj = {
        fileId: fileId,
        attachmentArray: this.attachmentsObject.attachmentsList
      };
      this.dispatchEvent(new CustomEvent('attachmentpreview', { detail: obj, bubbles: true, composed: true }));
      this._closeInlineDialog();
    }
  }, {
    key: '_openDefectDialog',
    value: function _openDefectDialog(bool) {
      var that = this;
      setTimeout(function () {
        if (that.isGrid && bool) {
          var triggerElement = that.shadowRoot.querySelector('.defect-hover').getBoundingClientRect();
          var viewportHeight = window.innerHeight;
          var viewportWidth = window.innerWidth;
          var height = that.shadowRoot.querySelector('.defects-inlineDialogWrapper').clientHeight;
          var topHeight = triggerElement.top + triggerElement.height + 2;

          var leftOffset, rightOffset, topOffset, bottomOffset;
          if (viewportHeight > topHeight + 200) {
            topOffset = topHeight;
            bottomOffset = 'auto';
          } else {
            topOffset = 'auto';
            bottomOffset = viewportHeight - topHeight + triggerElement.height + 5;
          }
          if (viewportWidth > triggerElement.left + 450) {
            leftOffset = triggerElement.left;
            rightOffset = 'auto';
          } else {
            rightOffset = viewportWidth - (triggerElement.left + triggerElement.width);
            leftOffset = 'auto';
            if (rightOffset < 0) {
              rightOffset = 0;
            }
          }
          AJS.$(that.shadowRoot.querySelector('.defects-inlineDialogWrapper')).css({ 'right': rightOffset, 'top' : topOffset, 'bottom' : bottomOffset , 'left' : leftOffset });
        }
      }, 0);
      return bool ? "open" : "close";
    }
  }, {
    key: '_openAtttachmentDialog',
    value: function _openAtttachmentDialog(bool) {
      return bool ? "open" : "close";
    }
  }, {
    key: '_canAddAttachment',
    value: function _canAddAttachment(bool) {
      return bool ? "show" : "hide";
    }
  }, {
    key: '_isImageTypeAttachment',
    value: function _isImageTypeAttachment(item) {
      return (/\.(gif|jpg|jpeg|tiff|png)$/i).test(item.fileName);
    }
  }, {
    key: '_adjustRowHeight',
    value: function _adjustRowHeight(ev) {
      ev.stopImmediatePropagation();
      ev.stopPropagation();
      ev.preventDefault();
      this.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true, detail: { index: this.rowindex } }));
    }
  }, {
    key: '_triggerInlineDialog',
    value: function _triggerInlineDialog(ev) {
      if (this.attachmentsObject.isEnable) {
        if (this.fetchAttachment && !this.isFetchAttachment) {
          var obj = {
            id: this.rowId,
          };
          this.isFetchAttachment = true;
          this.dispatchEvent(new CustomEvent('fetchattachment', { detail: obj, bubbles: true, composed: true }));
        } else {
          this.openInlineDialog = !this.openInlineDialog;
          if (this.isGrid) {
            var triggerElement = ev.currentTarget.getBoundingClientRect();
            var viewportHeight = window.innerHeight;
            var height = this.shadowRoot.querySelector('.attachments-inlineDialogWrapper').clientHeight;
            var topHeight = triggerElement.top + triggerElement.height + 2;
            if (viewportHeight > topHeight + height) AJS.$(this.shadowRoot.querySelector('.attachments-inlineDialogWrapper')).css({ 'top': topHeight, 'left': triggerElement.left }); else {
              AJS.$(this.shadowRoot.querySelector('.attachments-inlineDialogWrapper')).css({ 'top': topHeight - triggerElement.height - 5 - height, 'left': triggerElement.left });
            }
          }
          AJS.$(ev.target).focus();
        }
      }
    }
  }, {
    key: '_closeInlineDialog',
    value: function _closeInlineDialog(ev) {
      if (ev && ev.relatedTarget && ev.relatedTarget.localName === 'a') {
        this.openInlineDialog = true;
      } else {
        this.openInlineDialog = false;
      }
    }
  }, {
    key: 'connectedCallback',
    value: function connectedCallback() {
      _get(GridCell.prototype.__proto__ || Object.getPrototypeOf(GridCell.prototype), 'connectedCallback', this).call(this);
      this._initializer();
    }
  }, {
    key: '_initializer',
    value: function _initializer() {
      var that = this;

      this.attachment = false;
      this.openInlineDialog = false;
      this.singleSelect = false;
      this.multiSelect = false;
      this.checkbox = false;
      this.radio = false;
      this.date = false;
      this.executionsDefect = [];
      this.stepDefect = [];
      this.textarea = false;
      this.textfield = false;
      this.readMode = false;
      this.stepDefects = false;
      this.defect = false;
      this.dropDown = false;
      this.isEditMode = false;
      this.htmlContent = false;
      this.wikiTextarea = false;
      this.openDefectInlineDialog = false;
      this.openStepDefectInlineDialog = false;
      this.defectKeys = [];
      this.remainingDefectList = [];
      this.isDefectRemoved = false;
      this.defectResult = [];
      this.defectLabel = '';
      this.contextPath = '';
      this.defectContextPath = AJS.contextPath();
      this.canAddAttachment = false;
      this.tooltipLabel = '';

      var type = this.datacolumn.type;
      this.rowId = this.datarow.id;
      this.columnId = this.datacolumn.key;
      this.cellId = this.columnId;
      if (this.ispopup == 'true') {
        this.cellId = this.cellId + '-popup';
      }
      this.value = this.datarow[this.datacolumn.key];

      this.valueUrl = '';
      if (this.datacolumn.key == 'issueKey') {
        this.valueUrl = AJS.contextPath() + '/browse/' + this.value;
      } else if (this.datacolumn.key == 'cycleName') {
        this.valueUrl = AJS.contextPath() + "/DisplayCycle.jspa?cycleId=" + this.datarow.cycleId + "&versionId=" + this.datarow.versionId + "&issueKey=" + this.datarow.issueKey;
      } else if (this.datacolumn.key == 'assignee') {
        this.value = this.datarow['assignedToDisplay'];
      }

      if (!(this.value || this.value === 0 )) {
        this.value = '-';
      }

      if (this.datarow.editMode) {
        this.mode = 'edit';
        this.isEditMode = true;
      } else {
        this.mode = 'read';
        this.isEditMode = false;
      }

      if (type === 'HTMLContent') {
        this.htmlContent = true;
        this.$.renderHTML.innerHTML = this.value;
        var imgTags = this.$.renderHTML.querySelectorAll('img');
        if (imgTags.length) {
          var promisesArray = [];
          imgTags.forEach(function (imgtag, index) {
            promisesArray[index] = new Promise(function (resolve, reject) {
              imgtag.onload = function () {
                resolve();
              };
              imgtag.onerror = function () {
                resolve();
              };
            });
          });

          Promise.all(promisesArray).then(function (values) {
            that.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true,detail: { index: that.rowindex } }));
          }).catch(function (reason) {
            that.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true,detail: { index: that.rowindex } }));
          });
        }
      }
      if (type === 'String') {
        if (this.columnId === 'defects' && (this.datarow.stepDefectCount != 0 || this.datarow.totalDefectCount != 0)) {
          var defectsData = this.datarow[this.columnId] || [];
          var length = defectsData.length - 1;
          this.executionsDefect = this.datarow[this.columnId];
          if (this.datarow['stepDefect']) {
            this.stepDefect = this.datarow['stepDefect'];
          }
          if (this.datarow['showPopup']) {
            this.openDefectInlineDialog = true;
          } else {
            this.openDefectInlineDialog = false;
          }
          defectsData.map(function (defect, index) {
            var separator = true;
            if (index === length) {
              separator = false;
            }
            var url = AJS.contextPath() + '/browse/' + defect.key;
            that.defectKeys.push({ value: defect.key, url: url, separator: separator, resolution: defect.resolution });
          });
          this.defect = true;
        } else {
          this.readMode = true;
        }
      } else if (type === 'SELECT_STATUS') {
        this.options = [];
        var statusMap = this.datacolumn.executionSummaries;
        var status;
        if (this.datarow.status) {
          status = this.datarow.status;
        } else {
          status = this.datarow.executionStatus;
        }
        Object.keys(statusMap).map(function (index) {
          var obj = {
            "content": statusMap[index].name,
            "value": statusMap[index].id,
            "selected": statusMap[index].id === Number(status) ? true : false,
            "isStatus": true,
            "color": statusMap[index].color,
            "readOnly": !that.datacolumn.editable
          };
          that.options.push(obj);
        });
        this.dropDown = true;
        this.imgUrl = this.datacolumn.imgUrl;
      } else if (type === 'SINGLE_SELECT') {
        this.options = [];
        this.options = JSON.parse(JSON.stringify(this.datacolumn.options));
        var selectedValues = this.datarow[this.datacolumn.key];
        this.options.map(function (option) {
          if (option.value === selectedValues) {
            option.selected = true;
          } else {
            option.selected = false;
          }
        });
        this.singleSelect = true;
        this.imgUrl = this.datacolumn.imgUrl;
      } else if (type === 'MULTI_SELECT') {
        this.options = [];
        var selectedValues = [];
        this.options = JSON.parse(JSON.stringify(this.datacolumn.options));
        selectedValues = this.datarow[this.datacolumn.key] && this.datarow[this.datacolumn.key].split(',');
        this.options.map(function (option) {
          if (selectedValues && selectedValues.indexOf(option.value) >= 0) {
            option.selected = true;
          } else {
            option.selected = false;
          }
        });
        this.multiSelect = true;
        this.imgUrl = this.datacolumn.imgUrl;
      } else if (type === 'CHECKBOX') {
        this.options = JSON.parse(JSON.stringify(this.datacolumn.options));
        var selectedValues = this.datarow[this.datacolumn.key] || '';
        selectedValues = selectedValues.split(',');
        if (selectedValues.length) {
          this.options.map(function (option) {
            if (selectedValues.indexOf(option.value) >= 0) {
              option.selected = true;
            } else {
              option.selected = false;
            }
          });
        }
        this.checkbox = true;
        this.imgUrl = this.datacolumn.imgUrl;
      } else if (type === 'RADIO_BUTTON') {
        this.options = JSON.parse(JSON.stringify(this.datacolumn.options));
        var selectedValues = this.datarow[this.datacolumn.key];
        this.options.map(function (option) {
          if (option.value === that.datarow[that.datacolumn.key]) {
            option.selected = true;
          } else {
            option.selected = false;
          }
        });
        this.radio = true;
        this.imgUrl = this.datacolumn.imgUrl;
      } else if (type === 'DATE_TIME' || type === 'DATE') {
        this.options = [{ "value": this.datarow[this.datacolumn.key] }];
        this.imgUrl = this.datacolumn.imgUrl;
        this.date = true;
      } else if (type === 'TEXT') {
        this.options = [{ "value": this.datarow[this.datacolumn.editKey], "type": "TEXT", "readValue": this.datarow[this.datacolumn.key], "isHTMLKey": true  }];
        this.textfield = true;
      } else if (type === 'NUMBER') {
        this.options = [{ "value": this.datarow[this.datacolumn.key], "type": "NUMBER" }];
        this.textfield = true;
      } else if (type === 'LARGE_TEXT') {
        this.options = [{ "value": this.datarow[this.datacolumn.editKey], "type": "textarea", "readValue": this.datarow[this.datacolumn.key], "isHTMLKey": true }];
        this.textarea = true;
      } else if (type === 'WIKI_LARGE_TEXT') {
        this.options = [{ 'key': this.datacolumn.key, "editModeValue": this.datarow[this.datacolumn.key], "readModeValue": this.datarow[this.datacolumn.editKey], "wikiBaseUrl": this.datacolumn['wikiBaseUrl'], "wikiPreview": this.datacolumn['wikiPreview'], "wikiHelp": this.datacolumn['wikiHelp'], "wikiHelpUrl": this.datacolumn['wikiHelpUrl'] }];
        this.wikiTextarea = true;
      } else if (type === 'ATTACHMENTS') {
        var map = this.datarow[this.datacolumn.key] || [];
        this.canAddAttachment = this.datacolumn.canAddAttachment;
        this.tooltipLabel = this.datacolumn.tooltipLabel;
        if (this.datacolumn.fetchAttachment) {
          this.fetchAttachment = true;

        }
        if (this.datarow.showAttachmentPopover && this.isFetchAttachment) {
          this.openInlineDialog = !this.openInlineDialog;
          if (this.isGrid) {
            var triggerElement = this.shadowRoot.querySelector('.attachmentTrigger').getBoundingClientRect();
            var viewportHeight = window.innerHeight;
            var height = this.shadowRoot.querySelector('.attachments-inlineDialogWrapper').clientHeight;
            var topHeight = triggerElement.top + triggerElement.height + 2;
            if (viewportHeight > topHeight + height) AJS.$(this.shadowRoot.querySelector('.attachments-inlineDialogWrapper')).css({ 'top': topHeight, 'left': triggerElement.left }); else {
              AJS.$(this.shadowRoot.querySelector('.attachments-inlineDialogWrapper')).css({ 'top': topHeight - triggerElement.height - 5 - height, 'left': triggerElement.left });
            }
            AJS.$(triggerElement).focus();
          }
          this.isFetchAttachment = false;
        }
        if (map.length > 0 && this.datacolumn.fetchAttachment) {
          this.datarow.stepResultAttachmentCount = map.length;
        }
        if (this.datarow.stepResultAttachmentCount > 0 && this.datacolumn.fetchAttachment) {
          this.attachmentsObject = {
            isEnable: true,
            length: this.datarow.stepResultAttachmentCount,
            attachmentsList: map,
            baseUrl: this.datacolumn.baseUrl
          };
        }
        else if (map.length) {
          this.attachmentsObject = {
            isEnable: true,
            length: map.length,
            attachmentsList: map,
            baseUrl: this.datacolumn.baseUrl
          };
        } else {
          this.attachmentsObject = {
            isEnable: false,
            length: 0,
            attachmentsList: map,
            baseUrl: this.datacolumn.baseUrl
          };
        }
        this.attachment = true;
      } else if (type === 'STEP_DEFECTS') {
        this.stepLevelDefects = this.datarow[this.datacolumn.key] || [];
        this.stepDefectsLength = this.stepLevelDefects.length;
        this.stepLevelDefects.map(function (defect) {
          that.remainingDefectList.push(defect.key);
        });
        this.stepDefects = true;
        this.contextPath = this.datacolumn['contextPath'] || '';
        this.imgurlAddDefect = this.datacolumn['imgurlAddDefect'];
        this.imgurlCreateIssue = this.datacolumn['imgurlCreateIssue'];
        this.removeDefectImg = this.datacolumn['imgRemoveDefect'];
        if (this.datarow['showDefectsResult']) {
          var result = this.datarow['stepLevelDefect'].sections[0];
          this.defectResult = result.issues;
          this.contextPath = AJS.contextPath();
          if (this.defectResult.length) {
            this.defectLabel = result.label + ' ' + result.sub;
            this.showDefault = false;
          } else {
            this.defectLabel = result.label + ' Showing 0 of 0 result';
            this.showDefault = true;
          }
          this.showDefectSearchResult = true;
        } else {
          this.defectResult = [];
          this.defectLabel = '';
          this.openDefectSearchBox = false;
          this.showDefectSearchResult = false;
        }
      }
      if (that.updaterowheight === 'true') {
        setTimeout(function () {
          that.dispatchEvent(new CustomEvent('adjustrowheight', { bubbles: true, composed: true, detail: { index: that.rowindex, loader: that.loader == 'true' ? true : false } }));
        }, 0);
      }
    }
  }, {
    key: '_checkBlur',
    value: function _checkBlur(ev) {
      if (ev.relatedTarget && (ev.relatedTarget.className.indexOf('defects-container') >= 0 || ev.relatedTarget.className.indexOf('content') >= 0)) {
        this.shadowRoot.querySelector('.setDefectDialogFocus').focus();
        this.openStepDefectInlineDialog = true;
      } else {
        this.openStepDefectInlineDialog = false;
        if (this.isDefectRemoved) this.dispatchEvent(new CustomEvent('submitcell', { detail: { value: this.remainingDefectList, key: 'stepDefect', rowId: this.rowId, cellKey: 'defectList' }, bubbles: true, composed: true }));
      }
    }
  }, {
    key: '_checkRender',
    value: function _checkRender(bool) {
      return bool ? 'showValue' : 'editValue';
    }
  }, {
    key: '_strikeThrough',
    value: function _strikeThrough(status) {
      return status === 'Done' ? 'strikeThrough' : 'notDone';
    }
  }, {
    key: '_removeDefect',
    value: function _removeDefect(ev) {
      this.remainingDefectList = this.remainingDefectList.filter(function (defect) {
        return defect !== ev.currentTarget.dataKeyid;
      });
      this.isDefectRemoved = true;
      AJS.$(ev.currentTarget).parents('.defectsList').css("display", "none");
    }
  }, {
    key: '_openStepLevelDefectDialog',
    value: function _openStepLevelDefectDialog() {
      var defectsList = this.shadowRoot.querySelectorAll('.defectsList');
      if (defectsList.length) {
        this.shadowRoot.querySelectorAll('.defectsList').forEach(function (defect) {
          AJS.$(defect).css("display", "flex");
        });
        this.openStepDefectInlineDialog = !this.openStepDefectInlineDialog;
      }
    }
  }, {
    key: '_checkStepLevelDefectDialog',
    value: function _checkStepLevelDefectDialog(bool) {
      var that = this;
      if (that.isGrid && bool) {
        var triggerElement = that.shadowRoot.querySelector('.dropDown-wrapper').getBoundingClientRect();
        var viewportHeight = window.innerHeight;
        var viewportWidth = window.innerWidth;
        var height = that.shadowRoot.querySelector('.stepDefects-inlineDialogWrapper').clientHeight;
        var topHeight = triggerElement.top + triggerElement.height + 2;
        if (viewportHeight > topHeight + 200) {
          AJS.$(that.shadowRoot.querySelector('.stepDefects-inlineDialogWrapper')).css({ 'top': topHeight, 'bottom': 'auto', 'left': triggerElement.left });
        } else {
          AJS.$(that.shadowRoot.querySelector('.stepDefects-inlineDialogWrapper')).css({ 'top': 'auto', 'bottom': viewportHeight - topHeight + triggerElement.height + 5, 'left': triggerElement.left });
        }

        if (viewportWidth < triggerElement.left + 350) {
          AJS.$(that.shadowRoot.querySelector('.stepDefects-inlineDialogWrapper')).css({ 'left': triggerElement.left + triggerElement.width - 422 });
        }
      }
      return bool ? "open" : "close";
    }
  }, {
    key: '_triggerStepLevelDefectDropDown',
    value: function _triggerStepLevelDefectDropDown(ev) {
     if (ev && ev.relatedTarget && (ev.relatedTarget.tagName == 'DIV' && ev.relatedTarget.className.indexOf('stopBlur') > -1) ) {
          ev.target.focus();
      } else {
        this.openDefectSearchBox = !this.openDefectSearchBox;
        if (!this.openDefectSearchBox) {
          this.defectResult = [];
          this.defectLabel = '';
          this.shadowRoot.querySelector('.ajax-input').value = '';
          this.showDefectSearchResult = false;
        }
      }
    }
  }, {
    key: '_defectSearchBox',
    value: function _defectSearchBox(bool) {
      var that = this;
      if (that.isGrid && bool) {
        var triggerElement = that.shadowRoot.querySelector('.dropDown-wrapper').getBoundingClientRect();
        var viewportHeight = window.innerHeight;
        var height = that.shadowRoot.querySelector('.defectsSearchBox').clientHeight;
        var topHeight = triggerElement.top + triggerElement.height + 2;
        if (viewportHeight > topHeight + 200) AJS.$(that.shadowRoot.querySelector('.defectsSearchBox')).css({ 'top': topHeight, 'bottom': 'auto', 'left': triggerElement.left }); else {
          AJS.$(that.shadowRoot.querySelector('.defectsSearchBox')).css({ 'top': 'auto', 'bottom': viewportHeight - topHeight + triggerElement.height + 5, 'left': triggerElement.left });
        }
        setTimeout(function () {
          that.shadowRoot.querySelector('.ajax-input').focus();
        }, 100);
      }
      return bool ? "open" : "close";
    }
  }, {
    key: '_defectSearchResult',
    value: function _defectSearchResult(bool) {
      return bool ? "open" : "close";
    }
  }, {
    key: '_fetchDefects',
    value: function _fetchDefects(ev) {
      ev.stopPropagation();
      ev.stopImmediatePropagation();
      var value = ev.target.value + ev.key;
      var obj = {
        id: this.rowId,
        query: value,
        defectsPopup: true,
        defects: this.datarow.defects
      };
      this.defectValue = value;
      this.dispatchEvent(new CustomEvent('fetchdefect', { detail: obj, bubbles: true, composed: true }));
    }
  }, {
      key: '_enterDefects',
      value: function _enterDefects(ev) {
        ev.stopPropagation();
        ev.stopImmediatePropagation();
        var value = ev.target.value + ev.clipboardData.getData('text');;
        var obj = {
          id: this.rowId,
          query: value,
          defectsPopup: true
        };
        this.defectValue = value;
        this.dispatchEvent(new CustomEvent('fetchdefect', { detail: obj, bubbles: true, composed: true }));
      }
    }, {
    key: '_defectSelected',
    value: function _defectSelected(ev) {
      var defectList = [];
      var wasAlreadyAdded = true;
      this.stepLevelDefects.map(function (defect) {
        defectList.push(defect.key);
      });
      if (!ev.currentTarget.dataCreatedefect) {
        if (defectList.indexOf(ev.currentTarget.dataDefectid) == -1) {
          defectList.push(ev.currentTarget.dataDefectid);
          wasAlreadyAdded = false;
        }
      } else {
        defectList.push(this.defectValue);
      }
      var obj = {
        id: this.rowId,
        query: '',
        defectsPopup: false,
        wasAlreadyAdded: wasAlreadyAdded
      };
      this.defectValue = '';
      this.dispatchEvent(new CustomEvent('fetchdefect', { detail: obj, bubbles: true, composed: true }));
      this.dispatchEvent(new CustomEvent('submitcell', { detail: { wasAlreadyAdded: wasAlreadyAdded, value: defectList, key: 'stepDefect', rowId: this.rowId, cellKey: 'defectList' }, bubbles: true, composed: true }));
    }
  }, {
    key: '_defectMouseEnter',
    value: function _defectMouseEnter(e) {
      e.stopPropagation();
      e.stopImmediatePropagation();
      e.preventDefault();
      var obj = {
        id: this.rowId,
        defectsPopup: true
      };
      this.dispatchEvent(new CustomEvent('defecthover', { detail: obj, bubbles: true, composed: true }));
    }
  }, {
    key: '_defectMouseLeave',
    value: function _defectMouseLeave(e) {
      e.stopPropagation();
      e.preventDefault();
      var obj = {
        id: this.rowId,
        defectsPopup: false
      };
      this.dispatchEvent(new CustomEvent('defecthover', { detail: obj, bubbles: true, composed: true }));
    }
  }, {
    key: '_dateChooser',
    value: function _dateChooser(e) {
      var obj = {
        'type': this.datacolumn.type,
        'rowId': this.rowId,
        'cellKey': this.columnId,
        'value': e.detail.value,
        'event': e.detail.event,
        'isEditMode': this.isEditMode,
        'onlyUpdateValue': e.detail.onlyUpdateValue
      };
      this.dispatchEvent(new CustomEvent('datepicker', { detail: obj, bubbles: true, composed: true }));
    }
  }, {
    key: '_submit',
    value: function _submit(e) {
      var value = '';
      if (e.detail.type === 'CHECKBOX' || e.detail.type === 'MULTI_SELECT') {
        var valueContent = e.detail.value;
        value = valueContent.toString();
      } else {
        value = e.detail.value;
      }
      if (this.mode === 'read') {
        this.dispatchEvent(new CustomEvent('submitcell', { detail: { value: value, contentValue: e.detail.contentValue, rowId: this.rowId, cellKey: this.columnId }, bubbles: true, composed: true }));
      } else {
        this.dispatchEvent(new CustomEvent('savecellvalue', { detail: { value: value, contentValue: e.detail.contentValue, rowId: this.rowId, cellKey: this.columnId }, bubbles: true, composed: true }));
      }
    }
  }, {
    key: '_addAttachments',
    value: function _addAttachments(e) {
      this.dispatchEvent(new CustomEvent('submitcell', { detail: { key: 'addAttachment', rowId: this.rowId, cellKey: this.columnId }, bubbles: true, composed: true }));
    }
  }, {
    key: '_createDefect',
    value: function _createDefect(e) {
      this.dispatchEvent(new CustomEvent('submitcell', { detail: { key: 'createDefect', rowId: this.rowId, cellKey: this.columnId }, bubbles: true, composed: true }));
    }
  }, {
    key: '_defectPicker',
    value: function _defectPicker(e) {
      this.dispatchEvent(new CustomEvent('submitcell', { detail: { key: 'defectPicker', rowId: this.rowId, cellKey: this.columnId }, bubbles: true, composed: true }));
    }
  }, {
    key: '_deleteAttachment',
    value: function _deleteAttachment(e) {
      e.stopImmediatePropagation();
      e.stopPropagation();
      e.preventDefault();
      this.dispatchEvent(new CustomEvent('submitcell', { detail: { key: 'deleteAttachment', value: e.currentTarget.val, rowId: this.rowId, cellKey: this.columnId }, bubbles: true, composed: true }));
      this.openInlineDialog = false;
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        datarow: {
          type: Object,
          value: {},
          observer: 'check'
        },
        datacolumn: {
          type: Object,
          value: {},
          observer: 'check'
        },
        inlineedit: {
          type: Boolean,
          value: true
        },
        rowindex: {
          type: String
        },
        ispopup: {
          type: String
        },
        loader: {
          type: String
        }
      };
    }
  }]);

  return GridCell;
}(Polymer.Element);

customElements.define(GridCell.is, GridCell);

var GridComponent = function (_Polymer$Element11) {
  _inherits(GridComponent, _Polymer$Element11);

  _createClass(GridComponent, null, [{
    key: 'is',
    get: function get() {
      return "grid-component";
    }
  }]);

  function GridComponent() {
    _classCallCheck(this, GridComponent);

    var _this11 = _possibleConstructorReturn(this, (GridComponent.__proto__ || Object.getPrototypeOf(GridComponent)).call(this));

    _this11.testConfig = {};
    _this11.freezedColumns = [];
    _this11.unfreezedColumns = [];
    _this11.rowsHeights = [];
    _this11.draggableRowHeight;
    _this11.dragDirection;
    _this11.selectedRowId = '';
    _this11.selectedCells = [];
    _this11.rowsSelection = [];
    _this11.saveRowValues = {};
    _this11.draggedOver;
    _this11.prevPosition;
    _this11.maxFreezed = 2;
    _this11.canFreeze = false;
    _this11.isGrid = true;
    _this11.showColumnChooser = false;
    _this11.submitDisabled = false;
    _this11.columnChooserValues = [];
    _this11.openExportDropDown = false;
    _this11.setFocus = false;
    _this11.columnChooserArr = [];
    _this11.selectAll = false;
    _this11.initialCount = 10;
    _this11.columnChooserTrigger = '';
    this.dragStart = false;
    return _this11;
  }

  _createClass(GridComponent, [{
    key: 'draggingStart',
    value: function draggingStart(ev) {
      ev.preventDefault();
      ev.stopPropagation();
      ev.stopImmediatePropagation();
      var event = ev.currentTarget;
      var rowId = event.dataRowid;
      this.selectedCells = [];
      var that = this;
      this.draggableRowHeight = event.clientHeight + 10;
      this.shadowRoot.querySelectorAll('.table-row').forEach(function (row) {
        row.removeEventListener("mouseenter", function(){}, false);
        if (row.dataRowid === rowId) {
          that.selectedCells.push(row);
          AJS.$(row).addClass('selected');
          AJS.$(row).after('<div class="draggable table-row" style="height:' + that.draggableRowHeight + 'px' + '"></div>');
        } else {
          AJS.$(row).removeClass('selected');
        }
      });
      this.dragStart = true;
      document.removeEventListener('mouseup',function(){},true);
      document.addEventListener('mouseup', that.draggingStop.bind(that));
      document.removeEventListener('mousemove', function () { }, true);
      document.addEventListener('mousemove', that.dragElements.bind(that));

      // AJS.$('body')[0].addEventListener('dragover', this.possibleTarget.bind(this));
      // AJS.$('body')[0].addEventListener('drop', this.draggingStop.bind(this));
    }
  }, {
    key: 'possibleTarget',
    value: function possibleTarget(ev) {
      if(this.dragStart) {
        ev.preventDefault();
        ev.stopPropagation();
        ev.stopImmediatePropagation();
        var event = ev.currentTarget;
        if (event !== this.prevTarget || this.dragDirection !== this.prevDragDirection) {
        if (event.className.indexOf('draggable') >= 0) {
          this.draggedOver = event;
        } else {
          var rowId = event.dataRowid;
          // var parent = AJS.$(event).parents('.table-row');
          var index = AJS.$(event).index()
          if (index - 1 !== this.testConfig.row.length) {
            this.shadowRoot.querySelectorAll('.draggable.table-row').forEach(function (row) {
              row.parentElement.removeChild(row);
            });

            index = AJS.$(event).index()


            var freezedColumnRow = AJS.$(this.$.freezeColumns).find('.table-row')[index];
            var unfreezedColumnRow = AJS.$(this.$.unfreezeColumns).find('.table-row')[index];
            var actionColumnRow = AJS.$(this.$.gridActions).find('.table-row')[index];
            var draggableColumnRow = AJS.$(this.$.draggableColumn).find('.table-row')[index];
            var gridSelectionRow = AJS.$(this.$.gridSelection).find('.table-row')[index];

            if (this.dragDirection) {
              AJS.$(freezedColumnRow).after('<div class="draggable table-row" style="height:' + this.draggableRowHeight + 'px' + '"></div>');
              AJS.$(unfreezedColumnRow).after('<div class="draggable table-row" style="height:' + this.draggableRowHeight + 'px' + '"></div>');
              AJS.$(actionColumnRow).after('<div class="draggable table-row"  style="height:' + this.draggableRowHeight + 'px' + '"></div>');
              AJS.$(draggableColumnRow).after('<div class="draggable table-row" style="height:' + this.draggableRowHeight + 'px' + '"></div>');
              AJS.$(gridSelectionRow).after('<div class="draggable table-row" style="height:' + this.draggableRowHeight + 'px' + '"></div>');
            } else {
              AJS.$(freezedColumnRow).before('<div class="draggable table-row"  style="height:' + this.draggableRowHeight + 'px' + '"></div>');
              AJS.$(unfreezedColumnRow).before('<div class="draggable table-row"  style="height:' + this.draggableRowHeight + 'px' + '"></div>');
              AJS.$(actionColumnRow).before('<div class="draggable table-row" style="height:' + this.draggableRowHeight + 'px' + '"></div>');
              AJS.$(draggableColumnRow).before('<div class="draggable table-row" style="height:' + this.draggableRowHeight + 'px' + '"></div>');
              AJS.$(gridSelectionRow).before('<div class="draggable table-row" style="height:' + this.draggableRowHeight + 'px' + '"></div>');
            }
          }
        }
      }
      this.prevTarget = event;
      this.prevDragDirection = this.dragDirection;
    }
    }
    }, {
      key: 'dragEnd',
      value: function dragEnd(ev) {
        this.selectedCells.map(function (row) {
          var p = AJS.$(row).position();
          var left = p.left;
          // AJS.$(row).removeClass('selected');
          // if(AJS.$(row).parents('#freezeColumns').length) {
          //   AJS.$(that.$.freezeColumns).find('.draggable.table-row').after(row);
          // } else if(AJS.$(row).parents('#unfreezeColumns').length) {
          //   AJS.$(that.$.unfreezeColumns).find('.draggable.table-row').after(row);
          // } else if(AJS.$(row).parents('#gridActions').length) {
          //   AJS.$(that.$.gridActions).find('.draggable.table-row').after(row);
          // } else if(AJS.$(row).parents('#draggableColumn').length) {
          //   AJS.$(that.$.draggableColumn).find('.draggable.table-row').after(row);
          // } else if(AJS.$(row).parents('#gridSelection').length) {
          //   AJS.$(that.$.gridSelection).find('.draggable.table-row').after(row);
          // }
          AJS.$(row).css({ 'width': 'auto', 'top': "auto", "position": "static", "z-index": "0", "left": "auto", "pointer-events": "auto" });
        });
        let that = this;
        AJS.$(this.$.gridActions).find('.draggable.table-row').each(function (index) {
          var freezedColumnRow = AJS.$(that.$.freezeColumns).find('.draggable.table-row').remove();
          var unfreezedColumnRow = AJS.$(that.$.unfreezeColumns).find('.draggable.table-row').remove();
          var actionColumnRow = AJS.$(that.$.gridActions).find('.draggable.table-row').remove();
          var draggableColumnRow = AJS.$(that.$.draggableColumn).find('.draggable.table-row').remove();
          var gridSelectionRow = AJS.$(that.$.gridSelection).find('.draggable.table-row').remove();
        });
        this.dragStart = false;
      }
    }, {
    key: 'dragElements',
    value: function dragElements(ev) {
      if (this.dragStart) {
      ev.preventDefault();
      ev.stopImmediatePropagation();
      ev.stopPropagation();
      var event = ev;
      var rowId = event.currentTarget.dataRowid;
      var top = event.clientY - AJS.$(this.shadowRoot.querySelectorAll('.table-wrapper'))[0].getBoundingClientRect().top - 18;
      if (event.screenY !== 0 && event.screenX !== 0) {
        this.selectedCells.map(function (row) {
          var p = AJS.$(row).position();
          var offset = top;
          AJS.$(row).css({ 'top': offset, "position": "absolute", "z-index": "3000", "left": 'auto', "pointer-events": " none", "width": AJS.$(row)[0].clientWidth });
        });
      }
      if (this.prevPosition) {
        if (this.prevPosition - event.clientY > 0) {
          this.dragDirection = 0;
          this.prevDrag = 0;
        } else if (this.prevPosition - event.clientY == 0){
          this.dragDirection = this.prevDrag;
        }
        else {
          this.dragDirection = 1;
          this.prevDrag = 1;
        }
      }
      this.prevPosition = event.clientY;
      var scrollParent = AJS.$(this.shadowRoot.querySelector('.scrollWrapper'));

      if (scrollParent[0].clientHeight + scrollParent[0].scrollTop < event.clientY - (scrollParent.offset().top - scrollParent[0].scrollTop)) {
        if (scrollParent[0].scrollTop >= scrollParent[0].scrollHeight - scrollParent[0].clientHeight) scrollParent.scrollTop(scrollParent[0].scrollHeight - scrollParent[0].clientHeight); else scrollParent.scrollTop(event.clientY - (scrollParent.offset().top - scrollParent[0].scrollTop) - scrollParent[0].clientHeight);
      } else if (scrollParent[0].scrollTop > event.clientY - (scrollParent.offset().top - scrollParent[0].scrollTop)) {
        scrollParent.scrollTop(event.clientY - (scrollParent.offset().top - scrollParent[0].scrollTop));
      }
    }
    }
  }, {
    key: 'draggingStop',
    value: function draggingStop(ev) {
      if (this.dragStart) {
        ev.preventDefault();
        ev.stopImmediatePropagation();
        ev.stopPropagation();
        var that = this;
        var rowId = void 0;
        var selectedRowIndex = AJS.$(this.$.gridActions).find('.selected.table-row').index();

        this.selectedCells.map(function (row) {
          var p = AJS.$(row).position();
          var left = p.left;
          rowId = row.dataRowid;
          // AJS.$(row).removeClass('selected');
          // if(AJS.$(row).parents('#freezeColumns').length) {
          //   AJS.$(that.$.freezeColumns).find('.draggable.table-row').after(row);
          // } else if(AJS.$(row).parents('#unfreezeColumns').length) {
          //   AJS.$(that.$.unfreezeColumns).find('.draggable.table-row').after(row);
          // } else if(AJS.$(row).parents('#gridActions').length) {
          //   AJS.$(that.$.gridActions).find('.draggable.table-row').after(row);
          // } else if(AJS.$(row).parents('#draggableColumn').length) {
          //   AJS.$(that.$.draggableColumn).find('.draggable.table-row').after(row);
          // } else if(AJS.$(row).parents('#gridSelection').length) {
          //   AJS.$(that.$.gridSelection).find('.draggable.table-row').after(row);        // }
          });

          var selectedRow = AJS.$(this.$.gridActions).find('.draggable.table-row');
          var index = selectedRow.index();
          var afterRowId = AJS.$(this.$.draggableColumn).find('.table-row')[index + 1].dataRowid;

        var position;
        if (index === this.testConfig.row.length || afterRowId === -1) {
          position = 'First';
        } else {
          position = afterRowId;
        }

        var obj = {
          position: position,
          id: rowId,
          customEvent: 'movestep'
        };
        this.selectedRowId = rowId;
        this.dragEnd();
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
      }
    }
  }, {
    key: '_defectHover',
    value: function _defectHover(ev) {
      ev.stopImmediatePropagation();
      this.dispatchEvent(new CustomEvent('defecthover', { detail: ev.detail, bubbles: true, composed: true }));
    }
  }, {
    key: 'selectedClass',
    value: function selectedClass(id1, id2) {
      return this.testConfig.highlightSelectedRows ? id1 === id2 ? 'selected' : '' : '';
    }
  }, {
    key: 'updateRowHeight',
    value: function updateRowHeight(index, obj) {
      return this[obj].length - 1 == index ? 'true' : 'false';
    }
  }, {
    key: '_configUpdate',
    value: function _configUpdate() {
      var dummyConfig = JSON.parse(this.config);
      var that = this;

      if (this.testConfig.head && !(dummyConfig.hasOwnProperty('freezeColumns') && dummyConfig.freezeColumns !== this.testConfig.freezeColumns)) {
        dummyConfig.head.map(function (dummy) {
          that.testConfig.head.map(function (config) {
            if (dummy.key === config.key) {
              dummy.isFreeze = config.isFreeze;
            }
          });
        });
      }
      this.testConfig = dummyConfig;
      if (this.testConfig.isPopup) {
        this.isPopup = 'true';
      } else {
        this.isPopup = 'false';
      }

      this.initialCount = this.testConfig.initialCount;
      if (this.testConfig.selectedRowId) {
        this.selectedRowId = this.testConfig.selectedRowId;
      }

      if (Object.keys(this.testConfig).length) {
        this._createGrid();

        this.rowsSelection = [];
        if (this.testConfig.resetSelectAll) {
          this.selectAll = false;
        }
        var allSelected = true;
        if (this.testConfig.row.length === 0) {
          allSelected = false;
        }
        this.testConfig.row.map(function (row) {
          if (that.testConfig.hasOwnProperty("checkedRowId")) {
            if (that.testConfig.checkedRowId.indexOf(row.id) >= 0) {
              that.rowsSelection.push({ rowId: row.id, selected: true, permission: row.permission });
            } else {
              that.rowsSelection.push({ rowId: row.id, selected: false, permission: row.permission });
              allSelected = false;
            }
          } else {
            that.rowsSelection.push({ rowId: row.id, selected: false, permission: row.permission });
            allSelected = false;
          }
        });
        if (allSelected && that.testConfig.hasBulkActions) {
          setTimeout(function () {
            var trigger = AJS.$(that.shadowRoot.querySelector('.bulkAction-container.selectRows'))[0];

            that.testConfig.bulkActions.map(function (action, index) {
              var path = 'testConfig.bulkActions.' + index.toString(),
                imgSrcChecked = action.imgSrcChecked,
                imgSrcUnchecked = action.imgSrc;
              if (action.customEvent == 'selectRows') {
                that.set(path, { actionName: action.actionName, customEvent: action.customEvent, disabled: action.disabled, imgSrc: imgSrcChecked, imgSrcChecked: imgSrcUnchecked });
                !trigger.classList.contains('allSelected') && trigger.classList.add('allSelected');
              }
            });
          }, 0);
        }
      }
    }
  }, {
    key: '_createGrid',
    value: function _createGrid() {
      var _this12 = this;

      var that = this;
      var count = 0;
      var rowsCount = that.testConfig.row.length;

      if (this.testConfig.updateSaveValues) {
        this.saveRowValues = {};
      } else if (this.testConfig.row[rowsCount - 1] && this.testConfig.row[rowsCount - 1].editMode) {
        this.testConfig.head.map(function (column) {
          if ((column.type === 'DATE' || column.type === 'DATE_TIME') && that.testConfig.row[rowsCount - 1][column.key] !== '') {
            if (that.saveRowValues[that.testConfig.row[rowsCount - 1].id]) {
              that.saveRowValues[that.testConfig.row[rowsCount - 1].id][column.key] = {
                selectedOptions: '',
                value: that.testConfig.row[rowsCount - 1][column.key]
              };
            } else {
              that.saveRowValues[that.testConfig.row[rowsCount - 1].id] = {};
              that.saveRowValues[that.testConfig.row[rowsCount - 1].id][column.key] = {
                selectedOptions: '',
                value: that.testConfig.row[rowsCount - 1][column.key]
              };
            }
          }
        });
      }

      this.columnChooserValues = [];
      this.freezedColumns = this.testConfig.head.filter(function (cell) {
        if (cell.key !== "issueKey" && cell.key !== "orderId") {
          _this12.columnChooserValues.push({ key: cell.key, displayName: cell.displayName, isVisible: cell.isVisible });
        }
        that.columnChooserArr = JSON.parse(JSON.stringify(that.columnChooserValues));
        if (cell.isFreeze) {
          count++;
          if (count > _this12.maxFreezed) {
            return !cell.isFreeze;
          }
        }
        return cell.isFreeze;
      });
      if (this.freezedColumns.length < this.maxFreezed) {
        this.canFreeze = true;
      } else {
        this.canFreeze = false;
      }
      this.unfreezedColumns = this.testConfig.head.filter(function (cell) {
        return that.freezedColumns.indexOf(cell) === -1;
      });
      this.loader = 'true';
    }
  }, {
    key: '_adjustRowHeight',
    value: function _adjustRowHeight(ev) {
      ev.stopImmediatePropagation();
      ev.stopPropagation();
      ev.preventDefault();
      var index = ev.detail.index + 1;
      if (ev.detail.loader && this.testConfig.showLoader && index === 1) {
        this.$.loadingIndicator.style.display = 'flex';
      }
      var that = this;
      var gridActions = AJS.$(that.$.gridActions).find('.table-row')[index];
      var freezedColumnRow = AJS.$(that.$.freezeColumns).find('.table-row')[index];
      var unfreezedColumnRow = AJS.$(that.$.unfreezeColumns).find('.table-row')[index];
      var gridSelectionRow = AJS.$(that.$.gridSelection).find('.table-row')[index];
      var draggableColumnRow = AJS.$(that.$.draggableColumn).find('.table-row')[index];

      gridActions && (gridActions.style.height = "auto");
      freezedColumnRow && (freezedColumnRow.style.height = "auto");
      unfreezedColumnRow && (unfreezedColumnRow.style.height = "auto");
      draggableColumnRow && (draggableColumnRow.style.height = "auto");
      gridSelectionRow && (gridSelectionRow.style.height = "auto");
      var height = 45;

      if (freezedColumnRow && height < freezedColumnRow.clientHeight) {
        height = freezedColumnRow.clientHeight;
      }
      if (unfreezedColumnRow && height < unfreezedColumnRow.clientHeight) {
        height = unfreezedColumnRow.clientHeight;
      }

      gridActions && (gridActions.style.height = height + 'px');
      freezedColumnRow && (freezedColumnRow.style.height = height + 'px');
      unfreezedColumnRow && (unfreezedColumnRow.style.height = height + 'px');
      draggableColumnRow && (draggableColumnRow.style.height = height + 'px');
      gridSelectionRow && (gridSelectionRow.style.height = height + 'px');

      if (ev.detail.loader && this.testConfig.showLoader && (index === this.testConfig.row.length - 1 || index === this.testConfig.row.length)) {
        this.$.loadingIndicator.style.display = 'none';
      }
      if(index === this.testConfig.row.length - 1 && this.testConfig.gridComponentPage == 'issueView') {
        var freezeColumnEle = this.$.freezeColumns,
            unfreezeColumnEle = this.$.unfreezeColumns,
            freezeColumnsWidth, unfreezeColumnsWidth, unfreezeColumnsHeight;
            unfreezeColumnsHeight = unfreezeColumnEle.getBoundingClientRect().height;
        if(freezeColumnEle && unfreezeColumnEle && unfreezeColumnsHeight > 350){
          freezeColumnsWidth = freezeColumnEle.getBoundingClientRect().width;
          unfreezeColumnsWidth = unfreezeColumnEle.scrollWidth;
          console.log('freeze unfreeze columns',freezeColumnsWidth, unfreezeColumnsWidth);
          this.dispatchEvent(new CustomEvent('emitContainerDimensions', { detail: { freezeColumnsWidth: freezeColumnsWidth, unfreezeColumnsWidth: unfreezeColumnsWidth}, bubbles: true, composed: true }));

        }

      }
      //  THIS IS FOR SETTING OF THE SCROLL BAR BACK TO ITS POSITION. IT IS WORKING NOW, SO COMMENTING IT OFF.
      // if (gridScrollingValue != 0) {
      //   var tempGridScrollingValue = gridScrollingValue;
      //   gridScrollingValue = 0;
      //   var that = this;
      //   setTimeout(function () {
      //     AJS.$(that.$.unfreezeColumns)[0].scrollLeft = tempGridScrollingValue;
      //   }, 2000);
      // }
    }
  }, {
    key: 'conditionalClass',
    value: function conditionalClass(isVisible) {
      if (!isVisible) {
        return 'hide';
      } else {
        return '';
      }
    }
  }, {
    key: '_handleTap',
    value: function _handleTap(e) {
      var targetObj = JSON.parse(e.target.dataset.foo);
      var testConfig_clone = this.testConfig;
      for (var i = 0; i < testConfig_clone.head.length; i++) {
        if (targetObj.key == testConfig_clone.head[i].key) {
          testConfig_clone.head[i].isVisible = e.target.checked;
          break;
        }
      }
      var that = this;
      this.freezedColumns.map(function (column, index) {
        var path = 'freezeColumns.' + index.toString();
        if (column.key === targetObj.key) {
          column['isVisible'] = e.target.checked;
          that.set(path, column);
        }
      });

      this.unfreezedColumns.map(function (column, index) {
        var path = 'unfreezedColumns.' + index.toString();
        if (column.key === targetObj.key) {
          column['isVisible'] = e.target.checked;
          that.set(path, column);
        }
      });

      // this.set('testConfig.head[' +i+ '].isVisible', testConfig_clone.head[i].isVisible);
      this.set('testConfig.head.' + i + '.isVisible', testConfig_clone.head[i].isVisible);
      // console.log(this.get('testConfig.head.' +i+ '.isVisible'));
      // this._createGrid();
      // for(var i=0; i<this.testConfig.head.length; i++){
      //   if(targetObj.key == this.testConfig.head[i].key){
      //     this.setProperties('testConfig.head['+i+'].isVisible', {'isVisible':  e.target.checked});
      //     break;
      //   }
      // }
    }
  }, {
    key: '_toggleFreeze',
    value: function _toggleFreeze(e) {
      var columnId = AJS.$(e.currentTarget).parents('.cell-wrapper')[0].dataColumnid;
      var element = AJS.$(e.currentTarget);
      if (element.hasClass('selected')) {
        element.removeClass('selected');
        this.testConfig.head.map(function (cell) {
          if (cell.key === columnId) cell.isFreeze = false;
        });
      } else {
        element.addClass('selected');
        this.testConfig.head.map(function (cell) {
          if (cell.key === columnId) cell.isFreeze = true;
        });
      }
      this._createGrid();
      this.dispatchEvent(new CustomEvent('freezetoggle', { detail: { testConfig: this.testConfig }, bubbles: true, composed: true }));
    }
  }, {
    key: '_triggerExportDropdown',
    value: function _triggerExportDropdown(ev) {
      this.openExportDropDown = !this.openExportDropDown;
    }
  }, {
    key: '_closeInlineDialog',
    value: function _closeInlineDialog() {
      this.openExportDropDown = false;
    }
  }, {
    key: '_triggerEvent',
    value: function _triggerEvent(ev) {
      ev.preventDefault();
      ev.stopPropagation();
      var element = AJS.$(ev.currentTarget)[0];
      var selectedRow = {};
      this.testConfig.row.map(function (row) {
        if (row.id === element.dataRowid) {
          selectedRow = row;
        }
      });
      if (element.dataCustomevent === 'largeview') {
        var obj = {
          actionName: element.dataEventname,
          customEvent: element.dataCustomevent
        };
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
      } else if (element.dataCustomevent === 'exportvalue') {
        var obj = {
          exportId: element.dataExportid,
          actionName: element.dataEventname,
          customEvent: element.dataCustomevent
        };
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
        this.openExportDropDown = false;
      } else if (element.dataCustomevent === 'scrollDown') {
        var obj = {
          actionName: element.dataEventname,
          customEvent: element.dataCustomevent
        };
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
        this.setFocus = !this.setFocus;
      } else if (element.dataCustomevent === 'clearvalue') {
        var obj = {
          currentConfig: this.testConfig,
          actionName: element.dataEventname,
          customEvent: element.dataCustomevent
        };
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
      } else if (element.dataCustomevent === 'addstep') {
        var obj = {
          columnsValues: this.saveRowValues,
          actionName: element.dataEventname,
          customEvent: element.dataCustomevent
        };
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
      } else {
        var obj = {
          rowDetail: selectedRow,
          actionName: element.dataEventname,
          customEvent: element.dataCustomevent
        };
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
      }
    }
  }, {
    key: '_attachemntPreview',
    value: function _attachemntPreview(ev) {
      var obj = {
        attachmentArray: ev.detail.attachmentArray,
        selectedAttachment: ev.detail.fileId,
        actionName: 'viewImage',
        customEvent: 'viewImage'
      };
      this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
    }
  }, {
    key: '_checkColumnChooserblur',
    value: function _checkColumnChooserblur(ev) {
      if (ev.relatedTarget && (ev.relatedTarget.localName === 'custom-checkbox' || ev.relatedTarget.className.indexOf('content') >= 0)) {
        this.showColumnChooser = true;
      } else {
        this.columnChooserValues = JSON.parse(JSON.stringify(this.columnChooserArr));
        this.showColumnChooser = false;
      }
    }
  }, {
    key: '_showColumnChooser',
    value: function _showColumnChooser(ev) {
      if (!this.testConfig.columnchooser.disabled) {
        this.showColumnChooser = !this.showColumnChooser;
        this.columnChooserTrigger = ev.currentTarget;
        if (this.isGrid) {
          var triggerElement = ev.currentTarget.getBoundingClientRect();
          var viewportHeight = window.innerHeight;
          var height = this.shadowRoot.querySelector('.column-chooser-cont').clientHeight;
          var topHeight = triggerElement.top;
          if (viewportHeight > topHeight + height) AJS.$(this.shadowRoot.querySelector('.column-chooser-cont')).css({ 'top': topHeight + triggerElement.height + 5, 'left': triggerElement.left - 222 + triggerElement.width }); else {
            AJS.$(this.shadowRoot.querySelector('.column-chooser-cont')).css({ 'top': topHeight + triggerElement.height - height, 'left': triggerElement.left - 222 });
          }
        }
        // this.columnChooserTrigger.focus();
      }
    }
  }, {
    key: '_checkIsGrid',
    value: function _checkIsGrid(bool) {
      return bool ? "isGrid" : "isStandAlone";
    }
  }, {
    key: '_isWorkflowExecutionStatusCompleted',
    value: function _isWorkflowExecutionStatusCompleted(columnKey, row) {
      return row.executionWorkflowStatus == 'COMPLETED' && columnKey == 'status' ? 'disabled' : '';
    }
  }, {
    key: '_openColumnChooserDialog',
    value: function _openColumnChooserDialog(bool) {
      return bool ? "open" : "close";
    }
  }, {
    key: '_datePicker',
    value: function _datePicker(ev) {
      ev.detail['columnsValues'] = this.saveRowValues[ev.detail.rowId];
      this.dispatchEvent(new CustomEvent('gridActions', { detail: ev.detail, bubbles: true, composed: true }));
    }
  }, {
    key: '_submitCell',
    value: function _submitCell(ev) {
      var obj = {};
      if (ev.detail.key === 'addAttachment') {
        var obj = {
          rowDetail: ev.detail.rowId,
          actionName: 'addAttachment',
          customEvent: 'addAttachment'
        };
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
      } else if (ev.detail.key === 'deleteAttachment') {
        var obj = {
          rowDetail: ev.detail.value,
          actionName: 'deleteAttachment',
          customEvent: 'deleteAttachment'
        };
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
      } else if (ev.detail.key === 'createDefect') {
        var obj = {
          rowDetail: ev.detail.rowId,
          actionName: 'createDefect',
          customEvent: 'createDefect'
        };
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
      } else if (ev.detail.key === 'defectPicker') {
        var obj = {
          rowDetail: ev.detail.rowId,
          actionName: 'defectPicker',
          customEvent: 'defectPicker'
        };
        this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
      } else if (ev.detail.key === 'stepDefect') {
        obj[ev.detail.cellKey] = ev.detail.value;
        this.dispatchEvent(new CustomEvent('gridValueUpdated', { detail: { wasAlreadyAdded: ev.detail.wasAlreadyAdded, updatedValue: obj, rowId: ev.detail.rowId, testConfig: this.testConfig }, bubbles: true, composed: true }));
      } else {
        obj[ev.detail.cellKey] = {
          selectedOptions: ev.detail.contentValue,
          value: ev.detail.value
        };
        this.dispatchEvent(new CustomEvent('gridValueUpdated', { detail: { isObject: true, updatedValue: obj, rowId: ev.detail.rowId, testConfig: this.testConfig }, bubbles: true, composed: true }));
      }
    }
  }, {
    key: '_saveCellValue',
    value: function _saveCellValue(ev) {
      if (this.saveRowValues[ev.detail.rowId]) {
        this.saveRowValues[ev.detail.rowId][ev.detail.cellKey] = {
          selectedOptions: ev.detail.contentValue,
          value: ev.detail.value
        };
      } else {
        this.saveRowValues[ev.detail.rowId] = {};
        this.saveRowValues[ev.detail.rowId][ev.detail.cellKey] = {
          selectedOptions: ev.detail.contentValue,
          value: ev.detail.value
        };
      }
    }
  }, {
    key: '_rowSelect',
    value: function _rowSelect(ev) {
      this.selectedRowId = ev.currentTarget.dataRowid;
      var rowId = ev.currentTarget.dataRowid;
      var that = this;
      this.dispatchEvent(new CustomEvent('gridRowSelected', { detail: { rowId: rowId }, bubbles: true, composed: true }));
    }
  }, {
    key: '_rowSelectDbl',
    value: function _rowSelectDbl(ev) {
      var rowId = ev.currentTarget.dataRowid;
      var that = this;
      that.shadowRoot.querySelectorAll('.table-row').forEach(function (row) {
        if (row.dataRowid === rowId) {
          AJS.$(row).addClass('selected');
        } else {
          AJS.$(row).removeClass('selected');
        }
      });
      this.dispatchEvent(new CustomEvent('gridRowSelected', { detail: { rowId: rowId, dblClick: true }, bubbles: true, composed: true }));
    }
  }, {
    key: '_submit',
    value: function _submit(ev) {
      var value = '';
      var rowId = ev.detail.rowId;
      if (ev.detail.type === 'checkbox') {
        var valueContent = ev.detail.value;
        value = valueContent.toString();
      }
      var that = this;
      var length = 0;
      this.rowsSelection.map(function (row, index) {
        var path = 'rowsSelection.' + index.toString();
        if (row.rowId === rowId) {
          if (value) {
            that.set(path, { rowId: row.rowId, selected: true, permission: row.permission });
          } else {
            that.set(path, { rowId: row.rowId, selected: false, permission: row.permission });
          }
        }
      });
      var trigger = AJS.$(this.shadowRoot.querySelector('.bulkAction-container.selectRows'))[0];

      var counter = 0;
      this.rowsSelection.map(function (row) {
        if (that.testConfig.checkedRowId.indexOf(row.rowId) >= 0) {
          var rowIndex = that.testConfig.checkedRowId.indexOf(row.rowId);
          that.testConfig.checkedRowId.splice(rowIndex, 1);
        }
        if (row.selected === true && !row.permission) {
          counter++;
        }
        if (!row.permission) {
          length++;
        }
      });
      this.testConfig.bulkActions.map(function (action, index) {
        var path = 'testConfig.bulkActions.' + index.toString(),
          imgSrcChecked = action.imgSrcChecked,
          imgSrcUnchecked = action.imgSrc;
        if (action.customEvent == 'selectRows') {
          if (counter === length) {
            that.set(path, { actionName: action.actionName, customEvent: action.customEvent, disabled: action.disabled, imgSrc: imgSrcChecked, imgSrcChecked: imgSrcUnchecked });
            !trigger.classList.contains('allSelected') && trigger.classList.add('allSelected');
          } else if (trigger.classList.contains('allSelected')) {
            that.set(path, { actionName: action.actionName, customEvent: action.customEvent, disabled: action.disabled, imgSrc: imgSrcChecked, imgSrcChecked: imgSrcUnchecked });
            trigger.classList.remove('allSelected');
          }
        } else {
          if (counter > 0 && index > 0) that.set(path, { actionName: action.actionName, customEvent: action.customEvent, disabled: false, imgSrc: imgSrcChecked, imgSrcChecked: imgSrcUnchecked }); else if (index > 0) {
            var disabled = true;

            if (action.customEvent === 'bulkDelete' && that.testConfig.checkedRowId && that.testConfig.checkedRowId.length) {
              disabled = false;
            }
            that.set(path, { actionName: action.actionName, customEvent: action.customEvent, disabled: disabled, imgSrc: imgSrcChecked, imgSrcChecked: imgSrcUnchecked });
          }
        }
      });
      var selectedRow = [];
      this.get('rowsSelection').map(function (row) {
        if (row.selected) selectedRow.push(row.rowId);
      });
      var obj = {
        rowsSelection: selectedRow,
        actionName: 'rowSelection',
        customEvent: 'selectRows',
        rowData: this.get('rowsSelection')
      };
      this.dispatchEvent(new CustomEvent('gridBulkActions', { detail: obj, bubbles: true, composed: true }));
    }
  }, {
    key: '_columnChooser',
    value: function _columnChooser(ev) {
      var value = '';
      var valueContent = ev.detail.value;
      value = valueContent.toString();
      var that = this;
      this.columnChooserValues.map(function (columns, index) {
        var path = 'columnChooserValues.' + index.toString();
        if (columns.key === ev.detail.rowId) {
          if (value) {
            that.set(path, { key: columns.key, isVisible: true, displayName: columns.displayName });
          } else {
            that.set(path, { key: columns.key, isVisible: false, displayName: columns.displayName });
          }
        }
      });
      var counter = 0;
      this.columnChooserValues.map(function (column) {
        if (column.isVisible === true) {
          counter++;
        }
      });

      if (counter === 0) {
        this.submitDisabled = true;
      } else {
        this.submitDisabled = false;
      }
      this.columnChooserTrigger.focus();
    }
  }, {
    key: '_submitColumnChooser',
    value: function _submitColumnChooser() {
      var obj = {
        columnDetails: this.columnChooserValues,
        actionName: 'columnChooser',
        customEvent: 'columnChooser'
      };
      this.columnChooserArr = JSON.parse(JSON.stringify(this.columnChooserValues));
      this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
      this.showColumnChooser = false;
    }
  }, {
    key: '_closeColumnChooser',
    value: function _closeColumnChooser() {
      this.columnChooserValues = JSON.parse(JSON.stringify(this.columnChooserArr));
      this.showColumnChooser = false;
    }
  }, {
    key: '_addTests',
    value: function _addTests(ev) {
      var element = AJS.$(ev.currentTarget)[0];
      var obj = {
        actionName: element.dataCustomevent
      };
      this.dispatchEvent(new CustomEvent('addTests', { detail: obj, bubbles: true, composed: true }));
    }
  }, {
    key: '_bulkAction',
    value: function _bulkAction(ev) {
      var element = AJS.$(ev.currentTarget)[0];
      var isDisabled = false;
      this.testConfig.bulkActions.forEach(function (action) {
        if (element.dataCustomevent === action.customEvent && action.disabled) {
          isDisabled = true;
        }
      });
      if (isDisabled) {
        return;
      }
      var obj = {};
      var isSelected;
      var boolean = false;
      this.selectAll = !this.selectAll;
      if (element.dataEventname === 'selectRows') {
        var that = this;
        isSelected = element.classList.contains('allSelected');
        if (element.classList.contains('allSelected')) {
          element.classList.remove('allSelected');
          boolean = false;
        } else {
          element.classList.add('allSelected');
          boolean = true;
        }
        this.rowsSelection.map(function (row, index) {
          var rowId = row.rowId;
          var path = 'rowsSelection.' + index.toString();
          that.set(path, { rowId: rowId, selected: !isSelected, permission: row.permission });
        });
        this.get('rowsSelection').map(function (row) {
          if (that.testConfig.checkedRowId.indexOf(row.rowId) >= 0) {
            var rowIndex = that.testConfig.checkedRowId.indexOf(row.rowId);
            that.testConfig.checkedRowId.splice(rowIndex, 1);
          }
        });

        //var imgSrc = isSelected ? imgSrc
        this.testConfig.bulkActions.map(function (action, index) {
          var path = 'testConfig.bulkActions.' + index.toString();
          var imgSrcChecked = action.imgSrcChecked,
            imgSrcUnchecked = action.imgSrc;
          var disabled = action.customEvent == 'selectRows' ? action.disabled : isSelected;
          if (action.customEvent === 'bulkDelete' && that.testConfig.checkedRowId && that.testConfig.checkedRowId.length) {
            disabled = false;
          }
          that.set(path, { actionName: action.actionName, customEvent: action.customEvent, disabled: disabled, imgSrc: imgSrcChecked, imgSrcChecked: imgSrcUnchecked });
        });
      }
      var selectedRow = [];
      this.get('rowsSelection').map(function (row) {
        if (row.selected) {
          selectedRow.push(row.rowId);
        }
      });

      var obj = {
        columnsValues: this.saveRowValues,
        rowsSelection: selectedRow,
        rowData: this.get('rowsSelection'),
        actionName: element.dataEventname,
        customEvent: element.dataCustomevent
      };
      if (boolean) {
        element.classList.add('allSelected');
      } else {
        element.classList.remove('allSelected');
      }
      this.dispatchEvent(new CustomEvent('gridBulkActions', { detail: obj, bubbles: true, composed: true }));
    }
  }, {
    key: '_sortGrid',
    value: function _sortGrid(ev) {
      var sortOrder = ev.currentTarget.dataSortorder;
      var sortkey = ev.currentTarget.dataColumnkey;

      if (sortOrder === '' || sortOrder === 'DESC') {
        sortOrder = 'ASC';
      } else if (sortOrder === 'ASC') {
        sortOrder = 'DESC';
      }
      var obj = {
        sortQuery: { sortkey: sortkey, sortOrder: sortOrder },
        actionName: 'sortGrid',
        customEvent: 'sortGrid'
      };
      this.dispatchEvent(new CustomEvent('gridActions', { detail: obj, bubbles: true, composed: true }));
    }
  }, {
    key: '_gridValueChanged',
    value: function _gridValueChanged(newValue) {
      var value = JSON.parse(newValue);
      if (value.newRow) {
        this.testConfig.row.splice(-1, 0, value.rowData);
        var temp = this.get("testConfig.row");
        this.initialCount = temp.length;
        this.set('testConfig.row', []);
        this.set('testConfig.row', temp);
        var noOfRow = this.get("testConfig.row").length-1;
        value.addStepRow.orderId = noOfRow + 1;
        this.set('testConfig.row.' + noOfRow, Object.assign({}, this.get('testConfig.row.' + noOfRow), value.addStepRow));
      } else if (value.deleteRow) {
        // this.testConfig.row.splice(value.index,1);
        // var freezedColumnRow = AJS.$(this.$.freezeColumns).find('.table-row')[value.index + 1];
        // var unfreezedColumnRow = AJS.$(this.$.unfreezeColumns).find('.table-row')[value.index + 1];
        // var actionColumnRow = AJS.$(this.$.gridActions).find('.table-row')[value.index + 1];
        // var draggableColumnRow = AJS.$(this.$.draggableColumn).find('.table-row')[value.index + 1];
        // var gridSelectionRow = AJS.$(this.$.gridSelection).find('.table-row')[value.index + 1];

        // freezedColumnRow && (freezedColumnRow.parentNode.removeChild(freezedColumnRow))
        // unfreezedColumnRow && (unfreezedColumnRow.parentNode.removeChild(unfreezedColumnRow))
        // actionColumnRow && (actionColumnRow.parentNode.removeChild(actionColumnRow))
        // draggableColumnRow && (draggableColumnRow.parentNode.removeChild(draggableColumnRow))
        // gridSelectionRow && (gridSelectionRow.parentNode.removeChild(gridSelectionRow))
        // this.initialCount = 50;
        // this.splice('testConfig.row', value.index, 1);
      } else if (value.updateOpenPopup) {
        this.set('testConfig.isPopupOpen', value.isPopupOpen);
        this.set('testConfig.popupTestSteps', value.popupTestSteps);
      } else {
        this.set('testConfig.row.' + value.rowData.index, Object.assign({}, this.get('testConfig.row.' + value.rowData.index), value.rowData));
        var that = this;
          if (value.rowData.editMode) {
          this.testConfig.head.map(function (column) {
            if ((column.type === 'DATE' || column.type === 'DATE_TIME') && value.rowData[column.key] !== '') {
              if (that.saveRowValues[value.rowData.id]) {
                that.saveRowValues[value.rowData.id][column.key] = {
                  selectedOptions: '',
                  value: value.rowData[column.key]
                };
              } else {
                that.saveRowValues[value.rowData.id] = {};
                that.saveRowValues[value.rowData.id][column.key] = {
                  selectedOptions: '',
                  value: value.rowData[column.key]
                };
              }
            }
          });
        }
        this.loader = 'false';
        // console.log(this.get('testConfig.row.' + value.index));
      }
    }
  }, {
    key: '_fetchattachment',
      value: function _fetchattachment(ev) {
        this.dispatchEvent(new CustomEvent('fetchattachment', { detail: ev.detail, bubbles: true, composed: true }));
    }
  }, {
    key: '_scrollGridHorizontal',
    value: function _scrollGridHorizontal(ev) {
      //console.log('inside scrollgrid horizontal', ev, this.scrollgrid);
      this.$.unfreezeColumns.scrollLeft = this.scrollgrid;
    }
  },{
    key: '_fetchdefect',
    value: function _fetchdefect(ev) {
      this.dispatchEvent(new CustomEvent('fetchdefect', { detail: ev.detail, bubbles: true, composed: true }));
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        config: {
          type: String,
          value: '{}',
          observer: '_configUpdate'
        },
        updatedconfig: {
          type: String,
          observer: '_gridValueChanged'
        },
        scrollgrid: {
          type: String,
          observer: '_scrollGridHorizontal'
        }
      };
    }
  }]);

  return GridComponent;
}(Polymer.Element);

customElements.define(GridComponent.is, GridComponent);

var SingleSelect = function (_Polymer$Element12) {
  _inherits(SingleSelect, _Polymer$Element12);

  _createClass(SingleSelect, null, [{
    key: 'is',
    get: function get() {
      return "single-select";
    }
  }]);

  function SingleSelect() {
    _classCallCheck(this, SingleSelect);

    var _this13 = _possibleConstructorReturn(this, (SingleSelect.__proto__ || Object.getPrototypeOf(SingleSelect)).call(this));

    _this13.readMode = true;
    _this13.editMode = false;
    _this13.value = '';
    _this13.cloneOptions = [];
    _this13.selectedValue = '';
    return _this13;
  }

  _createClass(SingleSelect, [{
    key: '_optionsChanged',
    value: function _optionsChanged(newValue) {
      this.cloneOptions = JSON.parse(JSON.stringify(newValue));
      this._assignValues(newValue);
      this.addEventListener('submitValue', this._submitValue);
    }
  }, {
    key: '_assignValues',
    value: function _assignValues(value) {
      var _this14 = this;

      value.map(function (option) {
        if (option.selected) {
          _this14.value = option.content;
          _this14.selectedValue = option.value;
          if (option.color) {
            AJS.$(_this14.$.sessionStatus).addClass('session-status');
            AJS.$(_this14.$.sessionStatus).css('background', option.color);
          }
        }
      });
    }
  }, {
    key: '_modeChange',
    value: function _modeChange(newValue) {
      if (this.mode === 'read') {
        this.readMode = true;
        this.editMode = false;
      } else {
        this.readMode = false;
        this.editMode = true;
      }
    }
  }, {
    key: '_changeMode',
    value: function _changeMode() {
      AJS.$(this.$.sessionStatus).removeClass('session-status');
      this.readMode = false;
      this.editMode = true;
      this.$.editMode.focus();
    }
  }, {
    key: '_handleChange',
    value: function _handleChange(e) {
      var _this15 = this;

      this.cloneOptions.map(function (option) {
        if (option.value.toString() === e.target.value) {
          option.selected = true;
          _this15.selectedValue = e.target.value;
        } else {
          option.selected = false;
        }
      });
    }

    // _updateValues(e) {
    //   this.readMode = true;
    //   this.editMode = false;
    //   this.options = JSON.parse(JSON.stringify(this.cloneOptions));
    //   this.dispatchEvent(new CustomEvent('submitValue',{detail : {submit : true},bubbles: true, composed: true}));
    // }
    //
    // _cancelValues(e) {
    //   this.readMode = true;
    //   this.editMode = false;
    //   this._assignValues(this.options);
    //   this.cloneOptions = JSON.parse(JSON.stringify(this.options));
    //   this.dispatchEvent(new CustomEvent('submitValue',{detail : {submit : false},bubbles: true, composed: true}));
    // }

  }, {
    key: '_checkblur',
    value: function _checkblur(ev) {
      ev.stopPropagation();
      if (ev.relatedTarget && ev.relatedTarget.localName === 'select') {
        this.$.editMode.focus();
      } else {
        if (this.mode === 'read') {
          this.readMode = true;
          this.editMode = false;
        }
        this.options = JSON.parse(JSON.stringify(this.cloneOptions));
        this.dispatchEvent(new CustomEvent('submitvalue', { detail: { type: 'SINGLE_SELECT', value: this.selectedValue }, bubbles: true, composed: true }));
      }
    }
  }], [{
    key: 'properties',
    get: function get() {
      return {
        options: {
          type: Array,
          value: [],
          observer: '_optionsChanged'
        },
        rowid: {
          type: String,
          value: ''
        },
        cellkey: {
          type: String,
          value: ''
        },
        mode: {
          type: String,
          value: 'read',
          observer: '_modeChange'
        }
      };
    }
  }]);

  return SingleSelect;
}(Polymer.Element);

customElements.define(SingleSelect.is, SingleSelect);
