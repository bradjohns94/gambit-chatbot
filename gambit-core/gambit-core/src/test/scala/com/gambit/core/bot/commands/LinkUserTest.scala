package com.gambit.core.bot.commands

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers._
import slick.jdbc.PostgresProfile.api.Database

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.clients.{GambitUser, GambitUserClient, User, UserClient}

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

    val mockGambitUserClient = stub[GambitUserClient]
    val mockUserClient = stub[UserClient]
    (mockGambitUserClient.getGambitUserByNickname _) when("nick") returns(Future(Some(GambitUser(
      1, "nick", false, "", None, None))))
    (mockUserClient.setGambitUser(_, _)) when("fakeId", 1) returns(Future(Some(User("1", None))))
    val mockMapping = Map("client" -> mockUserClient)

    val command = new LinkUser(mockGambitUserClient, mockMapping)
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

    val mockGambitUserClient = stub[GambitUserClient]
    val mockUserClient = stub[UserClient]
    (mockGambitUserClient.getGambitUserByNickname _) when("nick") returns(Future(Some(GambitUser(
      1, "nick", false, "", None, None))))
    (mockUserClient.setGambitUser(_, _)) when("fakeId", 1) returns(Future(None))
    val mockMapping = Map("client" -> mockUserClient)

    val command = new LinkUser(mockGambitUserClient, mockMapping)
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

    val mockGambitUserClient = stub[GambitUserClient]
    val mockUserClient = stub[UserClient]
    val mockMapping = Map("notClient" -> mockUserClient)

    val command = new LinkUser(mockGambitUserClient, mockMapping)
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

    val mockGambitUserClient = stub[GambitUserClient]
    val mockUserClient = stub[UserClient]
    val mockMapping = Map("notClient" -> mockUserClient)

    val command = new LinkUser(mockGambitUserClient, mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe None
    }
  }
}
