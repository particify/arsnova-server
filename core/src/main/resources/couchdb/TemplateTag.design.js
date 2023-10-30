var designDoc = {
	"_id": "_design/TemplateTag",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (doc.type === "TemplateTag") {
					emit(doc._id, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		}
	}
};
