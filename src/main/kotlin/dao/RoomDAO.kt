package dao

import model.Player
import model.Room
import model.User

object RoomDAO {
    private val roomMap = mutableMapOf<String, Room>()

    fun findByName(name: String): Room? {
        return roomMap[name]
    }

    fun create(room: Room) {
        roomMap[room.name] = room
    }

    fun remove(room: Room) {
        roomMap.remove(room.name)
    }
}