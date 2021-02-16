package com.galekseev.dynalist_to_anki

import com.galekseev.dynalist_to_anki.model.Word
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.Json.parse
import play.api.libs.json._
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient

import java.net.URI
import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.{ExecutionContext, Future}

trait ListReader[T] {
  def read(): Future[Seq[T]]
}

class DynalistWordListReader(httpClient: AsyncHttpClient,
                             dynalistUri: URI,
                             listName: String,
                             dynalistApiKey: String)
                            (implicit executionContext: ExecutionContext)
  extends ListReader[Word] with StrictLogging {

  implicit val listReads: Reads[Word] = Json.reads[Word]

  private val data = Json.obj(
    "token" -> dynalistApiKey,
    "file_id" -> listName
  )

  override def read(): Future[Seq[Word]] = {

    val requestBody = Json.stringify(data)
    logger.info(s"Requesting words from Dynalist: $requestBody")

    val eventualResponse: Future[JsValue] = toScala(
      httpClient
        .preparePost(dynalistUri.toASCIIString)
        .addHeader("Content-Type", "application/json")
        .setBody(requestBody)
        .execute()
        .toCompletableFuture
    ).map(response => parse(response.getResponseBody))

    eventualResponse
      .map(json => (json \ "nodes")
        .validate[JsArray]
        .get
        .value
        .filter(v =>
          v.validate[JsObject]
            .map(o =>
              (o \ "id").validate[JsString]
                .map(id => id.value != "root")
                .get)
            .get
        ).map(a => (a \ "content").validate[JsString].map(_.value))
        .filter(_.isSuccess)
        .map(content => Word(content.get))
        .toSeq)
  }
}
