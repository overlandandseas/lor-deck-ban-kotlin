import io.javalin.Javalin
import redis.clients.jedis.Jedis

fun main(args: Array<String>) {

    val redis = Jedis("localhost");

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
            ws.onConnect(PlayerController::onConnect)
            ws.onMessage(PlayerController::onMessage)
            ws.onClose(PlayerController::onClose)
        }
    }

    app.start(7000)
}

fun getRandomRoomId(roomIdLength: Int): String =
        (0..roomIdLength).map { (65 + Math.random() * 26).toChar()}.joinToString("")