var designDoc = {
	"_id": "_design/Motd",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (doc.type === "Motd") {
					emit(doc._id, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid": {
			"map": function (doc) {
				if (doc.type === "Motd" && doc.audience === "ROOM") {
					emit(doc.roomId, {_rev: doc._rev});
				}
			}
		}
	}
};
