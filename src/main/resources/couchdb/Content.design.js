var designDoc = {
	"_id": "_design/Content",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (doc.type === "Content") {
					emit(doc._id, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid": {
			"map": function (doc) {
				if (doc.type === "Content") {
					emit(doc.roomId, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		}
	}
};
