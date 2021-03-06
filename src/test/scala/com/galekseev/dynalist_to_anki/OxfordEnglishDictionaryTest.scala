package com.galekseev.dynalist_to_anki

import com.galekseev.dynalist_to_anki.OxfordEnglishDictionaryTest.{LumpDefinitions, WatermelonDefinitions}
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
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Random, Success}

class OxfordEnglishDictionaryTest extends AsyncWordSpecLike
  with Matchers with TableDrivenPropertyChecks with BeforeAndAfterEach with BeforeAndAfterAll {

  private val host = "localhost"
  private val port = Random.between(49152, 65536)
  private val dictionaryServer = {
    val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))
    server.start()
    server
  }

  override def afterAll(): Unit = {
    dictionaryServer.stop()
  }

  override def beforeEach(): Unit = {
    dictionaryServer.resetAll()
    WireMock.configureFor(host, port)
  }

  private val testData = Table(
    ("word", "dictionaryResponseFile", "expectedDefinitions"),

    (Word("watermelon"), "oxford/oxford_dict_entries_watermelon.json", WatermelonDefinitions),
    (Word("lump"), "oxford/oxford_dict_entries_lump.json", LumpDefinitions)
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

      forAll(testData) ((word, dictionaryResponseFile, expectedDefinitions) => {

        givenThat(
          get(s"$path${word.chars}?strictMatch=false")
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("app_id", equalTo(appId))
            .withHeader("app_key", equalTo(appKey))
            .withQueryParam("strictMatch", equalTo("false"))
            .willReturn(aResponse().withBodyFile(dictionaryResponseFile)))

        Await.result({
          dictionary.translate(word).map(wordWithDefinitions =>

            wordWithDefinitions.definitions should contain theSameElementsAs expectedDefinitions
          )
        }, Duration(5, SECONDS))
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

      dictionary.translate(word).map(wordWithDefinition =>
        assert(wordWithDefinition.definitions.isEmpty)
      )
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

object OxfordEnglishDictionaryTest {

  private val WatermelonDefinitions = Iterable(
    WordDefinition(
      "noun",
      Iterable("plant", "food"),
      Iterable.empty,
      Iterable.empty,
      Iterable("the large fruit of a plant of the gourd family, with smooth green skin, red pulp, and watery juice."),
      Iterable.empty,
      Iterable("I like watermelons."),
      Iterable("big green berry"),
      Some("ˈwɔːtəmɛlən")),
    WordDefinition(
      "noun",
      Iterable("plant", "food"),
      Iterable.empty,
      Iterable.empty,
      Iterable("the African plant which yields watermelons."),
      Iterable.empty,
      Iterable.empty,
      Iterable.empty,
      Some("ˈwɔːtəmɛlən"))
  )

  private val LumpDefinitions = Iterable(
    WordDefinition(
      "noun",
      Iterable("foo"),
      Iterable.empty,
      Iterable.empty,
      Iterable("a compact mass of a substance, especially one without a definite or regular shape"),
      Iterable(
        "a swelling under the skin, especially one caused by injury or disease",
        "a small cube of sugar.",
        "a heavy, ungainly, or slow-witted person"),
      Iterable("there was a lump of ice floating in the milk"),
      Iterable(
        "chunk",
        "wedge",
        "hunk",
        "piece"
      ),
      Some("lʌmp")
    ),
    WordDefinition(
      "noun",
      Iterable.empty,
      Iterable.empty,
      Iterable("informal"),
      Iterable("the state of being self-employed and paid without deduction of tax, especially in the building industry"),
      Iterable.empty,
      Iterable(
        "‘Working?’ ‘Only on the lump, here and there’",
        "lump labour"
      ),
      Iterable.empty,
      Some("lʌmp")
    ),
    WordDefinition(
      "verb",
      Iterable.empty,
      Iterable.empty,
      Iterable.empty,
      Iterable(
        "put in an indiscriminate mass or group; treat as alike without regard for particulars"
      ),
      Iterable(
        "(in taxonomy) classify plants or animals in relatively inclusive groups, disregarding minor variations"
      ),
      Iterable(
        "Hong Kong and Bangkok tend to be lumped together in holiday brochures",
        "Nigel didn't like being lumped in with prisoners"
      ),
      Iterable(
        "combine",
        "put",
        "group"
      ),
      Some("lʌmp")
    ),
    WordDefinition(
      "verb",
      Iterable.empty,
      Iterable.empty,
      Iterable.empty,
      Iterable(
        "carry (a heavy load) somewhere with difficulty"
      ),
      Iterable.empty,
      Iterable(
        "the coalman had to lump one-hundredweight sacks right through the house"
      ),
      Iterable.empty,
      Some("lʌmp")
    ),
    WordDefinition(
      "verb",
      Iterable.empty,
      Iterable.empty,
      Iterable("informal"),
      Iterable(
        "accept or tolerate a disagreeable situation whether one likes it or not"
      ),
      Iterable.empty,
      Iterable(
        "you can like it or lump it but I've got to work"
      ),
      Iterable(
        "put up with it",
        "bear it"
      ),
      Some("lʌmp")
    )
  )
}
