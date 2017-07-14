var designDoc = {
	"_id": "_design/Answer",
	"language": "javascript",
	"views": {
		"by_questionid": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit(doc.questionId, {_rev: doc._rev});
				}
			}
		},
		"by_questionid_piround_text_subject": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.questionId, doc.piRound, doc.abstention, doc.answerText, doc.answerSubject, doc.successfulFreeTextAnswer], {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_questionid_timestamp": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.questionId, doc.timestamp], {_rev: doc._rev});
				}
			}
		},
		"by_questionid_user_piround": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.questionId, doc.user, doc.piRound], {_rev: doc._rev});
				}
			}
		},
		"by_sessionid": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit(doc.sessionId, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_sessionid_variant": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.sessionId, doc.questionVariant], {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_user_sessionid": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.user, doc.sessionId], {_rev: doc._rev});
				}
			}
		},
		"questionid_by_user_sessionid_variant": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.user, doc.sessionId, doc.questionVariant], doc.questionId);
				}
			}
		},
		"questionid_piround_by_user_sessionid_variant": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.user, doc.sessionId, doc.questionVariant], [doc.questionId, doc.piRound]);
				}
			}
		}
	}
};
