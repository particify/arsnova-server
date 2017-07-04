var designDoc = {
	"_id": "_design/content",
	"language": "javascript",
	"views": {
		"doc_by_sessionid_variant_active": {
			"map": function (doc) {
				if (doc.type === "skill_question") {
					emit([doc.sessionId, doc.questionVariant, doc.active, doc.subject, doc.text.substr(0, 16)], doc);
				}
			},
			"reduce": "_count"
		},
		"by_sessionid": {
			/* Redundant view but kept for now to allow simpler queries. */
			"map": function (doc) {
				if (doc.type === "skill_question") {
					emit(doc.sessionId, null);
				}
			},
			"reduce": "_count"
		},
		"by_sessionid_variant_active": {
			"map": function (doc) {
				if (doc.type === "skill_question") {
					emit([doc.sessionId, doc.questionVariant, doc.active, doc.subject, doc.text.substr(0, 16)], null);
				}
			},
			"reduce": "_count"
		}
	}
};
