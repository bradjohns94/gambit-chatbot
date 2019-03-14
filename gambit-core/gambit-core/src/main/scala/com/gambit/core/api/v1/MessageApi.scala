package com.gambit.core.api.v1

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

import cats.effect.IO
import com.typesafe.scalalogging.Logger
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.finch._
import io.finch.catsEffect._
import io.finch.circe._

import com.gambit.core.api.GambitEndpoint
import com.gambit.core.bot.engines.MessageEngine
import com.gambit.core.common.{ClientMessage, ClientMessageResponse}

/** Message API
 *  Common types and functions needed to recieve message inputs from client
 *  services and return async responses from the bot engine
 */
class MessageApi(engine: MessageEngine) extends GambitEndpoint[ClientMessageResponse] {
  val logger = Logger("MessageApi")

  val endpoints = postMessage

  implicit val decoder: Decoder[ClientMessage] = deriveDecoder[ClientMessage]
  implicit val encoder: Encoder[ClientMessage] = deriveEncoder[ClientMessage]

  /** Post Message
   *  The endpoint surrounding postMessageAction to be served with finch
   *  @param message the message object derived from the body of the post params
   *  @return a finch output wrapped API Message Response object
   */
  def postMessage: Endpoint[IO, ClientMessageResponse] =
    post("message" :: jsonBody[ClientMessage]) { postMessageAction _ }

  /** Post Message Action
   *  The action to perform when the API receives a POST to the /message endpoint,
   *  Take in a message, parse the JSON object, and respond with any successfully
   *  matched responses from the message engine
   *  @param message a client message object received as JSON
   *  @return a finch output wrapped API Message Response object
   */
  def postMessageAction(message: ClientMessage): Output[ClientMessageResponse] = {
    val futureResponse = engine.parseMessage(message)

    Try(Await.result(futureResponse, 1 minute)) match {
      case Success(result) => result.messages.isEmpty match {
        case true => NoContent
        case false => Ok(result)
      }
      case Failure(error) => {
        logger.warn(s"Failed to parse message response: ${error}")
        InternalServerError(throw(error))
      }
    }
  }
}
