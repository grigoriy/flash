package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{Json, Reads}

case class Example(text: String)

object Example {
  implicit val reads: Reads[Example] = Json.reads[Example]
}
