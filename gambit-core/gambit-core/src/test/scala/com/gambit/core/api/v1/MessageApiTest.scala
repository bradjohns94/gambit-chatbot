package com.gambit.core.api.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.twitter.finagle.http.Status
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.scalatest.Matchers._

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.bot.engines.MessageEngine

class MessageApiTest extends FlatSpec with PrivateMethodTester with MockFactory {
  // Test the Post Message Action
  behavior of "POST /message"

  it should "return a ClientMessage response of engine responses" in {
    val sampleClientMessage = ClientMessage("userId", "username", "message", "client")
    val sampleCoreMessage = CoreMessage("userId", "username", "message", "client")
    val sampleResponse = Future(Seq(
      CoreResponse("response 1"),
      CoreResponse("response 2")
    ))

    val mockEngine = stub[MessageEngine]
    (mockEngine.parseMessage _) when(sampleCoreMessage) returns(sampleResponse)
    val api = new MessageApi(mockEngine)

    api.postMessageAction(sampleClientMessage).status shouldEqual Status.Ok
  }

  it should "return a 204 with no engine responses" in {
    val sampleClientMessage = ClientMessage("userId", "username", "message", "client")
    val sampleCoreMessage = CoreMessage("userId", "username", "message", "client")
    val sampleResponse = Future(Seq.empty[CoreResponse])

    val mockEngine = stub[MessageEngine]
    (mockEngine.parseMessage _) when(sampleCoreMessage) returns(sampleResponse)
    val api = new MessageApi(mockEngine)

    api.postMessageAction(sampleClientMessage).status shouldEqual Status.NoContent
  }

  // Test Translate Message
  "translateMessage" should "convert an equivalent CoreMessage" in {
    val translateMessage = PrivateMethod[CoreMessage]('translateMessage)
    val sampleMessage = ClientMessage("userId", "testUser", "test Message", "client")

    val mockEngine = stub[MessageEngine]
    val api = new MessageApi(mockEngine)

    val expected = CoreMessage("userId", "testUser", "test Message", "client")
    (api invokePrivate translateMessage(sampleMessage)) shouldEqual expected
  }

  // Test Translate Response
  "translateResponse" should "create a single ClientMessageResponse" in {
    val translateResponse =
      PrivateMethod[ClientMessageResponse]('translateResponse)

    val sampleResponse = Seq(
      CoreResponse("test response 1"),
      CoreResponse("test response 2")
    )

    val mockEngine = stub[MessageEngine]
    val api = new MessageApi(mockEngine)

    val expected = ClientMessageResponse(Seq(
      CoreResponse("test response 1"),
      CoreResponse("test response 2")
    ))
    (api invokePrivate translateResponse(sampleResponse)) shouldEqual expected
  }
}
