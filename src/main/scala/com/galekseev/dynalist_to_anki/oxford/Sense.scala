package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{JsValue, Reads}

case class Sense(shortDefinitions: Iterable[String],
                 maybeDomainClasses: Option[Iterable[DomainClass]],
                 maybeExamples: Option[Iterable[Example]],
                 maybeSynonyms: Option[Iterable[Synonym]])

object Sense {
  implicit val reads: Reads[Sense] = (json: JsValue) =>
    for {
      shortDefinitions <- (json \ "shortDefinitions").validate[Iterable[String]]
      maybeDomainClasses <- (json \ "domainClassses").validateOpt[Iterable[DomainClass]]
      maybeExamples <- (json \ "examples").validateOpt[Iterable[Example]]
      maybeSynonyms <- (json \ "synonyms").validateOpt[Iterable[Synonym]]
    } yield Sense(shortDefinitions, maybeDomainClasses, maybeExamples, maybeSynonyms)
}
