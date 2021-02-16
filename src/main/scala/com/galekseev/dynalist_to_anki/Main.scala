package com.galekseev.dynalist_to_anki

import com.typesafe.config.ConfigFactory
import play.shaded.ahc.org.asynchttpclient.Dsl.asyncHttpClient

import java.lang.Integer.parseInt
import java.net.URI
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}

object Main extends App {

  private val httpClient = asyncHttpClient()

  try {
    val config = ConfigFactory.load()
    val wordListConfig = config.getConfig("words")
    val dictionaryConfig = config.getConfig("dictionary")
    val cardsConfig = config.getConfig("cards")
    val maxWordsToConvert =
      if (args.nonEmpty)
        parseInt(args(0))
      else
        config.getInt("max-num-words-to-convert")

    val converter = new Flash(
      new DynalistWordListReader(
        httpClient,
        URI.create(wordListConfig.getString("read-url")),
        wordListConfig.getString("list-name"),
        wordListConfig.getString("api-key")
      ),
      new OxfordEnglishDictionary(
        httpClient,
        URI.create(dictionaryConfig.getString("entries-url")),
        dictionaryConfig.getString("app-id"),
        dictionaryConfig.getString("app-key")
      ),
      new AnkiWordWithDefinitionCardWriter(
        httpClient,
        URI.create(cardsConfig.getString("url")),
        cardsConfig.getInt("api-version")
      )
    )

    Await.result(converter.convert(maxWordsToConvert), Duration(10L + maxWordsToConvert * 2, SECONDS))

  } finally {
    httpClient.close()
  }
}
