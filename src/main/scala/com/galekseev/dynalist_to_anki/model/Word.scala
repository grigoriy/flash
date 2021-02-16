package com.galekseev.dynalist_to_anki.model


case class Word(private val _chars: String) {
  val chars: String = _chars.toLowerCase
}
