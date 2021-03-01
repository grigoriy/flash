package com.galekseev.dynalist_to_anki.anki

import com.galekseev.dynalist_to_anki.model.{Word, WordDefinition}
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

  def apply(word: Word, definition: WordDefinition): WordAnkiNoteFields = {
    val domainClasses = formatAsAppendix(definition.domainClasses)
    val grammaticalFeatures = formatAsAppendix(definition.grammaticalFeatures)
    val registers = formatAsAppendix(definition.registers)
    val pronunciation = definition.pronunciation.getOrElse("")
    val definitions = formatAsList(definition.meanings ++ definition.submeanings)
      .map(_.replaceAll(toFirstCharCaseInsensitiveRegex(word.chars), "[_]"))
      .getOrElse("???")
    val examples = formatAsList(definition.examples).getOrElse("")
    val clozeDeletions = examples.replaceAll(toFirstCharCaseInsensitiveRegex(word.chars), "[...]")
    val synonyms = definition.synonyms.mkString(", ")

    apply(
      s"${word.chars} (${definition.lexicalCategory}$grammaticalFeatures$domainClasses$registers)",
      pronunciation,
      definitions,
      examples,
      clozeDeletions,
      synonyms
    )
  }

  private def formatAsAppendix(s: Iterable[Any]): String =
    if (s.nonEmpty)
      s"; ${s.mkString(", ")}"
    else
      ""

  private def formatAsList[T](items: Iterable[T]): Option[String] =
    if (items.size > 1)
      Some(items.map(str => s"* $str").mkString("<br/>"))
    else
      items.headOption.map(_.toString)

  private def toFirstCharCaseInsensitiveRegex(s: String): String =
    s"[${s.head.toLower}${s.head.toUpper}]${s.substring(1)}"
}
