var designDoc = {
	"_id": "_design/Comment",
	"language": "javascript",
	"views": {
		"by_roomid": {
			"map": function (doc) {
				if (doc.type === "Comment") {
					emit(doc.roomId, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid_creatorid_read": {
			"map": function (doc) {
				if (doc.type === "Comment") {
					emit([doc.roomId, doc.creatorId, doc.read], {_rev: doc._rev})
				}
			},
			"reduce": "_count"
		},
		"by_roomid_creatorid_creationtimestamp": {
			"map": function (doc) {
				if (doc.type === "Comment") {
					emit([doc.roomId, doc.creatorId, doc.creationTimestamp], {_rev: doc._rev});
				}
			}
		},
		"by_roomid_read": {
			"map": function (doc) {
				if (doc.type === "Comment") {
					emit([doc.roomId, doc.read], {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid_creationtimestamp": {
			"map": function (doc) {
				if (doc.type === "Comment") {
					emit([doc.roomId, doc.creationTimestamp], {_rev: doc._rev});
				}
			}
		}
	}
};
