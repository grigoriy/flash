package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{Json, Reads}

case class Entry(senses: Iterable[RecursiveSense],
                 pronunciations: Option[Iterable[Pronunciation]],
                 grammaticalFeatures: Option[Iterable[GrammaticalFeature]])

object Entry {
  implicit val reads: Reads[Entry] = Json.reads[Entry]
}
