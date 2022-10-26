var designDoc = {
	"_id": "_design/AccessToken",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (doc.type === "AccessToken") {
					emit(doc._id, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_expirationdate": {
			"map": function (doc) {
				if (doc.type === "AccessToken") {
					emit(doc.expirationDate, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_roomid_token": {
			"map": function (doc) {
				if (doc.type === "AccessToken") {
					emit([doc.roomId, doc.token], {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		}
	}
}
