package com.galekseev.dynalist_to_anki.anki

import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsString, Json, Writes}

case class WordAnkiNote(fields: WordAnkiNoteFields) {
  val deckName: String = "English::My English"
  val modelName: String = "English new words"
}

object WordAnkiNote {
  implicit val writes: Writes[WordAnkiNote] = (o: WordAnkiNote) =>
    Json.obj(
      "deckName" -> JsString(o.deckName),
      "modelName" -> JsString(o.modelName),
      "fields" -> toJson(o.fields)
    )
}
