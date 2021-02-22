package com.galekseev.dynalist_to_anki

import com.galekseev.dynalist_to_anki.model.Word
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
import scala.util.Random

class DynalistWordListReaderTest extends AsyncWordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val host = "localhost"
  private val port = Random.between(49152, 65536)
  private val dynalist = {
    val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))
    server.start()
    server
  }

  override def afterAll(): Unit =
    dynalist.stop()

  override def beforeEach(): Unit = {
    dynalist.resetAll()
    WireMock.configureFor(host, port)
  }

  "DynalistWordListReader.read()" should {
    "return words" in {
      val dynalistApiKey = "dynalistApiKey"
      val listName = "5kDjKRflsQ64Rk8-0olenOkk"
      val urlPath = "/api/v1/doc/read"

      val expectedRequestBody =
        s"""
           |{
           |  "token": "$dynalistApiKey",
           |  "file_id": "$listName"
           |}
           |""".stripMargin
      givenThat(post(urlPath)
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(new EqualToJsonPattern(expectedRequestBody, true,false))
        .willReturn(aResponse().withBodyFile("dynalist_words_response.json")))

      val reader = new DynalistWordListReader(
        asyncHttpClient(),
        URI.create(s"http://$host:$port$urlPath"),
        listName,
        dynalistApiKey)

      reader.read().map(wordList => {

        wordList should have size 3
        wordList should contain theSameElementsAs Seq(Word("whence"), Word("thence"), Word("mosh"))
      })
    }
  }
}
