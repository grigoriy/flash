package com.galekseev.dynalist_to_anki

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

class ListToCardConverter[A, B, C](listReader: ListReader[A],
                                   dictionary: Dictionary[A, B],
                                   cardWriter: CardWriter[B, C])
                                  (implicit executionContext: ExecutionContext)
  extends StrictLogging {

  def convert(maxNumWordsToConvert: Int): Future[C] =
    for {
      list <- listReader.read()
      cards <- {
        logger.info(s"Got words from Dynalist: $list")
        Future.sequence(list.take(maxNumWordsToConvert).map(word => {
          Thread.sleep(500) // scalastyle:ignore (to satisfy the Oxford Dictionary request rate limit)
          logger.info(s"Translating $word")
          dictionary.translate(word)
        }))
      }
      ankiResponse <- {
        logger.info(s"Writing notes into Anki: $cards")
        cardWriter.write(cards)
      }
    } yield {
      logger.info(s"Anki replied: $ankiResponse")
      ankiResponse
    }
}
