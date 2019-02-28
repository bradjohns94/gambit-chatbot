package com.gambit.core.bot.commands

import scala.concurrent.Future

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers._
import slick.jdbc.PostgresProfile.api.Database

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.ClientReference

class RegisterAllUsersTest extends AsyncFlatSpec with AsyncMockFactory {
  behavior of "runCommand"

  it should "eventually return a response after successfully registering some users" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "test: register all unlinked users",
      "client"
    )
    val mockReference = stub[ClientReference]
    (mockReference.registerUnlinkedUsers _: () => Future[Int]) when() returns(Future(2))
    val mockMapping = Map("client" -> mockReference)

    val command = new RegisterAllUsers(mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Successfully registered 2 users"
    }
  }

  it should "eventually return a response after failing to register any users" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "test: register all unlinked users",
      "client"
    )
    val mockReference = stub[ClientReference]
    (mockReference.registerUnlinkedUsers _: () => Future[Int]) when() returns(Future(0))
    val mockMapping = Map("client" -> mockReference)

    val command = new RegisterAllUsers(mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Failed to register any new users"
    }
  }

  it should "eventually return a response if the client is not recognized" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "test: register all unlinked users",
      "client"
    )
    val mockMapping: Map[String, ClientReference] = Map()

    val expected = "test client client does not currently support bulk user registration"
    val command = new RegisterAllUsers(mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual expected
    }
  }

  it should "never return a response if parsing fails" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "Invalid message",
      "client"
    )

    val mockReference = stub[ClientReference]
    val mockMapping = Map("notClient" -> mockReference)

    val command = new RegisterAllUsers(mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe None
    }
  }
}
