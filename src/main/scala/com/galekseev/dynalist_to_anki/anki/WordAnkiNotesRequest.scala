package com.galekseev.dynalist_to_anki.anki

import play.api.libs.json._

case class WordAnkiNotesRequest(notes: Seq[WordAnkiNote], apiVersion: Int)

object WordAnkiNotesRequest {

  implicit val writes: Writes[WordAnkiNotesRequest] = (o: WordAnkiNotesRequest) => Json.obj(
    "action" -> JsString("addNotes"),
    "version" -> JsNumber(o.apiVersion),
    "params" -> Json.obj(
      "notes" -> JsArray(o.notes.map(note => Json.toJson(note)))
    )
  )
}
