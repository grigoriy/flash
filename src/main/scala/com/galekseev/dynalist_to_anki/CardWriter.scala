package com.galekseev.dynalist_to_anki

import com.galekseev.dynalist_to_anki.anki.{AnkiCreateNotesResponse, WordAnkiNote, WordAnkiNoteFields, WordAnkiNotesRequest}
import com.galekseev.dynalist_to_anki.model.WordWithDefinition
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient

import java.net.URI
import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.{ExecutionContext, Future}

trait CardWriter[A, B] {
  def write(cards: Seq[A]): Future[B]
}

class AnkiWordWithDefinitionCardWriter(httpClient: AsyncHttpClient, uri: URI, apiVersion: Int)
                                      (implicit executionContext: ExecutionContext)
  extends CardWriter[WordWithDefinition, AnkiCreateNotesResponse] with StrictLogging {

  override def write(cards: Seq[WordWithDefinition]): Future[AnkiCreateNotesResponse] = {
    val notesWriteRequest = WordAnkiNotesRequest(cards.map(card => WordAnkiNote(WordAnkiNoteFields(card))), apiVersion)
    val requestBody = Json.stringify(toJson(notesWriteRequest))
    logger.info(s"Writing notes into Anki: $requestBody")

    toScala(
      httpClient.preparePost(uri.toASCIIString)
        .setBody(requestBody)
        .execute()
        .toCompletableFuture
    ).map(response =>
      Json.parse(response.getResponseBody)
        .validate[AnkiCreateNotesResponse]
        .getOrElse(throw new RuntimeException(s"Failed to parse the Anki response: ${response.getResponseBody}"))
    )
  }
}
