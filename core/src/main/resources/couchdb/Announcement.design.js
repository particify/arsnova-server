var designDoc = {
	"_id": "_design/Announcement",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (doc.type === "Announcement") {
					emit(doc._id, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid": {
			"map": function (doc) {
				if (doc.type === "Announcement") {
					emit(doc.roomId, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		}
	}
};
