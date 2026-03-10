export let designDoc = {
  "_id": "_design/RoomSettings",
  "language": "javascript",
  "views": {
    "by_id": {
      "map": function (doc) {
        if (doc.type === "RoomSettings") {
          emit(doc._id, {_rev: doc._rev});
        }
      },
      "reduce": "_count"
    }
  }
}
