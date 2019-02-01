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
import com.gambit.core.common.Types.{CoreMessage, CoreResponse}

/** ClientMessage
 *  The expected input type for the message API to come from a client
 *  @param username the username who sent the initial message
 *  @param messageText the actual string of text sent in the message
 */
final case class ClientMessage(
  username: String,
  messageText: String
)

final case class ClientMessageResponse(messages: Seq[CoreResponse])

/** Message API
 *  Common types and functions needed to recieve message inputs from client
 *  services and return async responses from the bot engine
 */
object MessageApi extends GambitEndpoint[ClientMessageResponse] {
  val logger = Logger("MessageApi")

  val endpoints = postMessage

  implicit val decoder: Decoder[ClientMessage] = deriveDecoder[ClientMessage]
  implicit val encoder: Encoder[ClientMessage] = deriveEncoder[ClientMessage]


  /** Post Message
   *  The endpoint to retrieve a message object from a client service. Takes
   *  in a message object as JSON, parses it, and responds with any number of
   *  responses to be run by the requesting API
   *  @param message the message object derived from the body of the post params
   *  @return a finch output wrapped API Message Response object
   */
  def postMessage: Endpoint[IO, ClientMessageResponse] =
    post("message" :: jsonBody[ClientMessage]) { message: ClientMessage =>
      val coreMessage = translateMessage(message)
      val futureResponse = MessageEngine.parseMessage(coreMessage)
                                        .flatMap{ response =>
                                          Future(translateResponse(response)) }
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

  /** Translate Client Message
   *  Convert a client message into a core message consumed by the message
   *  engine.
   *  @param message a client message recieved from an API client
   *  @return a core message derived from the client message
   */
  private def translateMessage(message: ClientMessage): CoreMessage =
    CoreMessage(message.username, message.messageText)

  /** Translate Core Response
   *  Given a core message passed back from the message engine, convert it into
   *  a ClientMessageResponse returnable from the API
   *  @param response a CoreResponse to translate
   *  @return a ClientMessageResponse derived from the CoreResponse
   */
  private def translateResponse(responses: Seq[CoreResponse]): ClientMessageResponse =
    ClientMessageResponse(responses)
}
