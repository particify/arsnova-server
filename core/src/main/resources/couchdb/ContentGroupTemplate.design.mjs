export let designDoc = {
  "_id": "_design/ContentGroupTemplate",
  "language": "javascript",
  "views": {
    "by_id": {
      "map": function (doc) {
        if (doc.type === "ContentGroupTemplate") {
          emit(doc._id, {_rev: doc._rev});
        }
      },
      "reduce": "_count"
    },
    "by_creatorid_updatetimestamp": {
      "map": function (doc) {
        if (doc.type === "ContentGroupTemplate" && doc.creatorId && (doc.updateTimestamp || doc.creationTimestamp)) {
          emit([doc.creatorId, doc.updateTimestamp ? doc.updateTimestamp : doc.creationTimestamp], {_rev: doc._rev});
        }
      }
    }
  }
};
