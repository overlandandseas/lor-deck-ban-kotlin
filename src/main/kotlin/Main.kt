import io.javalin.Javalin


fun main(args: Array<String>) {

    val port = System.getenv("PORT")?.toInt() ?: 7000

    val app = Javalin.create { config ->
        config.enableCorsForAllOrigins()
    }
    app.get("/") { ctx -> ctx.result("Hey all you cool cats and kittens") }
    app.post("/") { ctx ->

        val roomIdLength = ctx.queryParam("roomIdLength", "4")?.toInt() ?: 4

        var roomId = getRandomRoomId(roomIdLength)
        while (LOBBY_MAP.containsKey(roomId)) {
            roomId = getRandomRoomId(roomIdLength)
        }
        val lobbyNumbers = ctx.bodyAsClass(LobbyNumbers::class.java)

        LOBBY_MAP[roomId] = Lobby(lobbyNumbers)

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