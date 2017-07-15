var designDoc = {
	"_id": "_design/Content",
	"language": "javascript",
	"views": {
		"by_sessionid": {
			"map": function (doc) {
				if (doc.type === "skill_question") {
					emit(doc.sessionId, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_sessionid_variant_active": {
			"map": function (doc) {
				if (doc.type === "skill_question") {
					emit([doc.sessionId, doc.questionVariant, doc.active, doc.subject, doc.text.substr(0, 16)], {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		}
	}
};
