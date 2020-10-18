package com.gambit.core.bot.commands

import scala.concurrent.Future

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers._

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.clients.{Alias, AliasClient, GambitUser, Karma, KarmaClient}
import com.gambit.core.models.RedisReference

class ChangeKarmaTest extends AsyncFlatSpec with AsyncMockFactory {
  behavior of "runCommand"

  it should "eventually return a one-line response after one successful change" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "foo ++",
      "client",
      Some(GambitUser(1, "user", false, "", None, None))
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(10, 60, 20, 60)

    val mockKarma = stub[KarmaClient]
    val mockAliases = stub[AliasClient]
    val mockRedis = stub[RedisReference]
    (mockKarma.getKarmaForUser _) when(1) returns(Future(Seq.empty[Karma]))
    (mockKarma.updateKarma _) when(*) returns(Future(Seq(
      Karma("foo", 1, None, None, None)
    )))
    (mockAliases.getPrimaryName _) when("foo") returns(Future(None))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 1, 60) returns(Some(0))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 1, 60) returns(Some(0))

    val command = new ChangeKarma(mockAliases, mockKarma, mockRedis, rateLimitConfig)
    val expected = CoreResponse("Gave 1 karma to Foo, total: 1", "channel")
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get shouldEqual expected
    }
  }

  it should "eventually return a multi-line response after mutiple successfully changes" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "foo++ (foo bar)++ baz-- Foo--",
      "client",
      Some(GambitUser(1, "user", false, "", None, None))
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(10, 60, 20, 60)

    val mockKarma = stub[KarmaClient]
    val mockAliases = stub[AliasClient]
    val mockRedis = stub[RedisReference]
    (mockKarma.getKarmaForUser _) when(1) returns(Future(Seq.empty[Karma]))
    (mockKarma.updateKarma _) when(*) returns(Future(Seq(
      Karma("foo", 1, None, None, None),
      Karma("foo bar", 2, None, None, None),
      Karma("baz", 0, None, None, None)
    )))
    (mockAliases.getPrimaryName _) when("foo") returns(Future(None))
    (mockAliases.getPrimaryName _) when("foo bar") returns(Future(None))
    (mockAliases.getPrimaryName _) when("baz") returns(Future(None))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 4, 60) returns(Some(0))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 4, 60) returns(Some(0))

    val command = new ChangeKarma(mockAliases, mockKarma, mockRedis, rateLimitConfig)
    val expected = CoreResponse(Seq(
      "username giveth and username taketh away",
      "Gave 1 karma to Foo bar, total: 2",
      "Took 1 karma from Baz, total: 0"
    ).mkString("\n"), "channel")
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get shouldEqual expected
    }
  }

  it should "never return a user rate limit error without a provided user" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "foo ++",
      "client",
      None
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(1, 60, 20, 60)

    val mockKarma = stub[KarmaClient]
    val mockAliases = stub[AliasClient]
    val mockRedis = stub[RedisReference]
    (mockKarma.getKarmaForUser _) when(1) returns(Future(Seq.empty[Karma]))
    (mockKarma.updateKarma _) when(*) returns(Future(Seq(
      Karma("foo", 1, None, None, None)
    )))
    (mockAliases.getPrimaryName _) when("foo") returns(Future(None))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 1, 60) returns(Some(0))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 1, 60) returns(Some(10))

    val command = new ChangeKarma(mockAliases, mockKarma, mockRedis, rateLimitConfig)
    val expected = CoreResponse("Gave 1 karma to Foo, total: 1", "channel")
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get shouldEqual expected
    }
  }

  it should "treat None returns from redis as rate-limit passes" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "foo ++",
      "client",
      None
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(1, 60, 20, 60)

    val mockKarma = stub[KarmaClient]
    val mockAliases = stub[AliasClient]
    val mockRedis = stub[RedisReference]
    (mockKarma.getKarmaForUser _) when(1) returns(Future(Seq.empty[Karma]))
    (mockKarma.updateKarma _) when(*) returns(Future(Seq(
      Karma("foo", 1, None, None, None)
    )))
    (mockAliases.getPrimaryName _) when("foo") returns(Future(None))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 1, 60) returns(None)
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 1, 60) returns(None)

    val command = new ChangeKarma(mockAliases, mockKarma, mockRedis, rateLimitConfig)
    val expected = CoreResponse("Gave 1 karma to Foo, total: 1", "channel")
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get shouldEqual expected
    }
  }

  it should "eventually return an error response when the user rate limit is exceeded" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "foo ++",
      "client",
      Some(GambitUser(1, "user", false, "", None, None))
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(1, 60, 20, 60)

    val mockKarma = stub[KarmaClient]
    val mockAliases = stub[AliasClient]
    val mockRedis = stub[RedisReference]
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 1, 60) returns(Some(0))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 1, 60) returns(Some(10))

    val command = new ChangeKarma(mockAliases, mockKarma, mockRedis, rateLimitConfig)
    val expected = CoreResponse("Bitch be cool!", "channel")
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get shouldEqual expected
    }
  }

  it should "eventually return an error response when the channel rate limit is exceeded" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "foo ++",
      "client",
      Some(GambitUser(1, "user", false, "", None, None))
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(10, 60, 1, 60)

    val mockKarma = stub[KarmaClient]
    val mockAliases = stub[AliasClient]
    val mockRedis = stub[RedisReference]
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 1, 60) returns(Some(10))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 1, 60) returns(Some(0))

    val command = new ChangeKarma(mockAliases, mockKarma, mockRedis, rateLimitConfig)
    val expected = CoreResponse("Bitch be cool!", "channel")
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe a [Some[_]]
      result.get shouldEqual expected
    }
  }

  it should "never return a response if parsing fails" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "foo)( ++",
      "client",
      Some(GambitUser(1, "user", false, "", None, None))
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(10, 60, 1, 60)

    val mockKarma = stub[KarmaClient]
    val mockAliases = stub[AliasClient]
    val mockRedis = stub[RedisReference]

    val command = new ChangeKarma(mockAliases, mockKarma, mockRedis, rateLimitConfig)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe None
    }
  }
}
