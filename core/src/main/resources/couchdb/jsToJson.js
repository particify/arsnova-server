/**
 * Transforms JS map functions of the passed object to strings. JS functions are
 * not valid JSON. Additionally, redundant indentation is removed from the
 * function string.
 */
function jsToJson(designDoc) {
	var views = designDoc.views;
	Object.keys(views).forEach(function (viewName) {
		views[viewName].map = views[viewName].map.toString().replace(/\n\t{3}(\t*)/g, function (m, p1) {
			return "\n" + p1.replace(/\t/g, "  ");
		});
	});

	return designDoc;
}
