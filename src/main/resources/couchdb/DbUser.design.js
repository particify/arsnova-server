var designDoc = {
	"_id": "_design/DbUser",
	"language": "javascript",
	"views": {
		"by_creation_for_inactive": {
			"map": function (doc) {
				if (doc.type === "userdetails" && doc.activationKey) {
					emit(doc.creation, {_rev: doc._rev});
				}
			}
		},
		"by_username": {
			"map": function (doc) {
				if (doc.type === "userdetails") emit(doc.username, {_rev: doc._rev});
			}
		}
	}
};
