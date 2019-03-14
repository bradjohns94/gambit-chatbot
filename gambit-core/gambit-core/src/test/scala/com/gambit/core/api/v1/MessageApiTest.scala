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

import com.gambit.core.common.{ClientMessage, ClientMessageResponse, CoreResponse}
import com.gambit.core.bot.engines.MessageEngine

class MessageApiTest extends FlatSpec with PrivateMethodTester with MockFactory {
  // Test the Post Message Action
  behavior of "POST /message"

  it should "return a ClientMessage response of engine responses" in {
    val sampleClientMessage = ClientMessage("userId", "username", "channel", "message", "client")
    val sampleResponse = Future(
      ClientMessageResponse(Seq(
        CoreResponse("response 1", "channel"),
        CoreResponse("response 2", "channel")
      ))
    )

    val mockEngine = stub[MessageEngine]
    (mockEngine.parseMessage _) when(sampleClientMessage) returns(sampleResponse)
    val api = new MessageApi(mockEngine)

    api.postMessageAction(sampleClientMessage).status shouldEqual Status.Ok
  }

  it should "return a 204 with no engine responses" in {
    val sampleClientMessage = ClientMessage("userId", "username", "channel", "message", "client")
    val sampleResponse = Future(ClientMessageResponse(Seq.empty[CoreResponse]))

    val mockEngine = stub[MessageEngine]
    (mockEngine.parseMessage _) when(sampleClientMessage) returns(sampleResponse)
    val api = new MessageApi(mockEngine)

    api.postMessageAction(sampleClientMessage).status shouldEqual Status.NoContent
  }
}
