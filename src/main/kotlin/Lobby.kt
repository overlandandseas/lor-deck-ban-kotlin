import com.google.gson.Gson

data class LobbyNumbers(val numberOfDecks: Int = 3, val numberOfBans: Int = 1) {
    fun toJson(): String = Gson().toJson(this)
}


data class Lobby(
    val lobbyNumbers: LobbyNumbers,
    var connected: Int = 0
) {
    var guestBans: String? = null
    var hostBans: String? = null
    var guestDecks: String? = null
    var hostDecks: String? = null
}


val LOBBY_MAP = HashMap<String, Lobby>()
