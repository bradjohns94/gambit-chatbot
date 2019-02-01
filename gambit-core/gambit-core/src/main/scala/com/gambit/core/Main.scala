package com.gambit.core

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

import com.gambit.core.api.v1.MessageApi

/** Main
 *  Register all endpoints to a service definition and start the finch API
 *  ignoring warts here because this file was created by finch
 */
object Main extends App {
  val logger = Logger("Main")
  logger.info("Starting gambit core service...")

  def service: Service[Request,Response] = Bootstrap
    .serve[Application.Json](MessageApi.postMessage)
    .toService

  Await.ready(Http.server.serve(s":8080", service))
  logger.info("Stopping gambit core service...")
}
