package com.gambit.core.bot.commands

import scala.concurrent.Future

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers._
import slick.jdbc.PostgresProfile.api.Database

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.clients.{GambitUser, GambitUserClient, User, UserClient}

class RegisterAllUsersTest extends AsyncFlatSpec with AsyncMockFactory {
  behavior of "runCommand"

  it should "eventually return a response after successfully registering some users" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "test: register all unlinked users",
      "client",
      None
    )
    val mockGambitUserClient = stub[GambitUserClient]
    val mockUserClient = stub[UserClient]
    (mockUserClient.getUnlinkedUsers _: () => Future[Seq[User]]) when() returns(Future(Seq(
      User("1", None),
      User("2", None))
    ))
    (mockGambitUserClient.createGambitUser _) when("1") returns(Future(Some(GambitUser(
      1, "foo", false, "", None, None))))
    (mockGambitUserClient.createGambitUser _) when("2") returns(Future(Some(GambitUser(
      2, "bar", false, "", None, None))))
    (mockUserClient.setGambitUser (_, _)) when("1", 1) returns(Future(Some(User("1", Some(1)))))
    (mockUserClient.setGambitUser (_, _)) when("2", 2) returns(Future(Some(User("2", Some(2)))))
    val mockMapping = Map("client" -> mockUserClient)

    val command = new RegisterAllUsers(mockGambitUserClient, mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Successfully registered 2 users"
    }
  }

  it should "eventually return a response after failing to register any users" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "test: register all unlinked users",
      "client",
      None
    )
    val mockGambitUserClient = stub[GambitUserClient]
    val mockUserClient = stub[UserClient]
    (mockUserClient.getUnlinkedUsers _: () => Future[Seq[User]]) when() returns(Future(Seq()))
    val mockMapping = Map("client" -> mockUserClient)

    val command = new RegisterAllUsers(mockGambitUserClient, mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Failed to register any new users"
    }
  }

  it should "eventually return a response if the client is not recognized" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "test: register all unlinked users",
      "client",
      None
    )
    val mockGambitUserClient = stub[GambitUserClient]
    val mockMapping: Map[String, UserClient] = Map()

    val expected = "test client client does not currently support bulk user registration"
    val command = new RegisterAllUsers(mockGambitUserClient, mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual expected
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

    val command = new RegisterAllUsers(mockGambitUserClient, mockMapping)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe None
    }
  }
}
