package com.gambit.core.bot.commands

import scala.concurrent.Future
import scala.util.Try

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers._
import slick.jdbc.PostgresProfile.api.Database

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.clients.{GambitUser, GambitUserClient}

class CreateUserTest extends AsyncFlatSpec with AsyncMockFactory {
  behavior of "runCommand"

  it should "eventually return a response after successfuly creating a user" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "test create user nick",
      "client",
      None
    )
    val mockClient = stub[GambitUserClient]
    (mockClient.createGambitUser _) when("nick") returns(Future(Some(
      GambitUser(1, "user", false, "", None, None))))
    val command = new CreateUser(mockClient)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Successfully created user ID 1"
    }
  }

  it should "eventually return a response despite failing to create a user" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "test create user nick",
      "client",
      None
    )
    val mockClient = stub[GambitUserClient]
    (mockClient.createGambitUser _) when("nick") returns(Future(None))
    val command = new CreateUser(mockClient)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Failed to create user nick"
    }
  }

  it should "never return a response if parsing fails" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "not the right command",
      "client",
      None
    )
    val mockClient = stub[GambitUserClient]
    val command = new CreateUser(mockClient)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe None
    }
  }
}
