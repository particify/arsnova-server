var designDoc = {
	"_id": "_design/Comment",
	"language": "javascript",
	"views": {
		"by_sessionid": {
			"map": function (doc) {
				if (doc.type === "interposed_question") {
					emit(doc.sessionId, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_sessionid_creator_read": {
			"map": function (doc) {
				if (doc.type === "interposed_question") {
					emit([doc.sessionId, doc.creator, doc.read], {_rev: doc._rev})
				}
			},
			"reduce": "_count"
		},
		"by_sessionid_creator_timestamp": {
			"map": function (doc) {
				if (doc.type === "interposed_question") {
					emit([doc.sessionId, doc.creator, doc.timestamp], {_rev: doc._rev});
				}
			}
		},
		"by_sessionid_read": {
			"map": function (doc) {
				if (doc.type === "interposed_question") {
					emit([doc.sessionId, doc.read], {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_sessionid_timestamp": {
			"map": function (doc) {
				if (doc.type === "interposed_question") {
					emit([doc.sessionId, doc.timestamp], {_rev: doc._rev});
				}
			}
		}
	}
};
