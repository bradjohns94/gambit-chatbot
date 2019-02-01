package com.gambit.slackclient.slackapi

import com.typesafe.scalalogging.Logger
import slack.rtm.SlackRtmClient

case class SlackMessage(channel: String, messageText: String)

/** Slack Messager
 *  Object to foward messages into slack
 */
object SlackMessager {
  val logger = Logger("SlackMessager")
  /** Send
   *  Send a message to slack via a given RTM Client
   *  @param client the client object to send the message from
   *  @param message the slack message and channel to be sent
   */
  def send(client: SlackRtmClient, message: SlackMessage): Unit = {
    logger.info(s"Sending message: ${message.messageText}")
    client.sendMessage(message.channel, message.messageText)
  }
}
