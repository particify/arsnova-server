var designDoc = {
	"_id": "_design/Motd",
	"language": "javascript",
	"views": {
		"by_audience_for_global": {
			"map": function (doc) {
				if (doc.type === "motd" && doc.audience !== "session") {
					emit(doc.audience, {_rev: doc._rev});
				}
			}
		},
		"by_motdkey": {
			"map": function (doc) {
				if (doc.type === "motd") {
					emit(doc.motdkey, {_rev: doc._rev});
				}
			}
		},
		"by_sessionkey": {
			"map": function (doc) {
				if (doc.type === "motd" && doc.audience === "session") {
					emit(doc.sessionkey, {_rev: doc._rev});
				}
			}
		}
	}
};
