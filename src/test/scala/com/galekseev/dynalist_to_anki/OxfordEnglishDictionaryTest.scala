package com.galekseev.dynalist_to_anki

import com.galekseev.dynalist_to_anki.model.{Word, WordDefinition}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AsyncWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.shaded.ahc.org.asynchttpclient.Dsl.asyncHttpClient

import java.net.URI
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Random, Success}

class OxfordEnglishDictionaryTest extends AsyncWordSpecLike with Matchers with TableDrivenPropertyChecks with BeforeAndAfterEach with BeforeAndAfterAll {

  private val host = "localhost"
  private val port = Random.between(49152, 65536)
  private val dictionaryServer = {
    val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))
    server.start()
    server
  }

  override def afterAll(): Unit =
    dictionaryServer.stop()

  override def beforeEach(): Unit = {
    dictionaryServer.resetAll()
    WireMock.configureFor(host, port)
  }

  private val testData = Table(
    ("word", "dictionaryResponseFile", "expectedDefinition"),

    (Word("watermelon"),
      "oxford/oxford_dict_entries_watermelon.json",
      WordDefinition(
        Iterable(
          "large fruit with smooth green skin, red pulp, and watery juice",
          "African plant which yields watermelons"),
        Some("ˈwɔːtəmɛlən"),
        Iterable("I like watermelons."),
        Iterable("big green berry")))
  )

  "translate" should {
    "return a correct word definition" in {

      implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
      val path = "/api/v2/entries/en-gb/"
      val appId = "14398df8"
      val appKey = "afc1b8d90cc428904eabb4271978ba41"
      val dictionary = new OxfordEnglishDictionary(
        asyncHttpClient(),
        URI.create(s"http://$host:$port$path"),
        appId,
        appKey)

      forAll(testData) ((word, dictionaryResponseFile, expectedDefinition) => {

        givenThat(
          get(s"$path${word.chars}?strictMatch=false")
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("app_id", equalTo(appId))
            .withHeader("app_key", equalTo(appKey))
            .withQueryParam("strictMatch", equalTo("false"))
            .willReturn(aResponse().withBodyFile(dictionaryResponseFile)))

        dictionary.translate(word).map(wordWithDefinition =>

          assert(wordWithDefinition.definition === expectedDefinition))
      })
    }

    "throw an exception given a missing word" in {

      implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
      val path = "/api/v2/entries/en-gb/"
      val appId = "14398df8"
      val appKey = "afc1b8d90cc428904eabb4271978ba41"
      val dictionary = new OxfordEnglishDictionary(
        asyncHttpClient(),
        URI.create(s"http://$host:$port$path"),
        appId,
        appKey)
      val word = Word("lkwef134")

      givenThat(
        get(s"$path${word.chars}?strictMatch=false")
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("app_id", equalTo(appId))
          .withHeader("app_key", equalTo(appKey))
          .withQueryParam("strictMatch", equalTo("false"))
          .willReturn(aResponse().withStatus(404).withBodyFile("oxford/oxford_dict_entries_lkwefl34.json")))

      dictionary.translate(word).transformWith {
        case Failure(exception) =>
          assert(exception.getMessage === s"Failed to translate ${word.chars}; status: 404, message: 'No entry found matching supplied source_lang, word and provided filters'")
        case Success(value) =>
          fail(s"Got a successful response while expecting a failure: $value")
      }
    }

    "return an empty definition given a word without headword definitions" in {

      implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
      val path = "/api/v2/entries/en-gb/"
      val appId = "14398df8"
      val appKey = "afc1b8d90cc428904eabb4271978ba41"
      val dictionary = new OxfordEnglishDictionary(
        asyncHttpClient(),
        URI.create(s"http://$host:$port$path"),
        appId,
        appKey)
      val word = Word("lkwef134")

      givenThat(
        get(s"$path${word.chars}?strictMatch=false")
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("app_id", equalTo(appId))
          .withHeader("app_key", equalTo(appKey))
          .withQueryParam("strictMatch", equalTo("false"))
          .willReturn(aResponse().withBodyFile("oxford/oxford_dict_entries_missing_headwords.json")))

      dictionary.translate(word).map({ wordWithDefinition =>
        assert(wordWithDefinition.definition === WordDefinition(Iterable.empty, None, Iterable.empty, Iterable.empty))
      })
    }

    "throw an exception given an unexpected JSON schema" in {

      implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
      val path = "/api/v2/entries/en-gb/"
      val appId = "14398df8"
      val appKey = "afc1b8d90cc428904eabb4271978ba41"
      val dictionary = new OxfordEnglishDictionary(
        asyncHttpClient(),
        URI.create(s"http://$host:$port$path"),
        appId,
        appKey)
      val word = Word("some_word")

      givenThat(
        get(s"$path${word.chars}?strictMatch=false")
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("app_id", equalTo(appId))
          .withHeader("app_key", equalTo(appKey))
          .withQueryParam("strictMatch", equalTo("false"))
          .willReturn(aResponse().withBodyFile("oxford/oxford_dict_entries_invalid_results.json")))

      dictionary.translate(word).transformWith {
        case Failure(exception) =>
          assert(exception.getMessage.startsWith(s"Failed to parse Oxford Dictionary response for '${word.chars}':"))
        case Success(value) =>
          fail(s"Got a successful response while expecting a failure: $value")
      }
    }
  }
}
