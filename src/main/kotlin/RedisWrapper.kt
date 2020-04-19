import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI

private const val DEFAULT_REDIS_URL = "http://localhost:6379"

object Redis {

    private fun  getPool(): JedisPool {
        val redisURI = URI(System.getenv("REDIS_URL") ?: DEFAULT_REDIS_URL)
        val poolConfig = JedisPoolConfig()
        poolConfig.maxTotal = 10;
        poolConfig.maxIdle = 5;
        poolConfig.minIdle = 1;
        poolConfig.testOnBorrow = true;
        poolConfig.testOnReturn = true;
        poolConfig.testWhileIdle = true;

        return JedisPool(poolConfig, redisURI)
    }

    val instance: Jedis by lazy {
        getPool().resource
    }
}