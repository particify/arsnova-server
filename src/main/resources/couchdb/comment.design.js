var designDoc = {
	"_id": "_design/comment",
	"language": "javascript",
	"views": {
		"doc_by_sessionid_creator_timestamp": {
			"map": function (doc) {
				if (doc.type === "interposed_question") {
					emit([doc.sessionId, doc.creator, doc.timestamp], doc);
				}
			}
		},
		"doc_by_sessionid_timestamp": {
			"map": function (doc) {
				if (doc.type === "interposed_question") {
					emit([doc.sessionId, doc.timestamp], doc);
				}
			}
		},
		"by_sessionid": {
			/* Redundant view but kept for now to allow simpler queries. */
			"map": function (doc) {
				if (doc.type === "interposed_question") {
					emit(doc.sessionId, null);
				}
			},
			"reduce": "_count"
		},
		"by_sessionid_read": {
			"map": function (doc) {
				if (doc.type === "interposed_question") {
					emit([doc.sessionId, doc.read], null);
				}
			},
			"reduce": "_count"
		},
		"by_sessionid_creator_read": {
			"map": function (doc) {
				if (doc.type === "interposed_question") {
					emit([doc.sessionId, doc.creator, doc.read], null);
				}
			},
			"reduce": "_count"
		}
	}
};
