package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{Json, Reads}

case class GrammaticalFeature(id: String)

object GrammaticalFeature {
  implicit val reads: Reads[GrammaticalFeature] = Json.reads[GrammaticalFeature]
}
