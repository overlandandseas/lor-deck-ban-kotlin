package controller

import RoomIsFullException
import RoomNotFoundException
import UserHeaderNotFoundException
import client.RoomClient
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsMessageContext


object SocketController {

    private const val ACTION_DELIMITER = ";"

    private const val DECK_CODE_DELIMITER = ","

    fun onConnect(ctx: WsConnectContext) {
        val roomName = ctx.pathParam("room-name")

        val userId = ctx.pathParam("user")

        try {
            RoomClient.connect(roomName, userId, ctx)
        } catch (e: Exception) {
            when (e) {
                is RoomNotFoundException -> print("Room not found")
                is RoomIsFullException -> print("Room is full")
            }
        }


    }

    fun onMessage(ctx: WsMessageContext) {
        val roomName = ctx.pathParam("room-name")

        val userId = ctx.pathParam("user")


        val (action, values) = ctx.message().split(ACTION_DELIMITER)

        when (action) {
            "DECK_SUBMIT" -> {
                RoomClient.submitDecks(roomName, userId, values.split(DECK_CODE_DELIMITER))
            }
            "BAN_SUBMIT" -> {
                RoomClient.submitBans(roomName, userId, values.split(DECK_CODE_DELIMITER))
            }
            "PING" -> {
                ctx.send("PONG")
            }
            else -> {
                ctx.send("Unknown Request")
                println("Unknown Request: ${ctx.message()}")
            }
        }
    }

    fun onClose(ctx: WsCloseContext) {
        val roomName = ctx.pathParam("room-name")

        val userId = ctx.pathParam("user")

        try {
            RoomClient.disconnect(roomName, userId)
        } catch (e: Exception) {

        }
    }

}