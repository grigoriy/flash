package com.galekseev.dynalist_to_anki

import com.galekseev.dynalist_to_anki.model.{Word, WordDefinition, WordWithDefinitions}
import com.galekseev.dynalist_to_anki.oxford.{Entry, Headword, LexicalEntry, RecursiveSense}
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
                             (implicit executionContext: ExecutionContext) extends Dictionary[Word, WordWithDefinitions] with StrictLogging {

  override def translate(word: Word): Future[WordWithDefinitions] =

    queryOxfordDictionary(word).map(response => {
      val responseJson = Json.parse(response.getResponseBody)
      response.getStatusCode match {
        case status if status >= 200 && status < 300 =>

          (responseJson \ "results").validateOpt[Iterable[Headword]] match {
            case JsSuccess(maybeHeadwords, _) =>
              WordWithDefinitions(word, maybeHeadwords.map(headwords =>
                parseWordDefinition(headwords)
              ).getOrElse({
                logger.warn(s"Got no headwords for '${word.chars}.")
                Iterable.empty
              }))

            case JsError(errors) =>
              throw new RuntimeException(s"Failed to parse Oxford Dictionary response for '${word.chars}': '$errors'")
          }

        case other =>
          throw new RuntimeException(s"Failed to translate ${word.chars}; status: $other, message: '${
            (responseJson \ "error").validate[String].getOrElse("???")}'")
      }
    })

  private def parseWordDefinition(headwords: Iterable[Headword]): Iterable[WordDefinition] = {
    val lexicalEntries = parseLexicalEntries(headwords)
    for {
      lexicalEntry <- lexicalEntries
      entry <- lexicalEntry.entries
      recursiveSense <- entry.senses
    } yield {
      val pronunciation = parsePronunciation(entry)
      val grammaticalFeatures = parseGrammaticalFeatures(entry)
      val definitions = recursiveSense.sense.definitions
      val subdefinitions = parseSubdefinitions(recursiveSense)
      val examples = parseExamples(recursiveSense)
      val synonyms = parseSynonyms(recursiveSense)
      val domainClasses = parseDomainClasses(recursiveSense)
      val registers = parseRegisters(recursiveSense)

      WordDefinition(
        lexicalEntry.lexicalCategory.id,
        domainClasses,
        grammaticalFeatures,
        registers,
        definitions,
        subdefinitions,
        examples,
        synonyms,
        pronunciation)
    }
  }

  private def queryOxfordDictionary(word: Word): Future[Response] =
    toScala(httpClient.prepareGet(s"${baseUri.toASCIIString}${word.chars}")
      .addHeader("Accept", "application/json")
      .addHeader("app_id", appId)
      .addHeader("app_key", appKey)
      .addQueryParam("strictMatch", "false")
      .execute()
      .toCompletableFuture)

  private def parseLexicalEntries(headwords: Iterable[Headword]): Iterable[LexicalEntry] =
    for {
      headword <- headwords
      if headword.maybeLexicalEntries.isDefined
      lexicalEntry <- headword.maybeLexicalEntries.get
    } yield lexicalEntry

  private def parsePronunciation(entry: Entry): Option[String] =
    for {
      pronunciations <- entry.pronunciations
      firstPronunciation <- pronunciations.headOption
    } yield firstPronunciation.phoneticSpelling

  private def parseGrammaticalFeatures(entry: Entry): Iterable[String] =
    entry.grammaticalFeatures.getOrElse(Iterable.empty).map(_.id)

  private def parseSubdefinitions(recursiveSense: RecursiveSense): Iterable[String] =
    for {
      subsense <- recursiveSense.maybeSubsenses.getOrElse(Iterable.empty)
      definition <- subsense.definitions
    } yield definition

  private def parseExamples(recursiveSense: RecursiveSense): Iterable[String] =
    for {
      examples <- recursiveSense.sense.maybeExamples.getOrElse(Iterable.empty)
    } yield examples.text

  private def parseSynonyms(recursiveSense: RecursiveSense): Iterable[String] =
    for {
      synonyms <- recursiveSense.sense.maybeSynonyms.getOrElse(Iterable.empty)
    } yield synonyms.text

  private def parseDomainClasses(recursiveSense: RecursiveSense): Iterable[String] =
    for {
      domainClass <- recursiveSense.sense.maybeDomainClasses.getOrElse(Iterable.empty)
    } yield domainClass.id

  private def parseRegisters(recursiveSense: RecursiveSense): Iterable[String] =
    for {
      domainClass <- recursiveSense.sense.maybeRegisters.getOrElse(Iterable.empty)
    } yield domainClass.id
}
