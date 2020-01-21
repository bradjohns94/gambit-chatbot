package com.gambit.user

import cats.effect.IO
import io.finch.catsEffect._
import io.circe.generic.auto._

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Await
import com.typesafe.scalalogging.Logger
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import org.postgresql.ds.PGSimpleDataSource
import slick.jdbc.PostgresProfile.api._

import com.gambit.user.api.v1._
import com.gambit.user.models._

/** Main
 *  Register all endpoints to a service definition and start the finch API
 *  ignoring warts here because this file was created by finch
 */
object Main extends App {
  val logger = Logger("Main")
  logger.info("Starting gambit user API service...")

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
  val gambitUserTable = new GambitUserReference(db)
  val slackUserTable = new SlackUserReference(db)

  // Initialize the API Endpoints
  val gambitUserApi = new GambitUserApi(gambitUserTable)
  val slackUserApi = new SlackUserApi(slackUserTable)

  def service: Service[Request,Response] = (
    gambitUserApi.endpoints :+:
    slackUserApi.endpoints
  ).toServiceAs[Application.Json]

  Await.ready(Http.server.serve(s":8080", service))
  logger.info("Stopping gambit user API service...")
}
