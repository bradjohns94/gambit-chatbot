package com.gambit.core.bot.commands

import scala.concurrent.Future

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers._

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.KarmaReference

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

    val mockReference = stub[KarmaReference]
    (mockReference.getKarmaValue _) when("thing") returns(Future(42))

    val command = new GetKarma(mockReference)
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

    val mockReference = stub[KarmaReference]
    (mockReference.getKarmaValue _) when("foo bar") returns(Future(42))

    val command = new GetKarma(mockReference)
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
    val mockReference = stub[KarmaReference]

    val command = new GetKarma(mockReference)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe None
    }
  }
}
