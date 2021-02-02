var designDoc = {
	"_id": "_design/Comment",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (doc.type === "Comment") {
					emit(doc._id, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid": {
			"map": function (doc) {
				if (doc.type === "Comment") {
					emit(doc.roomId, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid_creatorid_creationtimestamp": {
			"map": function (doc) {
				if (doc.type === "Comment") {
					emit([doc.roomId, doc.creatorId, new Date(doc.creationTimestamp).getTime()], {_rev: doc._rev});
				}
			}
		},
		"by_roomid_creationtimestamp": {
			"map": function (doc) {
				if (doc.type === "Comment") {
					emit([doc.roomId, new Date(doc.creationTimestamp).getTime()], {_rev: doc._rev});
				}
			}
		}
	}
};
