package com.galekseev.dynalist_to_anki.anki

import play.api.libs.json._

case class AnkiCreateNotesResponse(result: Iterable[String], error: Option[String])

object AnkiCreateNotesResponse {
  implicit val optionReads: Reads[Iterable[String]] = (json: JsValue) =>
    json.validate[JsArray]
      .map(_.value.map({
        case JsNull => None
        case other => Some(Json.stringify(other))
      }).filter(_.isDefined).map(_.get))
  implicit val reads: Reads[AnkiCreateNotesResponse] = Json.reads[AnkiCreateNotesResponse]
}
