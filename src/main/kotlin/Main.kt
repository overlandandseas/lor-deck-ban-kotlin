import controller.RoomController
import controller.SocketController
import io.javalin.Javalin
import java.util.*


fun main(args: Array<String>) {

    val port = System.getenv("PORT")?.toInt() ?: 7000

    val app = Javalin.create { config ->
        config.enableCorsForAllOrigins()
    }


    /**
     * New Runeterraban Endpoints
     */
    app.apply {
        post("/room", RoomController::createRoom)

        get("/user") { ctx ->
            val userId = UUID.randomUUID().toString()
            ctx.result(userId)
        }

        ws("/websocket/:room-name/:user") { ws ->
            ws.onConnect(SocketController::onConnect)
            ws.onMessage(SocketController::onMessage)
            ws.onClose(SocketController::onClose)
        }

    }

    app.start(port)

}
