/*!
 * A simple widget that allows you to adjust the width of an element by dragging. The width is persisted in local
 * storage and restored on page reload.
 *
 * Depends:
 *   jquery.ui.widget.js
 */
(function($) {


    $.widget( "ui.sidebar",  {
        version: "0.1",

        /* defaults */
        options: {
            /**
             * A function that returns the minimum width the user can resize to.
             * @param {instance} ui
             * @return {number} minWidth
             */
            minWidth: function (ui) { return  50; },
            /**
             * A function that returns the maximum width the user can resize to.
             * @param {instance} ui
             * @return {number} maxWidth
             */
            maxWidth: function (ui) { return jQuery(window).width(); },

            /**
             * A callback for when user is resizing sidebar
             * @param {instance} ui
             * @return {number} width of sizebar
             */
            resize: $.noop
        },

        /**
         * @constructor
         */
        _create: function() {
            _.bindAll(this, "_handleDrag", "_persist", "_setContainment", "updatePosition");
            if (!this.options.id) {
                console.error("ui.sidebar: You must specify an id")
            }
            this._restore();
            this._addHandle();
            this.handle.mousedown(this._setContainment);
            this.handle.draggable({axis: "x", drag: this._handleDrag, stop: this._persist});
            $(window).resize(_.debounce(this.updatePosition, 30));
        },

        /**
         * Restores the sidebar to the user configured width
         * @private
         */
        _restore: function () {
            if (window.localStorage) {
                var width = localStorage.getItem("ui.sidebar." + this.options.id);
                if (width) {
                    this.element.width(width);
                }
            }
        },

        /**
         * Persists the sidebar to the user configured width so it works across page refreshes.
         * @private
         */
        _persist: function () {
            if (window.localStorage) {
                localStorage.setItem("ui.sidebar." + this.options.id, this.element.width());
            }
        },

        /**
         * Sets the min & max width boundaries for dragging side bar
         * @private
         */
        _setContainment: function () {
            var windowHeight = jQuery(window).height();
            this._elementLeft = this.element.offset().left;
            this._minLeft = this._elementLeft + this.options.minWidth(this);
            this._maxLeft = Math.max(this._minLeft, this._elementLeft + this.options.maxWidth(this));
            this.handle.draggable({containment: [this._minLeft, windowHeight,  this._maxLeft, windowHeight]});
        },

        /**\
         * Sets the width of sidebar
         * @param {Object} e
         * @param {Object} ui
         * @private
         */
        _handleDrag: function (e, ui) {
            var target = ui.position.left - this._elementLeft;
            this._setWidth(target, true);
        },

        _setWidth: function (target, force) {
            var padding = this.element.outerWidth() - this.element.width();
            if (!force) {
                var maxWidth = this.options.maxWidth(this);
                var minWidth = this.options.minWidth(this);
                if (target > maxWidth) {
                    target = maxWidth
                } else if (target < minWidth) {
                    target = minWidth;
                }
            }
            this.element.width(target - padding);
            this.options.resize(this, target);
            this._trigger("resize", null, target);
        },


        /**
         * Appends a drag handle next to the sidebar
         * @private
         */
        _addHandle: function () {
            this.handle = jQuery("<div />").addClass("ui-sidebar").appendTo(this.element);
            this._setHandlePosition();
        },

        /**
         * Aligns the drag handler to the sidebar
         * @private
         */
        _setHandlePosition: function () {
            this._setContainment();
            if (this._minLeft === this._maxLeft) {
                this.handle.hide();
            } else {
                var elOffset = this.element.offset();
                var left = elOffset.left + this.element.outerWidth();
                this.handle.css({
                    top: elOffset.top,
                    left: left,
                    height: this.element.outerHeight()
                }).show();
            }
        },

        /**
         * Updates position of handle. You trigger this externally by jQuery(".sidebar").sidebar("updatePosition")
         */
        updatePosition: function () {
            this._setHandlePosition();
            this._setWidth(this.handle.offset().left - this._elementLeft);
            this._persist();
        }
    });

})( jQuery );
