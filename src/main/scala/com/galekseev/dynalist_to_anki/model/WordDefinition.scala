package com.galekseev.dynalist_to_anki.model

case class WordDefinition(meaning: Iterable[String], pronunciation: Option[String], examples: Iterable[String])
