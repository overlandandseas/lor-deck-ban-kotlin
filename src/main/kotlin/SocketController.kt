import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import java.util.concurrent.ConcurrentHashMap


private val playerSessionMap = ConcurrentHashMap<String, ArrayList<WsContext>>()
private val redis = Redis.instance

object SocketController {

    fun onConnect(ctx: WsConnectContext) {
        val roomId = ctx.pathParam("room-id")
        if (playerSessionMap.containsKey(roomId))
            playerSessionMap[roomId]?.add(ctx)
        else
            playerSessionMap[roomId] = arrayListOf(ctx as WsContext)

        val connected = redis.hincrBy(roomId, "connected", 1).toInt()
        sendAll(roomId, "Connected: $connected")

        val roomState = when (connected) {
            1 -> "NEW_LOBBY"
            2 -> "LOBBY_FOUND"
            else -> "LOBBY_FULL"
        }

        ctx.send(roomState)

        val numberOfDecks = redis.hget(roomId, "numberOfDecks").toInt()
        val numberOfBans = redis.hget(roomId, "numberOfBans").toInt()
        val lobbyJson = Lobby(numberOfDecks, numberOfBans).toJson()

        ctx.send("ROOM_INFO:$lobbyJson")

    }

    fun onMessage(ctx: WsMessageContext) {
        val roomId = ctx.pathParam("room-id")
        val splitMsg = ctx.message().split(":")

        val action = splitMsg[0]
        val role = splitMsg[1]
        val data = splitMsg[2]

        when (action) {

            // When a player submit's their decks, alert everyone, check to see if all have been submitted
            // and submit all of those to everyone
            "DECK_SUBMIT" -> {
                submitDecks(roomId, role, data.split("|"))
                sendAll(roomId, "DECK_SUBMITTED:$role")
                val numberOfDecks = redis.hget(roomId, "numberOfDecks").toInt()
                if (allDecksSubmitted(roomId, numberOfDecks)) {

                    val hostDecks = redis.hmget(roomId, *(0..numberOfDecks).map { "HOST_deck$it" }.toTypedArray())
                    val guestDecks = redis.hmget(roomId, *(0..numberOfDecks).map { "GUEST_deck$it" }.toTypedArray())

                    sendAll(roomId, "DECKS:HOST:${hostDecks.joinToString("|")}")
                    sendAll(roomId, "DECKS:GUEST:${guestDecks.joinToString("|")}")
                }
            }

            // When a ban is submitted from a player, alert everyone and check if all have been submitted
            // then alert everyone again
            "BAN_SUBMIT" -> {
                banDeck(roomId, role, data)
                sendAll(roomId, "BAN_SUBMITTED:$role")

                if (allBanned(roomId)) {
                    val bannedDecks = redis.hmget(roomId, "BAN_DECK:HOST", "BAN_DECK:GUEST")
                    bannedDecks.forEach {
                        sendAll(roomId, "BANNED:HOST:$it")
                    }
                }
            }
        }
    }

    fun onClose(ctx: WsCloseContext) {
        val roomId = ctx.pathParam("room-id")
        val connected = redis.hincrBy(roomId, "connected", -1).toInt()
        sendAll(roomId, "Connected: $connected")
        playerSessionMap[roomId]?.remove(ctx)
        if (connected == 0) {
            redis.hdel(roomId)
        }
    }
}

private fun submitDecks(roomId: String, role: String, decks: List<String>) {
    val deckMap = decks.mapIndexed { idx, value ->
        Pair("${role}_deck$idx", value)
    }.toMap()
    redis.hmset(roomId, deckMap)
}

private fun sendAll(roomId: String, message: String) {
    playerSessionMap[roomId]?.filter { it.session.isOpen }?.forEach { it.send(message) }
}

private fun allDecksSubmitted(roomId: String, numberOfDecks: Int): Boolean = (0..numberOfDecks).all {
    redis.hexists(roomId, "HOST_deck$it")
}

private fun banDeck(roomId: String, role: String, bannedDeck: String) {
    redis.hset(roomId, "BAN_DECK:$role", bannedDeck)
}

private fun allBanned(roomId: String): Boolean =
    redis.hexists(roomId, "BAN_DECK:HOST") && redis.hexists(roomId, "BAN_DECK:GUEST")
