export let designDoc = {
  "_id": "_design/statistics",
  "language": "javascript",
  "views": {
    "statistics": {
      "map": function (doc) {
        emit([doc.type], 1);
        switch (doc.type) {
        case "UserProfile":
          emit([doc.type, "authProvider", doc.authProvider], 1);
          if (doc.account && doc.account.activationKey) {
            emit([doc.type, "activationPending"], 1);
          }
          break;
        case "Room":
          if (doc.closed) {
            emit([doc.type, "closed"], 1);
          }
          break;
        case "ContentGroup":
          if (doc.published) {
            emit([doc.type, "published"], 1);
          }
          if (doc.lastPublishedIndex !== -1) {
            emit([doc.type, "usingPublishingRange"], 1);
          }
          if (doc.templateId) {
            emit([doc.type, "fromTemplate"], 1);
          }
          break;
        case "Content":
          emit([doc.type, "format", doc.format], 1);
          if (doc.templateId) {
            emit([doc.type, "fromTemplate"], 1);
          }
          break;
        case "ContentGroupTemplate":
          emit([doc.type, "language", doc.language], 1);
          emit([doc.type, "license", doc.license], 1);
          break;
        case "Answer":
          emit([doc.type, "format", doc.format], 1);
          break;
        case "ViolationReport":
          emit([doc.type, "reason", doc.reason], 1);
          emit([doc.type, "decision", doc.decision], 1);
          break;
        case "Deletion":
          emit([doc.deletedType, "deleted"], doc.count || 1);
          break;
        }
      },
      "reduce": "_sum"
    },
    "unique_room_owners": {
      "map": function (doc) {
        if (doc.type === "Room") {
          emit(doc.ownerId, 1);
        }
      },
      "reduce": "_count"
    }
  }
};
