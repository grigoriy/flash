package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{JsValue, Reads}

case class RecursiveSense(sense: Sense, maybeSubsenses: Option[Iterable[Sense]])

object RecursiveSense {
  implicit val reads: Reads[RecursiveSense] = (json: JsValue) =>
    for {
      sense <- json.validate[Sense]
      maybeSubsenses <- (json \ "subsenses").validateOpt[Iterable[Sense]]
    } yield RecursiveSense(sense, maybeSubsenses)
}


