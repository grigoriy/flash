package com.galekseev.dynalist_to_anki.anki

import com.galekseev.dynalist_to_anki.model.WordWithDefinition
import play.api.libs.json.{JsString, Json, Writes}

case class WordAnkiNoteFields(word: String,
                              phoneticSymbol: String,
                              definition: String,
                              examples: String,
                              cloze: String,
                              synonyms: String
//                              audio: String,
//                              picture: String,
                             )

object WordAnkiNoteFields {
  implicit val writes: Writes[WordAnkiNoteFields] = (o: WordAnkiNoteFields) => Json.obj(
    "Word" -> JsString(o.word),
    "Phonetic symbol" -> JsString(o.phoneticSymbol),
    "Definition" -> JsString(o.definition),
    "Examples" -> JsString(o.examples),
    "Cloze" -> JsString(o.cloze),
    "Synonyms" -> JsString(o.synonyms))

  def apply(wordWithDefinition: WordWithDefinition): WordAnkiNoteFields = apply(
    wordWithDefinition.word.chars,
    wordWithDefinition.definition.pronunciation.getOrElse(""),
    formatAsList(wordWithDefinition.definition.meaning).getOrElse("???"),
    formatAsList(wordWithDefinition.definition.examples).getOrElse(""),
    formatAsList(wordWithDefinition.definition.examples
      .map(_.replaceAll(wordWithDefinition.word.chars, "[...]"))
    ).getOrElse(""),
    wordWithDefinition.definition.synonyms.mkString(", ")
  )

  private def formatAsList[T](items: Iterable[T]): Option[String] =
    if (items.size > 1)
      Some(items.map(str => s"* $str").mkString("<br/>"))
    else
      items.headOption.map(_.toString)
}
