package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{JsValue, Reads}

case class HeadwordEntry(maybeLexicalEntries: Option[Iterable[LexicalEntry]])

object HeadwordEntry {

  implicit val reads: Reads[HeadwordEntry] = (json: JsValue) =>
    (json \ "lexicalEntries").validateOpt[Iterable[LexicalEntry]].map(HeadwordEntry(_))
}
