var designDoc = {
	"_id": "_design/UserProfile",
	"language": "javascript",
	"views": {
		"by_creation_for_inactive": {
			"map": function (doc) {
				if (doc.type === "UserProfile" && doc.activationKey) {
					emit(doc.creation, {_rev: doc._rev});
				}
			}
		},
		"by_username": {
			"map": function (doc) {
				if (doc.type === "UserProfile") emit(doc.username, {_rev: doc._rev});
			}
		}
	}
};
