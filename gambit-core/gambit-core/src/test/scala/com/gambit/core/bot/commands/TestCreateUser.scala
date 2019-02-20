package com.gambit.core.bot.commands

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.scalatest.Matchers._
import slick.jdbc.PostgresProfile.api.Database

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.GambitUsersReference

class CreateUserTest extends FlatSpec with PrivateMethodTester with MockFactory {
  behavior of "runCommand"

  it should "return a response after successfuly creating a user" in {
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

  it should "return a response despite failing to create a user" in {
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
      result.get.messageText shouldEqual "Failed to create user: nick"
    }
  }

  it should "not return a response if parsing fails" in {
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
