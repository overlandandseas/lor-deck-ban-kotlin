import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI

private const val DEFAULT_REDIS_URL = "http://localhost:6379"

val redisURI = URI(System.getenv("REDIS_URL") ?: DEFAULT_REDIS_URL)


fun  getPool(): JedisPool {

    val poolConfig = JedisPoolConfig()
    poolConfig.maxTotal = 10;
    poolConfig.maxIdle = 5;
    poolConfig.minIdle = 1;
    poolConfig.testOnBorrow = true;
    poolConfig.testOnReturn = true;
    poolConfig.testWhileIdle = true;

    return JedisPool(poolConfig, redisURI)
}
object Redis {
    val jedisPool = getPool()
}