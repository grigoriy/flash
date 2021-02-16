package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{Json, Reads}

case class LexicalCategory(id: String)

object LexicalCategory {
  implicit val reads: Reads[LexicalCategory] = Json.reads[LexicalCategory]
}
