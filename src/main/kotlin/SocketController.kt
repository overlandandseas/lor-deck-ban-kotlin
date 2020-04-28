import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import java.util.concurrent.ConcurrentHashMap


private val playerSessionMap = ConcurrentHashMap<String, ArrayList<WsContext>>()

const val DELIMITER = ';'

object SocketController {

    fun onConnect(ctx: WsConnectContext) {
        val roomId = ctx.pathParam("room-id")

        val lobby = LOBBY_MAP[roomId] ?: createNew(roomId)

        if (playerSessionMap.containsKey(roomId))
            playerSessionMap[roomId]?.add(ctx)
        else
            playerSessionMap[roomId] = arrayListOf(ctx as WsContext)

        lobby.connected++
        val connected = lobby.connected

        sendAll(roomId, "CONNECTED$DELIMITER$connected")

        val roomState = when (connected) {
            1 -> "NEW_LOBBY"
            2 -> "LOBBY_FOUND"
            else -> "LOBBY_FULL"
        }

        ctx.send(roomState)

        val lobbyJson = lobby.lobbyNumbers.toJson()

        ctx.send("ROOM_INFO$DELIMITER$lobbyJson")

        if (allDecksSubmitted(lobby)) {
            val hostDecks = lobby.hostDecks
            val guestDecks = lobby.guestDecks

            sendAll(roomId, "DECKS${DELIMITER}HOST$DELIMITER${hostDecks}")
            sendAll(roomId, "DECKS${DELIMITER}GUEST$DELIMITER${guestDecks}")
        }

        if (allBanned(lobby)) {

            val hostBans = lobby.hostBans
            val guestBans = lobby.guestBans

            sendAll(roomId, "BANNED${DELIMITER}HOST$DELIMITER${hostBans}")
            sendAll(roomId, "BANNED${DELIMITER}GUEST$DELIMITER${guestBans}")
        }

    }

    fun onMessage(ctx: WsMessageContext) {
        val roomId = ctx.pathParam("room-id")
        val splitMsg = ctx.message().split(DELIMITER)

        val lobby = LOBBY_MAP[roomId] ?: createNew(roomId)

        val action = splitMsg[0]
        val role = if (splitMsg.size > 1) splitMsg[1] else "default_role"
        val data = if (splitMsg.size > 2) splitMsg[2] else "default_data"

        when (action) {

            // When a player submit's their decks, alert everyone, check to see if all have been submitted
            // and submit all of those to everyone
            "DECK_SUBMIT" -> {
                submitDecks(lobby, role, data)
                sendAll(roomId, "DECK_SUBMITTED$DELIMITER$role")

                if (allDecksSubmitted(lobby)) {
                    val hostDecks = lobby.hostDecks
                    val guestDecks = lobby.guestDecks

                    sendAll(roomId, "DECKS${DELIMITER}HOST$DELIMITER${hostDecks}")
                    sendAll(roomId, "DECKS${DELIMITER}GUEST$DELIMITER${guestDecks}")
                }
            }

            // When a ban is submitted from a player, alert everyone and check if all have been submitted
            // then alert everyone again
            "BAN_SUBMIT" -> {
                banDeck(lobby, role, data)
                sendAll(roomId, "BAN_SUBMITTED$DELIMITER$role")

                if (allBanned(lobby)) {

                    val hostBans = lobby.hostBans
                    val guestBans = lobby.guestBans

                    sendAll(roomId, "BANNED${DELIMITER}HOST$DELIMITER${hostBans}")
                    sendAll(roomId, "BANNED${DELIMITER}GUEST$DELIMITER${guestBans}")
                }
            }
            "PING" -> {
                ctx.send("PONG")
            }
            else -> {
                println("Unknown Request")
            }
        }
    }

    fun onClose(ctx: WsCloseContext) {

        val roomId = ctx.pathParam("room-id")
        val lobby = LOBBY_MAP[roomId] ?: createNew(roomId)

        lobby.connected--
        val connected = lobby.connected

        sendAll(roomId, "CONNECTED$DELIMITER$connected")
        playerSessionMap[roomId]?.remove(ctx)

        if (connected == 0 && allBanned(lobby)) LOBBY_MAP.remove(roomId)
    }
}

private fun submitDecks(lobby: Lobby, role: String, decks: String) {
    when (role) {
        "HOST" -> lobby.hostDecks = decks
        "GUEST" -> lobby.guestDecks = decks
    }
}

private fun sendAll(roomId: String, message: String) {
    playerSessionMap[roomId]?.filter { it.session.isOpen }?.forEach { it.send(message) }
}

private fun allDecksSubmitted(lobby: Lobby): Boolean {
    return lobby.hostDecks !== null && lobby.guestDecks !== null
}


private fun banDeck(lobby: Lobby, role: String, bannedDeck: String) {
    when (role) {
        "HOST" -> lobby.hostBans = bannedDeck
        "GUEST" -> lobby.guestBans = bannedDeck
    }
}

private fun allBanned(lobby: Lobby): Boolean {
    return lobby.hostBans !== null && lobby.guestBans !== null
}


private fun createNew(roomId: String): Lobby {
    val lobby = Lobby(LobbyNumbers())
    LOBBY_MAP[roomId] = lobby
    return lobby
}