package model

import io.javalin.websocket.WsContext

data class Player(var decks: List<String>, var bans: List<String>, var ctx: WsContext, var connected: Boolean)