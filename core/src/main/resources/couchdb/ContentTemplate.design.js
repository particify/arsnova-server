var designDoc = {
	"_id": "_design/ContentTemplate",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (doc.type === "ContentTemplate") {
					emit(doc._id, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		}
	}
};
