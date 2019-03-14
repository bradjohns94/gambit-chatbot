package com.gambit.core.bot.commands

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers._
import slick.jdbc.PostgresProfile.api.Database

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.ClientReference

class LinkUserTest extends AsyncFlatSpec with AsyncMockFactory {
  behavior of "runCommand"

  it should "eventually return a response after successfully linking a user" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "test link fakeId to nick",
      "client",
      None
    )

    val mockSave = Future(Success(1))
    val mockReference = stub[ClientReference]
    (mockReference.setGambitUserFromNickname(_, _)) when("fakeId", "nick") returns(mockSave)
    val mockMapping = Map("client" -> mockReference)

    val command = new LinkUser(mockMapping)
    val actual = command.runCommand(sampleMessage)
    actual.map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Successfully linked client ID fakeId to nick"
    }
  }

  it should "eventually return a response when failing to create a user" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "test link fakeId to nick",
      "client",
      None
    )

    val mockSave = Future(Failure(new Exception("boom")))
    val mockReference = stub[ClientReference]
    (mockReference.setGambitUserFromNickname(_, _)) when("fakeId", "nick") returns(mockSave)
    val mockMapping = Map("client" -> mockReference)

    val command = new LinkUser(mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Failed to link client ID fakeId to nick"
    }
  }

  it should "eventually return a response if the client is not found" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "test link fakeId to nick",
      "client",
      None
    )

    val mockSave = Future(Failure(new Exception("boom")))
    val mockReference = stub[ClientReference]
    val mockMapping = Map("notClient" -> mockReference)

    val command = new LinkUser(mockMapping)
    val expectedResponse = "test client client does not currently support linking users"
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual expectedResponse
    }
  }

  it should "never return a response if parsing fails" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "Invalid message",
      "client",
      None
    )

    val mockReference = stub[ClientReference]
    val mockMapping = Map("notClient" -> mockReference)

    val command = new LinkUser(mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe None
    }
  }
}
