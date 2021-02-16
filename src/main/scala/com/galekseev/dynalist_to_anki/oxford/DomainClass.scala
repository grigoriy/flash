package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{Json, Reads}

case class DomainClass(id: String)

object DomainClass {
  implicit val reads: Reads[DomainClass] = Json.reads[DomainClass]
}
