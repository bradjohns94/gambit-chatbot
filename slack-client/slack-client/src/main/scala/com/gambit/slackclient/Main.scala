package com.gambit.slackclient

import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import com.typesafe.scalalogging.Logger
import slack.models.Message
import slack.rtm.SlackRtmClient

import com.gambit.slackclient.slackapi.SlackMessager
import com.gambit.slackclient.handlers._

/** Main
 *  Main application object for the gambit slack client
 */
object Main {
  val logger = Logger("Main")

  /** Main
   *  Main function of the application, setup a slack client and manage
   *  any event handlers found in the handlers directory
   *  @param args CLI args (not used)
   */
  def main(args: Array[String]): Unit = {
    logger.info("Starting Gambit Slack Client...")
    val client = setupClient
    client.onEvent {
      case message: Message => MessageHandler.processEvent(message)
        .onSuccess{ responses => responses.foreach{ SlackMessager.send(client, _) } }
        .onFailure{ error => logger.warn(s"Failed to send response ${error}") }
      case _ => logger.info("Recieved unsupported event type")
    }
    logger.info("Stopping Gambit Slack Client...")
  }

  /** Setup Client
   *  Create a client to establish a connection with slack
   *  @return a Slack Realtime Messaging client derived from the environment=
   */
  private def setupClient: SlackRtmClient = {
    implicit val system = ActorSystem("slack")
    val token = sys.env("SLACK_API_TOKEN")
    SlackRtmClient(token)
  }
}
