package client

import dao.UserDAO
import model.User

object UserClient {
    fun getPreviouslySubmittedDecks(id: String): List<String> {
        val user = UserDAO.findById(id)

        return user?.previouslySubmittedDecks ?: listOf()
    }

    fun setSubmittedDecks(id: String, decks: List<String>) {
        val user = User(id, decks)
        UserDAO.create(user)
    }

    fun removeUser(id: String) {
        UserDAO.removeById(id)
    }

    fun findById(id: String): User {
        return UserDAO.findById(id) ?: User(id, listOf())
    }
}