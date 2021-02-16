package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{Json, Reads}

case class LexicalEntry(lexicalCategory: LexicalCategory, entries: Iterable[Entry] )

object LexicalEntry {
    implicit val reads: Reads[LexicalEntry] = Json.reads[LexicalEntry]
}
