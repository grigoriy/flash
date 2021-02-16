package com.galekseev.dynalist_to_anki

import com.galekseev.dynalist_to_anki.model.{Word, WordDefinition, WordWithDefinition}
import com.galekseev.dynalist_to_anki.oxford.{Entry, Headword}
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json._
import play.shaded.ahc.org.asynchttpclient.{AsyncHttpClient, Response}

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

    queryOxfordDictionary(word).map(response => {
      val responseJson = Json.parse(response.getResponseBody)
      response.getStatusCode match {
        case status if status >= 200 && status < 300 =>

          (responseJson \ "results").validateOpt[Iterable[Headword]] match {
            case JsSuccess(maybeHeadwords, _) =>
              WordWithDefinition(word, maybeHeadwords.map(headwords =>
                parseWordDefinition(headwords)
              ).getOrElse({
                logger.warn(s"Got no headwords for '${word.chars}.")
                WordDefinition(Iterable.empty, None, Iterable.empty)
              }))

            case JsError(errors) =>
              throw new RuntimeException(s"Failed to parse Oxford Dictionary response for '${word.chars}': '$errors'")
          }

        case other =>
          throw new RuntimeException(s"Failed to translate ${word.chars}; status: $other, message: '${
            (responseJson \ "error").validate[String].getOrElse("???")}'")
      }
    })

  private def parseWordDefinition(headwords: Iterable[Headword]): WordDefinition = {
    val entries = parseHeadwords(headwords)
    val shortDefinitions = parseDefinitions(entries)
    val pronunciation = parsePronunciation(entries)
    val examples = parseExamples(entries)
    WordDefinition(shortDefinitions, pronunciation, examples)
  }

  private def queryOxfordDictionary(word: Word): Future[Response] =
    toScala(httpClient.prepareGet(s"${baseUri.toASCIIString}${word.chars}")
      .addHeader("Accept", "application/json")
      .addHeader("app_id", appId)
      .addHeader("app_key", appKey)
      .addQueryParam("strictMatch", "false")
      .execute()
      .toCompletableFuture)

  private def parseHeadwords(headwords: Iterable[Headword]): Iterable[Entry] =
    for {
      headword <- headwords
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
