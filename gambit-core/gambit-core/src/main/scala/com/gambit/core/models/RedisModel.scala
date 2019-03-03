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

  /** Get
   *  Async wrapper around the redis client's get operation
   *  @param key the key to fetch from the database
   *  @return a future string value at the given key
   */
  def get(key: String): Future[Option[String]] = Future(client.get(key))

  /** Set with Expiration
   *  Async wrapper around the redis client's setex operation
   *  @param key the key to set the value for
   *  @param expiry the number of seconds until expiration
   *  @param value the value to set with the key
   *  @return a future containing whether or not the set was successful I think?
   */
  def setex(key: String, expiry: Long, value: String): Future[Boolean] = Future(
    client.setex(key, expiry, value)
  )

  /** Expire
   *  Async wrapper around the redis client's expire operation
   *  @param key the key to set the expiry on
   *  @param ttl the time left to live on the key in seconds
   *  @return a future containing whether or not the operation was successful?
   */
  def expire(key: String, ttl: Int): Future[Boolean] = Future(client.expire(key, ttl))

  /** Increment By
   *  Async wrapper around the redis client's incrby operation
   *  @param key the key to increment the value for
   *  @param increment the amount to increment by
   *  @return a future with the new value if any
   */
  def incrby(key: String, increment: Long): Future[Option[Long]] = Future(
    client.incrby(key, increment)
  )

  /** Increment and update TTL
   *  Increment the given key by the given amount, then update the expiration ttl
   *  @param key the key to increment the value for
   *  @param increment the amount to increment by
   *  @param ttl the new TTL of the key
   *  @return a future with the new value if any
   */
   def incrbyex(key: String, increment: Long, ttl: Int): Future[Option[Long]] = {
     incrby(key, increment).map{ ret =>
       expire(key, ttl).map{ expireRes =>
         expireRes match {
           case true => logger.info(s"Updated TTL for key ${key} to ${ttl}")
           case false => logger.warn(s"Failed to update TTL for key ${key}")
         }
       }
       ret
     }
   }
}
