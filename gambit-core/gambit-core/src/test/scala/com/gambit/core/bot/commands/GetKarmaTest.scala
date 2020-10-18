package com.gambit.core.bot.commands

import scala.concurrent.Future

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers._

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.clients.{Karma, KarmaClient}

class GetKarmaTest extends AsyncFlatSpec with AsyncMockFactory {
  behavior of "runCommand"

  it should "eventually return a response with the fetched value when successful" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "test karma Thing",
      "client",
      None
    )

    val mockClient = stub[KarmaClient]
    (mockClient.getKarma _) when("thing") returns(Future(Some(Karma("thing", 42, None, None, None))))

    val command = new GetKarma(mockClient)
    val actual = command.runCommand(sampleMessage)
    actual.map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Karma for Thing is 42"
    }
  }

  it should "eventually return a response with trimmer parens" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "test karma (foo Bar)",
      "client",
      None
    )

    val mockClient = stub[KarmaClient]
    (mockClient.getKarma _) when("foo bar") returns(Future(Some(Karma("foo bar", 42, None, None, None))))

    val command = new GetKarma(mockClient)
    val actual = command.runCommand(sampleMessage)
    actual.map{ result =>
      result shouldBe a [Some[_]]
      result.get.messageText shouldEqual "Karma for foo Bar is 42"
    }
  }

  it should "never return if parsing fails" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "Invalid message",
      "client",
      None
    )
    val mockClient = stub[KarmaClient]

    val command = new GetKarma(mockClient)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe None
    }
  }
}
