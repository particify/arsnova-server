export let designDoc = {
  "_id": "_design/RoomUserAlias",
  "language": "javascript",
  "views": {
    "by_id": {
      "map": function (doc) {
        if (doc.type === "RoomUserAlias") {
          emit(doc._id, {_rev: doc._rev});
        }
      },
      "reduce": "_count"
    },
    "by_roomid_userid": {
      "map": function (doc) {
        if (doc.type === "RoomUserAlias") {
          emit([doc.roomId, doc.userId], {_rev: doc._rev});
        }
      }
    },
    "by_userid": {
      "map": function (doc) {
        if (doc.type === "RoomUserAlias") {
          emit(doc.userId, {_rev: doc._rev});
        }
      }
    }
  }
};
