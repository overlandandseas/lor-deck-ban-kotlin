import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import redis.clients.jedis.Jedis
import java.util.concurrent.ConcurrentHashMap


private val playerSessionMap = ConcurrentHashMap<String, ArrayList<WsContext>>()
private val redis = Jedis("localhost");

object PlayerController {

    fun onConnect(ctx: WsConnectContext) {
        val roomId = ctx.pathParam("roomId")
        if (playerSessionMap.containsKey(roomId))
            playerSessionMap[roomId]?.add(ctx);
        else
            playerSessionMap[roomId] = arrayListOf(ctx)

        val connected = redis.hincrBy(roomId, "connected", 1)
        sendAll(roomId, "Connected: $connected")
    }

    fun onMessage(ctx: WsMessageContext) {
        val splitMsg = ctx.message().split(":")
        splitMsg[0]
    }

    fun onClose(ctx: WsCloseContext) {

    }
}


private fun sendAll(roomId: String, message: String) {
    playerSessionMap[roomId]?.filter{ it.session.isOpen }?.forEach{ it.send(message)}
}
