package com.gambit.slackclient.handlers

import java.net.URL

import com.twitter.util.Future
import featherbed.Client
import slack.models.SlackEvent
import slack.rtm.SlackRtmClient

import com.gambit.slackclient.slackapi.SlackMessage

/** Event Handler
 *  Abstract handler trait to be inherited by anything that needs to act as
 *  a handler for a slack event type
 */
trait EventHandler[A <: SlackEvent] {
  private val coreURL = sys.env("GAMBIT_CORE_URL")
  private val corePort = sys.env("GAMBIT_CORE_PORT").toInt
  val httpClient = new Client(new URL(s"${coreURL}:${corePort}"))

  /** Process Event
   *  Spin up an asynchronous thread to handle the incoming event and forward
   *  any potential responses to slack
   *  @param client the client to extract any other necessary info out of
   *  @param event the event triggered from slack
   *  @return a future of any slack messages that need to be responded to
   */
  def processEvent(client: SlackRtmClient, event: A): Future[Seq[SlackMessage]]
}
