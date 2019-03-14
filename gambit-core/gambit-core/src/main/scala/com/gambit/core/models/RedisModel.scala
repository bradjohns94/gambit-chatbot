package com.gambit.core.models

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.redis.RedisClient
import com.typesafe.scalalogging.Logger

/** Redis Reference
 *  Wrapper class for any operations pertaining to the redis data cache
 *  @param client an already-established redis client
 */
class RedisReference(client: RedisClient) {
  val logger = Logger("Redis Reference")

  /** Increment and update TTL
   *  Increment the given key by the given amount, then update the expiration ttl
   *  @param key the key to increment the value for
   *  @param increment the amount to increment by
   *  @param ttl the new TTL of the key
   *  @return a future with the new value if any
   */
   def incrbyex(key: String, increment: Long, ttl: Int): Option[Long] = {
     logger.info(s"Incrementing cache key ${key} by ${increment}")
     client.incrby(key, increment).map{ ret =>
       client.expire(key, ttl) match {
         case true => logger.info(s"Updated TTL for key ${key} to ${ttl}")
         case false => logger.warn(s"Failed to update TTL for key ${key}")
       }
       ret
     }
   }
}
