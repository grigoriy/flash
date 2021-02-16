package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{JsValue, Reads}

case class Headword(maybeLexicalEntries: Option[Iterable[LexicalEntry]])

object Headword {

  implicit val reads: Reads[Headword] = (json: JsValue) =>
    (json \ "lexicalEntries").validateOpt[Iterable[LexicalEntry]].map(Headword(_))
}
