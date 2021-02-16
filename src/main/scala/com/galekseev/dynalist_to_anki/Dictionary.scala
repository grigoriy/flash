package com.galekseev.dynalist_to_anki

import com.galekseev.dynalist_to_anki.model.{Word, WordDefinition, WordWithDefinition}
import com.galekseev.dynalist_to_anki.oxford.{Entry, HeadwordEntry}
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json._
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient

import java.net.URI
import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.{ExecutionContext, Future}

trait Dictionary[A, B] {
  def translate(word: A): Future[B]
}

class OxfordEnglishDictionary(httpClient: AsyncHttpClient,
                              baseUri: URI,
                              appId: String,
                              appKey: String)
                             (implicit executionContext: ExecutionContext) extends Dictionary[Word, WordWithDefinition] with StrictLogging {

  override def translate(word: Word): Future[WordWithDefinition] =

    getWordDefinitionJson(s"${baseUri.toASCIIString}${word.chars}").map(json => {
      (json \ "results").validateOpt[Iterable[HeadwordEntry]] match {

        case JsSuccess(maybeHeadwords, _) =>
          val wordDefinition = maybeHeadwords.map(headwordEntries => {
            val entries = parseHeadwordEntries(headwordEntries)
            val shortDefinitions = parseDefinitions(entries)
            val pronunciation = parsePronunciation(entries)
            val examples = parseExamples(entries)
            WordDefinition(shortDefinitions, pronunciation, examples)
          }).getOrElse(WordDefinition(Iterable.empty, None, Iterable.empty))

          WordWithDefinition(word, wordDefinition)

        case JsError(errors) =>
          throw new RuntimeException(s"Failed to parse Oxford Dictionary response for '${word.chars}': $errors")
      }
    })

  private def getWordDefinitionJson(url: String): Future[JsValue] =
    toScala(httpClient.prepareGet(url)
      .addHeader("Accept", "application/json")
      .addHeader("app_id", appId)
      .addHeader("app_key", appKey)
      .addQueryParam("strictMatch", "false")
      .execute()
      .toCompletableFuture)
      .map(response => Json.parse(response.getResponseBody))

  private def parseHeadwordEntries(headwordEntries: Iterable[HeadwordEntry]): Iterable[Entry] =
    for {
      headword <- headwordEntries
      if headword.maybeLexicalEntries.isDefined
      lexicalEntry <- headword.maybeLexicalEntries.get
      entry <- lexicalEntry.entries
    } yield entry

  private def parseDefinitions(entries: Iterable[Entry]): Iterable[String] =
    for {
      entry <- entries
      sense <- entry.senses
      shortDefinition <- sense.shortDefinitions
    } yield shortDefinition

  private def parsePronunciation(entries: Iterable[Entry]): Option[String] =
    (for {
      entry <- entries
      if entry.pronunciations.isDefined
      pronunciation <- entry.pronunciations.get
    } yield pronunciation.phoneticSpelling).headOption

  private def parseExamples(entries: Iterable[Entry]): Iterable[String] =
    for {
      entry <- entries
      sense <- entry.senses
      examples <- sense.maybeExamples.getOrElse(Iterable.empty)
    } yield examples.text
}