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
import scala.util.Random

// scalastyle:off
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
        Iterable.empty))
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
  }
}
