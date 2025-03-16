package com.bharatballot.securevote.data

data class Candidate(
    val id: String = "",
    val name: String = "",
    val party: String = "",
    val symbol: String = "", // URL or resource identifier for party symbol
    val manifesto: String = "" // Brief description of the candidate's agenda
)
