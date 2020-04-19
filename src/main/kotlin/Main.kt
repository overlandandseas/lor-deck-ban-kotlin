import com.google.gson.Gson
import io.javalin.Javalin

data class Lobby(val numberOfDecks: Int = 3, val numberOfBans: Int = 1) {
    fun toJson() {
        Gson().toJson(this)
    }
}

fun main(args: Array<String>) {

    val redis = Redis.instance

    val app =  Javalin.create ()
    app.get("/") { ctx -> ctx.result("It's cool dude, everything's cool")}
    app.post("/") {ctx ->

        val roomIdLength = ctx.queryParam<Int>("roomIdLength").get()

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
        ws("/websocket/:room_id") { ws  ->
            ws.onConnect(SocketController::onConnect)
            ws.onMessage(SocketController::onMessage)
            ws.onClose(SocketController::onClose)
        }
    }

    app.start(7000)
}

fun getRandomRoomId(roomIdLength: Int): String =
        (0..roomIdLength).map { (65 + Math.random() * 26).toChar()}.joinToString("")