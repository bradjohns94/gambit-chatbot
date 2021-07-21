package com.gambit.core

import cats.effect.IO
import io.finch.catsEffect._
import io.circe.generic.auto._

import com.redis.RedisClient
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Await
import com.typesafe.scalalogging.Logger
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory
import slick.jdbc.PostgresProfile.api._

import com.gambit.core.api.v1.MessageApi
import com.gambit.core.bot.engines.{MessageEngine, MessageEngineConfig}

/** Main
 *  Register all endpoints to a service definition and start the finch API
 *  ignoring warts here because this file was created by finch
 */
object Main extends App {
  // System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "Info");
  val logger = Logger(LoggerFactory.getLogger(Main.getClass))
  logger.info("Starting gambit core service...")

  /** Get Database From Environment
   *  Setup a connection to the gambit postgres database from a set of variables
   *  stored in the system environment.
   *  @return a slick database connection object
   */
  val getDatabaseFromEnvironment: Database = {
    val dataSource = new PGSimpleDataSource()
    dataSource.setServerName(sys.env("PG_URL"))
    dataSource.setUser(sys.env("PG_USER"))
    dataSource.setPassword(sys.env("PG_PASSWORD"))
    dataSource.setDatabaseName(sys.env("PG_DB"))
    Database.forDataSource(dataSource, None)
  }

  // Initialize the database
  val db: Database = getDatabaseFromEnvironment

  // Initialize the redis cache
  val redis: RedisClient = new RedisClient(
    sys.env("REDIS_HOST"),
    sys.env("REDIS_PORT").toInt
  )

  // Initialize the engine configs
  val messageConfig = new MessageEngineConfig(db, redis)
  val messageEngine = new MessageEngine(messageConfig)

  // Initialize the API Endpoints
  val messageApi = new MessageApi(messageEngine)

  def service: Service[Request,Response] = Bootstrap
    .serve[Application.Json](messageApi.postMessage)
    .toService

  Await.ready(Http.server.serve(s":8080", service))
  logger.info("Stopping gambit core service...")
}
