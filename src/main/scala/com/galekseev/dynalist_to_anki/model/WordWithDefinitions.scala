package com.galekseev.dynalist_to_anki.model

case class WordWithDefinitions(word: Word, definitions: Iterable[WordDefinition])
