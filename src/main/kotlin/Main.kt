import com.google.gson.Gson
import io.javalin.Javalin

data class Lobby(val numberOfDecks: Int = 3, val numberOfBans: Int = 1) {
    fun toJson(): String = Gson().toJson(this)
}

fun main(args: Array<String>) {

    val redis = Redis.instance

    val port = System.getenv("PORT")?.toInt() ?: 7000

    val app = Javalin.create{ config ->
        config.enableCorsForAllOrigins()
    }
    app.get("/") { ctx -> ctx.result("Hey all you cool cats and kittens") }
    app.post("/") { ctx ->
        val roomIdLength = ctx.queryParam("roomIdLength", "4")?.toInt() ?: 4


        var roomId = getRandomRoomId(roomIdLength)
        while (redis.exists(roomId)) {
            roomId = getRandomRoomId(roomIdLength)
        }
        val lobby = ctx.bodyAsClass(Lobby::class.java)

        redis.hset(roomId, "numberOfDecks", lobby.numberOfDecks.toString())
        redis.hset(roomId, "numberOfBans", lobby.numberOfBans.toString())

        ctx.result(roomId)

    }
    app.apply {
        ws("/websocket/:room-id") { ws ->
            ws.onConnect(SocketController::onConnect)
            ws.onMessage(SocketController::onMessage)
            ws.onClose(SocketController::onClose)
        }
    }


    app.start(port)
}


fun getRandomRoomId(roomIdLength: Int): String =
    (0 until roomIdLength).map { (65 + Math.random() * 26).toChar() }.joinToString("")