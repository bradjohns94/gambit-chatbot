package com.gambit.slackclient.handlers

import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.scalatest.Matchers._
import slack.models.Message

import com.gambit.slackclient.slackapi.SlackMessage

class MessageHandlerTest extends FlatSpec with PrivateMethodTester {
  // Tests for translateMessage
  "translateMessage" should "create a CoreRequest" in {
    val translateMessage = PrivateMethod[CoreRequest]('translateMessage)
    val sampleMessage = Message(null, null, "user", "message", None, None)
    val actual = MessageHandler invokePrivate translateMessage(sampleMessage)
    actual shouldEqual CoreRequest("user", "<@user>", "message", "slack")
  }

  // Tests for translateResponse
  "translateResponse" should "create a SlackMessage" in {
    val translateResponse = PrivateMethod[SlackMessage]('translateResponse)
    val sampleMessage = CoreMessage("message")
    val actual = MessageHandler invokePrivate translateResponse("channel", sampleMessage)
    actual shouldEqual SlackMessage("channel", "message")
  }

  // Tests for convertResponse
  behavior of "convertResponse"

  it should("return the parsed messages on success") in {
    val convertResponse = PrivateMethod[Seq[CoreMessage]]('convertResponse)
    val sampleResponse = "{\"messages\": [{\"messageText\": \"some\"}, " +
                         "{\"messageText\": \"messages\"}]}"
    val actual = MessageHandler invokePrivate convertResponse(sampleResponse)
    actual shouldEqual Seq(
      CoreMessage("some"),
      CoreMessage("messages")
    )
  }

  it should "create an empty sequence of CoreMessages on error" in {
    val convertResponse = PrivateMethod[Seq[CoreMessage]]('convertResponse)
    MessageHandler invokePrivate convertResponse("invalid") shouldEqual Seq.empty[CoreMessage]
  }
}
