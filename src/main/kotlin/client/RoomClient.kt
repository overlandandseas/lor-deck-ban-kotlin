package client

import PlayerAlreadySubmittedDecksException
import PlayerNotFoundException
import RoomAlreadyExistsException
import RoomIsFullException
import RoomNotFoundException
import dao.RoomDAO
import io.javalin.plugin.json.JavalinJson
import io.javalin.websocket.WsContext

import model.Player
import model.PlayerRoomState

import model.Room
import java.util.*
import kotlin.concurrent.schedule


object RoomClient {

    private const val MAX_ROOM_SIZE = 2

    private const val ONE_MINUTE_IN_MILLIS = 60000L

    private val timerMap = mutableMapOf<String, TimerTask>()

    fun getUniqueRoomName(): String {
        fun randomChars() = (0 until 5).map { (65 + Math.random() * 26).toChar() }.joinToString("")

        var roomName = randomChars()
        while (RoomDAO.findByName(roomName) != null) {
            roomName = randomChars()
        }
        return roomName
    }

    fun create(name: String, numberOfDecks: Int, numberOfBans: Int) {

        if (RoomDAO.findByName(name) != null) {
            throw RoomAlreadyExistsException("Room $name already exists")
        }
        val players = mutableMapOf<String, Player>()
        val room = Room(name = name, numberOfDecks = numberOfDecks, numberOfBans = numberOfBans, players = players)
        RoomDAO.create(room)

        updateRoomStateToPlayers(room)
    }


    fun connect(name: String, user: String, ctx: WsContext) {

//        val room = RoomDAO.findByName(name) ?: {
//            val players = mutableMapOf(Pair(user, Player(listOf(), listOf(), ctx, true)))
//            val room = Room(name, players, 3, 1)
//            RoomDAO.create(room)
//            room
//        }()

        val room = RoomDAO.findByName(name) ?: throw RoomNotFoundException("Room $name is not found")

        if (room.players.size >= MAX_ROOM_SIZE) {
            val existingPlayer = room.players[user] ?: throw RoomIsFullException("Room $name is full")
            existingPlayer.ctx = ctx
            existingPlayer.connected = true
        } else {
            val player = Player(decks = mutableListOf(), bans = mutableListOf(), ctx = ctx, connected = true)
            room.players[user] = player
        }
        updateRoomStateToPlayers(room)
    }

    fun disconnect(name: String, user: String) {
        val room = RoomDAO.findByName(name) ?: throw RoomNotFoundException("Room $name not found")

        val player = room.players[user] ?: throw PlayerNotFoundException("Player not found in room $name")

        player.connected = false


        if (room.players.values.none { it.connected }) {

            timerMap[room.name]?.cancel()

            timerMap[room.name] = Timer("DeleteRoom${room.name}", false).schedule(ONE_MINUTE_IN_MILLIS) {
                if (room.players.values.none { it.connected }) {
                    RoomDAO.remove(room)
                }
            }

        } else {
            updateRoomStateToPlayers(room)
        }

    }

    fun submitDecks(name: String, user: String, decks: List<String>) {
        val room = RoomDAO.findByName(name) ?: throw RoomNotFoundException("Room $name not found")

        val player = room.players[user] ?: throw PlayerNotFoundException("Player not found in room $name")
        if (player.decks.isNotEmpty()) throw PlayerAlreadySubmittedDecksException("Player already submitted decks")

        player.decks = decks

//        UserClient.setSubmittedDecks(user, decks)

        updateRoomStateToPlayers(room)
    }

    fun submitBans(name: String, user: String, bans: List<String>) {
        val room = RoomDAO.findByName(name) ?: throw RoomNotFoundException("Room $name not found")

        val player = room.players[user] ?: throw PlayerNotFoundException("Player not found in room $name")
        if (player.decks.isNotEmpty()) throw PlayerAlreadySubmittedDecksException("Player already submitted bans")

        player.bans = bans

        updateRoomStateToPlayers(room)
    }

    private fun sendAll(room: Room, message: String) {
        room.players.values.forEach { it.ctx.send(message) }
    }

    private fun sendPlayer(player: Player, message: String) {
        if (player.ctx.session.isOpen) player.ctx.send(message)
    }

    private fun updateRoomStateToPlayers(room: Room) {
        room.players.forEach { (userId, player) ->
            val yourDecks = player.decks


            // Check if all players submitted decks
            val otherDecks =
                if (room.players.values.all { it.decks.isNotEmpty() })
                    room.players.filter { it.key != userId }.values.map { it.decks }
                else
                    if (yourDecks.isEmpty())
                        if (room.players.filter { it.key != userId }.values.all { it.decks.isNotEmpty() })
                            listOf<List<String>>()
                        else
                            null
                    else
                        null


            val yourBans = player.bans

            // Check if all players submitted bans
            val otherBans =
                if (room.players.values.all { it.bans.isNotEmpty() })
                    room.players.filter { it.key != userId }.values.map { it.bans }
                else
                    if (yourBans.isEmpty())
                        if (room.players.filter { it.key != userId }.values.all { it.bans.isNotEmpty() })
                            listOf<List<String>>()
                        else
                            null
                    else
                        null

            val playerRoomState = PlayerRoomState(
                yourDecks = yourDecks,
                yourBans = yourBans,
                otherDecks = otherDecks,
                otherBans = otherBans,
                connected = room.players.values.count { it.connected },
                numberOfDecks = room.numberOfDecks,
                numberOfBans = room.numberOfBans
            )


            sendPlayer(player, JavalinJson.toJson(playerRoomState))
        }
    }
}

