package model

data class Room(val name: String, val players: MutableMap<String, Player>, val numberOfDecks: Int, val numberOfBans: Int)