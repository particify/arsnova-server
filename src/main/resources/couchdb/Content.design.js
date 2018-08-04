var designDoc = {
	"_id": "_design/Content",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (["Content", "ChoiceQuestionContent"].indexOf(doc.type) !== -1) {
					emit(doc._id, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid": {
			"map": function (doc) {
				if (["Content", "ChoiceQuestionContent"].indexOf(doc.type) !== -1) {
					emit(doc.roomId, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid_group_locked": {
			"map": function (doc) {
				if (["Content", "ChoiceQuestionContent"].indexOf(doc.type) !== -1) {
					emit([doc.roomId, doc.group, doc.locked, doc.subject, doc.body.substr(0, 16)], {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		}
	}
};
