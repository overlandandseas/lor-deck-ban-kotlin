package model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerRoomState (
    val yourDecks: List<String>?,
    val otherDecks: List<List<String>>?,
    val yourBans: List<String>?,
    val otherBans: List<List<String>>?,
    val connected: Int,
    val numberOfDecks: Int,
    val numberOfBans: Int
)