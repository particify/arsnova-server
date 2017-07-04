var designDoc = {
	"_id": "_design/motd",
	"language": "javascript",
	"views": {
		"doc_by_sessionkey": {
			"map": function (doc) {
				if (doc.type === "motd" && doc.audience === "session") {
					emit(doc.sessionkey, doc);
				}
			}
		},
		"doc_by_audience_for_global": {
			"map": function (doc) {
				if (doc.type === "motd" && doc.audience !== "session") {
					emit(doc.audience, doc);
				}
			}
		},
		"by_motdkey": {
			"map": function (doc) {
				if (doc.type === "motd") {
					emit(doc.motdkey, doc);
				}
			}
		}
	}
};
