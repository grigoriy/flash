package com.galekseev.dynalist_to_anki.oxford

import play.api.libs.json.{Json, Reads}

case class Register(id: String)

object Register {
  implicit val reads: Reads[Register] = Json.reads[Register]
}
