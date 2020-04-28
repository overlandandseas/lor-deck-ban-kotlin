import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import redis.clients.jedis.Jedis
import java.util.concurrent.ConcurrentHashMap


private val playerSessionMap = ConcurrentHashMap<String, ArrayList<WsContext>>()

const val DELIMITER = ';'

object SocketController {

    fun onConnect(ctx: WsConnectContext, redis: Jedis) {
        val roomId = ctx.pathParam("room-id")
        if (playerSessionMap.containsKey(roomId))
            playerSessionMap[roomId]?.add(ctx)
        else
            playerSessionMap[roomId] = arrayListOf(ctx as WsContext)

        val connected = redis.hincrBy(roomId, "connected", 1).toInt()
        sendAll(roomId, "CONNECTED$DELIMITER$connected")

        val roomState = when (connected) {
            1 -> "NEW_LOBBY"
            2 -> "LOBBY_FOUND"
            else -> "LOBBY_FULL"
        }

        ctx.send(roomState)

        val numberOfDecks = redis.hget(roomId, "numberOfDecks")?.toInt() ?: 3
        val numberOfBans = redis.hget(roomId, "numberOfBans")?.toInt() ?: 1
        val lobbyJson = Lobby(numberOfDecks, numberOfBans).toJson()

        ctx.send("ROOM_INFO$DELIMITER$lobbyJson")

        if (allDecksSubmitted(roomId, redis)) {
            val decks = redis.hmget(roomId, "DECKS_HOST", "DECKS_GUEST")

            sendAll(roomId, "DECKS${DELIMITER}HOST$DELIMITER${decks[0]}")
            sendAll(roomId, "DECKS${DELIMITER}GUEST$DELIMITER${decks[1]}")
        }

        if (allBanned(roomId, redis)) {
            val bannedDecks = redis.hmget(roomId, "BAN_HOST", "BAN_GUEST")

            sendAll(roomId, "BANNED${DELIMITER}HOST$DELIMITER${bannedDecks[0]}")
            sendAll(roomId, "BANNED${DELIMITER}GUEST$DELIMITER${bannedDecks[1]}")
        }

    }

    fun onMessage(ctx: WsMessageContext, redis: Jedis) {
        val roomId = ctx.pathParam("room-id")
        val splitMsg = ctx.message().split(DELIMITER)

        val action = splitMsg[0]
        val role = if (splitMsg.size > 1) splitMsg[1] else "default_role"
        val data = if (splitMsg.size > 2) splitMsg[2] else "default_data"

        when (action) {

            // When a player submit's their decks, alert everyone, check to see if all have been submitted
            // and submit all of those to everyone
            "DECK_SUBMIT" -> {
                submitDecks(roomId, role, data, redis)
                sendAll(roomId, "DECK_SUBMITTED$DELIMITER$role")

                if (allDecksSubmitted(roomId, redis)) {
                    val decks = redis.hmget(roomId, "DECKS_HOST", "DECKS_GUEST")

                    sendAll(roomId, "DECKS${DELIMITER}HOST$DELIMITER${decks[0]}")
                    sendAll(roomId, "DECKS${DELIMITER}GUEST$DELIMITER${decks[1]}")

                }
            }

            // When a ban is submitted from a player, alert everyone and check if all have been submitted
            // then alert everyone again
            "BAN_SUBMIT" -> {
                banDeck(roomId, role, data, redis)
                sendAll(roomId, "BAN_SUBMITTED$DELIMITER$role")

                if (allBanned(roomId, redis)) {
                    val bannedDecks = redis.hmget(roomId, "BAN_HOST", "BAN_GUEST")

                    sendAll(roomId, "BANNED${DELIMITER}HOST$DELIMITER${bannedDecks[0]}")
                    sendAll(roomId, "BANNED${DELIMITER}GUEST$DELIMITER${bannedDecks[1]}")

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

    fun onClose(ctx: WsCloseContext, redis: Jedis) {
        val roomId = ctx.pathParam("room-id")
        val connected = redis.hincrBy(roomId, "connected", -1).toInt()
        sendAll(roomId, "CONNECTED$DELIMITER$connected")
        playerSessionMap[roomId]?.remove(ctx)
        redis.close()
//        if (connected == 0) {
//            redis.del(roomId)
//        }
    }
}

private fun submitDecks(roomId: String, role: String, decks: String, redis: Jedis) {
    redis.hset(roomId, "DECKS_$role", decks)
}

private fun sendAll(roomId: String, message: String) {
    playerSessionMap[roomId]?.filter { it.session.isOpen }?.forEach { it.send(message) }
}

private fun allDecksSubmitted(roomId: String, redis: Jedis): Boolean {
    return redis.hexists(roomId, "DECKS_HOST") && redis.hexists(roomId, "DECKS_GUEST")
}

private fun banDeck(roomId: String, role: String, bannedDeck: String, redis: Jedis) {
    redis.hset(roomId, "BAN_$role", bannedDeck)
}

private fun allBanned(roomId: String, redis: Jedis): Boolean =
    redis.hexists(roomId, "BAN_HOST") && redis.hexists(roomId, "BAN_GUEST")
