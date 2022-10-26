var designDoc = {
	"_id": "_design/ContentGroup",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (doc.type === "ContentGroup") {
					emit(doc._id, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid_name": {
			"map": function (doc) {
				if (doc.type === "ContentGroup") {
					emit([doc.roomId, doc.name], {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		}
	}
};
