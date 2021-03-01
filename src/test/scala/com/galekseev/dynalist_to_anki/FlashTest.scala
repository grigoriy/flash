package com.galekseev.dynalist_to_anki

import com.galekseev.dynalist_to_anki.FlashTest._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.shaded.ahc.org.asynchttpclient.Dsl.asyncHttpClient

import java.net.URI
import scala.concurrent.ExecutionContext
import scala.util.Random

class FlashTest extends AsyncWordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val externalSystemHost = "localhost"
  private val externalSystemPort = Random.between(49152, 65536)
  private val externalSystemServer = {
    val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(externalSystemPort))
    server.start()
    server
  }

  override def afterAll(): Unit = {
    externalSystemServer.stop()
  }

  override def beforeEach(): Unit = {
    externalSystemServer.resetAll()
    WireMock.configureFor(externalSystemHost, externalSystemPort)
  }

  "ListToCardConverter.convert(N)" when {
    "given a Dynalist list with at least N words" should {
      "add N words into Anki" in {
        // Dynalist mock
        val dynalistApiKey = "dynalistApiKey"
        val dynalistListName = "5kDjKRflsQ64Rk8-0olenOkk"
        val dynalistUrlPath = "/api/v1/doc/read"
        val expectedDynalistRequestBody =
          s"""
             |{
             |  "token": "$dynalistApiKey",
             |  "file_id": "$dynalistListName"
             |}
             |""".stripMargin
        givenThat(post(dynalistUrlPath)
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(new EqualToJsonPattern(expectedDynalistRequestBody, true, false))
          .willReturn(aResponse().withBodyFile("dynalist_words_response.json")))

        // Oxford dictionary mock
        val dictionaryUrlPath = "/api/v2/entries/en-gb/"
        val dictionaryAppId = "14398df8"
        val dictionaryAppKey = "afc1b8d90cc428904eabb4271978ba41"

        givenThat(
          get(s"${dictionaryUrlPath}whence?strictMatch=false")
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("app_id", equalTo(dictionaryAppId))
            .withHeader("app_key", equalTo(dictionaryAppKey))
            .withQueryParam("strictMatch", equalTo("false"))
            .willReturn(aResponse().withBodyFile("oxford/oxford_dict_entries_whence.json")))
        givenThat(
          get(s"${dictionaryUrlPath}thence?strictMatch=false")
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("app_id", equalTo(dictionaryAppId))
            .withHeader("app_key", equalTo(dictionaryAppKey))
            .withQueryParam("strictMatch", equalTo("false"))
            .willReturn(aResponse().withBodyFile("oxford/oxford_dict_entries_thence.json")))

        val expectedAnkiResponse_1_Body =
          s"""
             |{
             |    "result": [1496198395707, null],
             |    "error": null
             |}
             |""".stripMargin
        val expectedAnkiResponse_2_Body =
          s"""
             |{
             |    "result": [1496198395709, null],
             |    "error": null
             |}
             |""".stripMargin
        givenThat(post("/")
          .withRequestBody(new EqualToJsonPattern(ThenceAnkiRequestBody, true, false))
          .willReturn(aResponse().withBody(expectedAnkiResponse_1_Body)))
        givenThat(post("/")
          .withRequestBody(new EqualToJsonPattern(WhenceAnkiRequestBody, true, false))
          .willReturn(aResponse().withBody(expectedAnkiResponse_2_Body)))

        implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
        val httpClient = asyncHttpClient()
        val converter = new Flash(
          new DynalistWordListReader(
            httpClient,
            URI.create(s"http://$externalSystemHost:$externalSystemPort$dynalistUrlPath"),
            dynalistListName,
            dynalistApiKey),
          new OxfordEnglishDictionary(httpClient,
            URI.create(s"http://$externalSystemHost:$externalSystemPort$dictionaryUrlPath"),
            dictionaryAppId,
            dictionaryAppKey),
          new AnkiWordWithDefinitionCardWriter(
            httpClient,
            URI.create(s"http://$externalSystemHost:$externalSystemPort"),
            AnkiApiVersion
          )
        )
        val maxNumWordsToConvert = 2

        converter.convert(maxNumWordsToConvert).map(response => {

          assert(response.result.size === maxNumWordsToConvert)
          assert(response.error.isEmpty)
        })
      }
    }

    "given a 3 words, 2 of which are not in Oxford Dictionary" should {
      "add 1 word into Anki" in {
        // Dynalist mock
        val dynalistApiKey = "dynalistApiKey"
        val dynalistListName = "5kDjKRflsQ64Rk8-0olenOkk"
        val dynalistUrlPath = "/api/v1/doc/read"
        val expectedDynalistRequestBody =
          s"""
             |{
             |  "token": "$dynalistApiKey",
             |  "file_id": "$dynalistListName"
             |}
             |""".stripMargin
        givenThat(post(dynalistUrlPath)
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(new EqualToJsonPattern(expectedDynalistRequestBody, true,false))
          .willReturn(aResponse().withBodyFile("dynalist_words_response.json")))

        // Oxford dictionary mock
        val dictionaryUrlPath = "/api/v2/entries/en-gb/"
        val dictionaryAppId = "14398df8"
        val dictionaryAppKey = "afc1b8d90cc428904eabb4271978ba41"

        givenThat(
          get(s"${dictionaryUrlPath}whence?strictMatch=false")
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("app_id", equalTo(dictionaryAppId))
            .withHeader("app_key", equalTo(dictionaryAppKey))
            .withQueryParam("strictMatch", equalTo("false"))
            .willReturn(aResponse().withBodyFile("oxford/oxford_dict_entries_whence.json")))
        givenThat(
          get(s"${dictionaryUrlPath}thence?strictMatch=false")
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("app_id", equalTo(dictionaryAppId))
            .withHeader("app_key", equalTo(dictionaryAppKey))
            .withQueryParam("strictMatch", equalTo("false"))
            .willReturn(aResponse().withStatus(404).withBodyFile("oxford/oxford_dict_entries_lkwefl34.json")))
        givenThat(
          get(s"${dictionaryUrlPath}mosh?strictMatch=false")
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("app_id", equalTo(dictionaryAppId))
            .withHeader("app_key", equalTo(dictionaryAppKey))
            .withQueryParam("strictMatch", equalTo("false"))
            .willReturn(aResponse().withStatus(404).withBodyFile("oxford/oxford_dict_entries_lkwefl34.json")))

        val expectedAnkiResponse_1_Body = s"""
                                             |{
                                             |    "result": [1496198395707, null],
                                             |    "error": null
                                             |}
                                             |""".stripMargin
        val expectedAnkiResponse_2_Body = s"""
                                             |{
                                             |    "result": [1496198395709, null],
                                             |    "error": null
                                             |}
                                             |""".stripMargin
        givenThat(post("/")
          .withRequestBody(new EqualToJsonPattern(ThenceAnkiRequestBody, true, false))
          .willReturn(aResponse().withBody(expectedAnkiResponse_1_Body)))
        givenThat(post("/")
          .withRequestBody(new EqualToJsonPattern(WhenceAnkiRequestBody, true, false))
          .willReturn(aResponse().withBody(expectedAnkiResponse_2_Body)))

        implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
        val httpClient = asyncHttpClient()
        val converter = new Flash(
          new DynalistWordListReader(
            httpClient,
            URI.create(s"http://$externalSystemHost:$externalSystemPort$dynalistUrlPath"),
            dynalistListName,
            dynalistApiKey),
          new OxfordEnglishDictionary(httpClient,
            URI.create(s"http://$externalSystemHost:$externalSystemPort$dictionaryUrlPath"),
            dictionaryAppId,
            dictionaryAppKey),
          new AnkiWordWithDefinitionCardWriter(
            httpClient,
            URI.create(s"http://$externalSystemHost:$externalSystemPort"),
            AnkiApiVersion
          )
        )

        converter.convert(3).map(response => {

          assert(response.result.size === 1)
          assert(response.error.isDefined)
          assert(response.error.get.contains("thence"))
          assert(response.error.get.contains("mosh"))
        })
      }
    }
  }
}

object FlashTest {
  private val AnkiApiVersion = 6
  private val AnkiDeckName = "English::My English"
  private val AnkiModelName = "English new words"
  private val WhenceAnkiRequestBody = s"""
                                         |{
                                         |  "action" : "addNotes",
                                         |  "version" : $AnkiApiVersion,
                                         |  "params" : {
                                         |    "notes" : [
                                         |    {
                                         |      "deckName" : \"$AnkiDeckName\",
                                         |      "modelName" : \"$AnkiModelName\",
                                         |      "fields" : {
                                         |        "Word" : "whence (adverb; interrogative; formal, archaic)",
                                         |        "Phonetic symbol" : "wɛns",
                                         |        "Definition" : "from what place or source",
                                         |        "Examples" : "Whence does Parliament derive this power?",
                                         |        "Cloze" : "[...] does Parliament derive this power?",
                                         |        "Synonyms" : ""
                                         |      }
                                         |    },
                                         |    {
                                         |      "deckName" : \"$AnkiDeckName\",
                                         |      "modelName" : \"$AnkiModelName\",
                                         |      "fields" : {
                                         |        "Word" : "whence (adverb; relative; formal, archaic)",
                                         |        "Phonetic symbol" : "wɛns",
                                         |        "Definition" : "* from which; from where<br/>* to the place from which<br/>* as a consequence of which",
                                         |        "Examples" : "the Ural mountains, whence the ore is procured",
                                         |        "Cloze" : "the Ural mountains, [...] the ore is procured",
                                         |        "Synonyms" : ""
                                         |      }
                                         |    }]
                                         |  }
                                         |}
                                         |""".stripMargin

  private val ThenceAnkiRequestBody = s"""
                                         |{
                                         |  "action" : "addNotes",
                                         |  "version" : $AnkiApiVersion,
                                         |  "params" : {
                                         |    "notes" : [
                                         |    {
                                         |      "deckName" : \"$AnkiDeckName\",
                                         |      "modelName" : \"$AnkiModelName\",
                                         |      "fields" : {
                                         |        "Word" : "thence (adverb; formal)",
                                         |        "Phonetic symbol" : "ðɛns",
                                         |        "Definition" : "* from a place or source previously mentioned<br/>* as a consequence",
                                         |        "Examples" : "they intended to cycle on into France and thence home via Belgium",
                                         |        "Cloze" : "they intended to cycle on into France and [...] home via Belgium",
                                         |        "Synonyms" : "foo, bar"
                                         |      }
                                         |    } ]
                                         |  }
                                         |}
                                         |""".stripMargin
}
