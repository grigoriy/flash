package com.galekseev.dynalist_to_anki

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

// scalastyle:off
class ListToCardConverterTest extends AsyncWordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

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

  "ListToCardConverter.convert(1)" when {
    "given a Dynalist list with at least one word" should {
      "add a word into Anki" in {
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
            .willReturn(aResponse().withBodyFile("oxford/oxford_dict_entries_thence.json")))

        // Anki mock
        val ankiApiVersion = 6
        val ankiDeckName = "English::My English"
        val ankiModelName = "English new words"
        val expectedAnkiRequestBody = s"""
                                         |{
                                         |  "action" : "addNotes",
                                         |  "version" : $ankiApiVersion,
                                         |  "params" : {
                                         |    "notes" : [
                                         |    {
                                         |      "deckName" : \"$ankiDeckName\",
                                         |      "modelName" : \"$ankiModelName\",
                                         |      "fields" : {
                                         |        "Word" : "thence",
                                         |        "Phonetic symbol" : "ðɛns",
                                         |        "Definition" : "from place or source previously mentioned",
                                         |        "Extra information" : "they intended to cycle on into France and thence home via Belgium"
                                         |    }
                                         |  }, {
                                         |      "deckName" : \"$ankiDeckName\",
                                         |      "modelName" : \"$ankiModelName\",
                                         |      "fields" : {
                                         |        "Word" : "whence",
                                         |        "Phonetic symbol" : "wɛns",
                                         |        "Definition" : "* from what place or source<br/>* from which",
                                         |        "Extra information" : "* whence does Parliament derive this power?<br/>* the Ural mountains, whence the ore is procured"
                                         |      }
                                         |    } ]
                                         |  }
                                         |}
                                         |""".stripMargin
        val expectedAnkiResponseBody = s"""
                                          |{
                                          |    "result": [1496198395707, 1496198395709, null],
                                          |    "error": null
                                          |}
                                          |""".stripMargin
        givenThat(post("/")
          .withRequestBody(new EqualToJsonPattern(expectedAnkiRequestBody, true,false))
          .willReturn(aResponse().withBody(expectedAnkiResponseBody)))

        implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
        val httpClient = asyncHttpClient()
        val converter = new ListToCardConverter(
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
            ankiApiVersion
          )
        )
        val maxNumWordsToConvert = 2

        converter.convert(maxNumWordsToConvert).map(response => {

          assert(response.result.size === maxNumWordsToConvert)
          assert(response.error.isEmpty)
        })
      }
    }
  }
}
