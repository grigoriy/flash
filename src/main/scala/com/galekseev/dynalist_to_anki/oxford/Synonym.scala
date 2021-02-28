package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{Json, Reads}

case class Synonym(text: String)

object Synonym {
  implicit val reads: Reads[Synonym] = Json.reads[Synonym]
}


