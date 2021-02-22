package com.galekseev.dynalist_to_anki

import com.galekseev.dynalist_to_anki.anki.AnkiCreateNotesResponse
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class Flash[A, B](listReader: ListReader[A],
            dictionary: Dictionary[A, B],
            cardWriter: CardWriter[B, AnkiCreateNotesResponse])
           (implicit executionContext: ExecutionContext)
  extends StrictLogging {

  def convert(maxNumWordsToConvert: Int): Future[AnkiCreateNotesResponse] =
    for {
      list <- listReader.read()
      response <- {
        logger.info(s"Got words from Dynalist: $list")

        list.take(maxNumWordsToConvert)
          .map(word => {
            // satisfy the Oxford Dictionary request rate limit
            Thread.sleep(500) // scalastyle:ignore
            logger.info(s"Translating $word")
            dictionary.translate(word)
          })

          .map(eventualCard =>
            eventualCard.flatMap(card => {
              logger.info(s"Writing a note into Anki: $card")
              cardWriter.write(Seq(card))
            })
          )
          .foldLeft(successful(AnkiCreateNotesResponse(Iterable.empty, None)))((a, b) =>
            for {
              aResult <- a
              bResult <- b.recover({ case e => AnkiCreateNotesResponse(Iterable.empty, Some(e.getMessage)) })
            } yield AnkiCreateNotesResponse(
              aResult.result ++ bResult.result,
              aResult.error.flatMap(aError => bResult.error.map(aError + "\n" + _).orElse(aResult.error)).orElse(bResult.error))
          )
      }
    } yield response
}
