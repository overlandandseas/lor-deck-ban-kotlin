package dao

import model.User

object UserDAO {

    private val userMap = mutableMapOf<String, User>()

    fun findById(id: String): User? {
        return userMap[id]
    }

    fun create(user: User) {
        userMap[user.id] = user
    }

    fun remove(user: User) {
        userMap.remove(user.id)
    }

    fun removeById(id: String) {
        userMap.remove(id)
    }
}