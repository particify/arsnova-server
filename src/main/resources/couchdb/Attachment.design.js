var designDoc = {
	"_id": "_design/Attachment",
	"language": "javascript",
	"views": {
		"by_creatorid": {
			"map": function (doc) {
				if (doc.type === "Attachment") {
					emit(doc.creatorId, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		}
	}
}
