package com.gambit.slackclient.handlers

import scala.util.{Failure, Success}

import com.twitter.finagle.http.Response
import com.twitter.util.Future
import com.typesafe.scalalogging.Logger
import featherbed.circe._
import io.circe.generic.auto._
import io.circe.parser.decode
import slack.models.{Message, User}
import slack.rtm.SlackRtmClient

import com.gambit.slackclient.slackapi.SlackMessage

/** Core Request
 *  The expected input to the gambit core API
 *  @param userId the identifier of the user who sent the message
 *  @param username the username who sent the initial message
 *  @param messageText the actual string of text sent in the message
 *  @param client a string identifying the requesting client
 */
case class CoreRequest(
  userId: String,
  username: String,
  messageText: String,
  client: String
)

/** Core Message
 *  A datatype returned from the core API corresponding to an individual
 *  message to be send back to slack
 *  @param messageText the text of the response message
 */
case class CoreMessage(
  messageText: String
)

/** Core Response
 *  The expected response from the gambit Core API
 *  @param messages a list of slack message objects
 */
case class CoreResponse(
  messages: Seq[CoreMessage]
)

/** Message Handler
 *  Types and functions responsible for being the intermediary between slack
 *  messages and the core API
 */
object MessageHandler extends EventHandler[Message] {
  val logger = Logger("MessageHandler")

  /** Process Event
   *  Take in a message from the Slack API, forward it to the core API, and
   *  return a sequence of any number of messages that need to be sent back
   *  to slack
   *  @param client the client to extract any other necessary info out of
   *  @param message the slack API message object received by the client
   *  @return an asynchronous list of slack messages to be sent back
   */
  def processEvent(client: SlackRtmClient, message: Message): Future[Seq[SlackMessage]] = {
    logger.info(s"Sending message: ${message.text} from user ${message.user} to the core API")
    httpClient.post("/message")
              .withContent(translateMessage(message), "application/json")
              .send[Response]()
              .map{ _.contentString }
              .map{ convertResponse(_).map{ translateResponse(message.channel, _) } }
  }

  /** Translate Message
   *  Convert a message into a request that needs to be forwarded into the
   *  core API
   *  @param message the slack API message object retrieved by the client
   *  @return a CoreRequest object that can be sent to the core API
   */
  private def translateMessage(message: Message): CoreRequest =
    CoreRequest(s"${message.user}", s"<@${message.user}>", message.text, "slack")

  /** Translate Response
   *  Convert a response from the core API into a message consumable by slack
   *  @param channel the channel to forward the response to
   *  @param message the response message from the core API
   *  @return a slack-consumable message object to be sent back
   */
  private def translateResponse(channel: String, message: CoreMessage): SlackMessage =
    SlackMessage(channel, message.messageText)

  /** Convert Response
   *  Convert the broader response object from a string buffer retrieved from
   *  featherbed into a sequence of core responses if any were returned
   *  @param response the raw response string returned from featherbed
   *  @return a sequence of CoreMessage objects to be translated into slack responses
   */
  private def convertResponse(response: String): Seq[CoreMessage] =
    decode[CoreResponse](response) match {
      case Left(error) => {
        logger.warn(s"Failed to parse Core API response: ${error}")
        Seq.empty[CoreMessage]
      }
      case Right(resp) => resp.messages
    }
}
