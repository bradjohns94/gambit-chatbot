package com.gambit.core.api.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.twitter.finagle.http.Status
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.scalatest.Matchers._

import com.gambit.core.common.Types.{CoreMessage, CoreResponse}

class MessageApiTest extends FlatSpec with PrivateMethodTester {
  // Test Post Message
  behavior of "POST /message"

  it should "return a ClientMessage response of engine responses" in {
    val samplePost = Input.post("/message").withBody[Application.Json](
      ClientMessage("user", "hi test")
    )
    val actual = MessageApi.postMessage(samplePost).awaitOutputUnsafe().map(_.status)
    actual shouldEqual Some(Status.Ok)
  }

  it should "return a 204 with no engine responses" in {
    val samplePost = Input.post("/message").withBody[Application.Json](
      ClientMessage("user", "")
    )
    val actual = MessageApi.postMessage(samplePost).awaitOutputUnsafe().map(_.status)
    actual shouldEqual Some(Status.NoContent)
  }

  // Test Translate Message
  "translateMessage" should "convert an equivalent CoreMessage" in {
    val translateMessage = PrivateMethod[CoreMessage]('translateMessage)
    val sampleMessage = ClientMessage("testUser", "test Message")
    val actual = MessageApi invokePrivate translateMessage(sampleMessage)
    val expected = CoreMessage("testUser", "test Message")
    actual shouldEqual expected
  }

  // Test Translate Response
  "translateResponse" should "create a single ClientMessageResponse" in {
    val translateResponse =
      PrivateMethod[ClientMessageResponse]('translateResponse)

    val sampleResponse = Seq(
      CoreResponse("test response 1"),
      CoreResponse("test response 2")
    )
    val expected = ClientMessageResponse(Seq(
      CoreResponse("test response 1"),
      CoreResponse("test response 2")
    ))
    val actual = MessageApi invokePrivate translateResponse(sampleResponse)
    actual shouldEqual expected
  }
}
