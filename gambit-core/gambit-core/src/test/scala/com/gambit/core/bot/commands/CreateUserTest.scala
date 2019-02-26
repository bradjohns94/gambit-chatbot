package com.gambit.core.bot.commands

import scala.concurrent.Future
import scala.util.Try

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers._
import slick.jdbc.PostgresProfile.api.Database

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.GambitUsersReference

class CreateUserTest extends AsyncFlatSpec with AsyncMockFactory {
  behavior of "runCommand"

  it should "eventually return a response after successfuly creating a user" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "test create user nick",
      "client"
    )
    val mockTable = stub[GambitUsersReference]
    (mockTable.createGambitUser _) when("nick") returns(Future(Try("nick")))
    val command = new CreateUser(mockTable)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Successfully created user: nick"
    }
  }

  it should "eventually return a response despite failing to create a user" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "test create user nick",
      "client"
    )
    val mockTable = stub[GambitUsersReference]
    (mockTable.createGambitUser _) when("nick") returns(Future(Try(
      throw new Exception("failure")
    )))
    val command = new CreateUser(mockTable)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Failed to create user nick"
    }
  }

  it should "never return a response if parsing fails" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "not the right command",
      "client"
    )
    val mockTable = stub[GambitUsersReference]
    val command = new CreateUser(mockTable)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe None
    }
  }
}
