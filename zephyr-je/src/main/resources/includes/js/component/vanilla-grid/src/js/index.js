var vanillaGrid = vanillaGrid || {};

(function() {

	vanillaGrid.init = function(container, config) {
		if(!container) {
			return;
		}
		vanillaGrid.templates.initialize(container, config); 
		vanillaGrid.events.initialize(container, config);
	}

})();