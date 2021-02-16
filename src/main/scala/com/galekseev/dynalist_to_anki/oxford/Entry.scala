package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{Json, Reads}

case class Entry(senses: Iterable[Sense], pronunciations: Option[Iterable[Pronunciation]])

object Entry {
  implicit val reads: Reads[Entry] = Json.reads[Entry]
}
