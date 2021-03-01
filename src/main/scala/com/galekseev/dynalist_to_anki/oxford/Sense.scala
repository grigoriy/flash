package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{JsValue, Reads}

case class Sense(definitions: Iterable[String],
                 maybeDomainClasses: Option[Iterable[DomainClass]],
                 maybeExamples: Option[Iterable[Example]],
                 maybeSynonyms: Option[Iterable[Synonym]],
                 maybeRegisters: Option[Iterable[Register]])

object Sense {
  implicit val reads: Reads[Sense] = (json: JsValue) =>
    for {
      definitions <- (json \ "definitions").validate[Iterable[String]]
      maybeDomainClasses <- (json \ "domainClasses").validateOpt[Iterable[DomainClass]]
      maybeExamples <- (json \ "examples").validateOpt[Iterable[Example]]
      maybeSynonyms <- (json \ "synonyms").validateOpt[Iterable[Synonym]]
      maybeRegisters <- (json \ "registers").validateOpt[Iterable[Register]]
    } yield Sense(definitions, maybeDomainClasses, maybeExamples, maybeSynonyms, maybeRegisters)
}
