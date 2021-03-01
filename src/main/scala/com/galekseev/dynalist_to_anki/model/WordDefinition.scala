package com.galekseev.dynalist_to_anki.model

case class WordDefinition(lexicalCategory: String,
                          domainClasses: Iterable[String],
                          grammaticalFeatures: Iterable[String],
                          registers: Iterable[String],
                          meanings: Iterable[String],
                          submeanings: Iterable[String],
                          examples: Iterable[String],
                          synonyms: Iterable[String],
                          pronunciation: Option[String])
