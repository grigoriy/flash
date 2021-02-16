package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{Json, Reads}

case class Pronunciation(phoneticSpelling: String)

object Pronunciation {
  implicit val reads: Reads[Pronunciation] = Json.reads[Pronunciation]
}
