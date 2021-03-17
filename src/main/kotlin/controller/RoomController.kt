package controller

import client.RoomClient
import io.javalin.http.Context
import model.RoomRequest
import java.util.*

object RoomController {

    fun createRoom(ctx: Context) {
        val roomName = RoomClient.getUniqueRoomName()
        val roomRequest = ctx.body<RoomRequest>()

        val user = roomRequest.user ?: UUID.randomUUID().toString()

        data class Response(val user: String, val room: String)

        RoomClient.create(
            name = roomName,
            numberOfDecks = roomRequest.numberOfDecks,
            numberOfBans = roomRequest.numberOfBans
        )

        ctx.json(Response(user = user, room = roomName))
    }
}