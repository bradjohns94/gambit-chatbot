package com.gambit.core.bot.commands

import scala.concurrent.Future

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers._

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.{AliasReference, GambitUser, KarmaReference, RedisReference}

class ChangeKarmaTest extends AsyncFlatSpec with AsyncMockFactory {
  behavior of "runCommand"

  it should "eventually return a one-line response after one successful change" in {
    val sampleMessage = CoreMessage(
      "userId",
      "username",
      "channel",
      "foo ++",
      "client",
      Some(GambitUser(Some(1), "user", None, None, None, None))
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(10, 60, 20, 60)

    val mockKarma = stub[KarmaReference]
    val mockAliases = stub[AliasReference]
    val mockRedis = stub[RedisReference]
    (mockKarma.getUserLinkedKarma _) when(1) returns(Future(Seq.empty[String]))
    (mockKarma.incrementKarma _) when(*) returns(
      Future(Map("foo" -> (1, 1)))
    )
    (mockAliases.getPrimaryName _) when("foo") returns(Future("foo"))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 1, 60) returns(Some(0))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 1, 60) returns(Some(0))

    val command = new ChangeKarma(mockKarma, mockAliases, mockRedis, rateLimitConfig)
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
      "foo++ bar++ baz-- foo--",
      "client",
      Some(GambitUser(Some(1), "user", None, None, None, None))
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(10, 60, 20, 60)

    val mockKarma = stub[KarmaReference]
    val mockAliases = stub[AliasReference]
    val mockRedis = stub[RedisReference]
    (mockKarma.getUserLinkedKarma _) when(1) returns(Future(Seq.empty[String]))
    (mockKarma.incrementKarma _) when(*) returns(
      Future(Map("foo" -> (0, 1), "bar" -> (1, 2), "baz" -> (-1, 0)))
    )
    (mockAliases.getPrimaryName _) when("foo") returns(Future("foo"))
    (mockAliases.getPrimaryName _) when("bar") returns(Future("bar"))
    (mockAliases.getPrimaryName _) when("baz") returns(Future("baz"))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 4, 60) returns(Some(0))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 4, 60) returns(Some(0))

    val command = new ChangeKarma(mockKarma, mockAliases, mockRedis, rateLimitConfig)
    val expected = CoreResponse(Seq(
      "username giveth and username taketh away",
      "Gave 1 karma to Bar, total: 2",
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

    val mockKarma = stub[KarmaReference]
    val mockAliases = stub[AliasReference]
    val mockRedis = stub[RedisReference]
    (mockKarma.getUserLinkedKarma _) when(1) returns(Future(Seq.empty[String]))
    (mockKarma.incrementKarma _) when(*) returns(
      Future(Map("foo" -> (1, 1)))
    )
    (mockAliases.getPrimaryName _) when("foo") returns(Future("foo"))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 1, 60) returns(Some(0))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 1, 60) returns(Some(10))

    val command = new ChangeKarma(mockKarma, mockAliases, mockRedis, rateLimitConfig)
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

    val mockKarma = stub[KarmaReference]
    val mockAliases = stub[AliasReference]
    val mockRedis = stub[RedisReference]
    (mockKarma.getUserLinkedKarma _) when(1) returns(Future(Seq.empty[String]))
    (mockKarma.incrementKarma _) when(*) returns(
      Future(Map("foo" -> (1, 1)))
    )
    (mockAliases.getPrimaryName _) when("foo") returns(Future("foo"))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 1, 60) returns(None)
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 1, 60) returns(None)

    val command = new ChangeKarma(mockKarma, mockAliases, mockRedis, rateLimitConfig)
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
      Some(GambitUser(Some(1), "user", None, None, None, None))
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(1, 60, 20, 60)

    val mockKarma = stub[KarmaReference]
    val mockAliases = stub[AliasReference]
    val mockRedis = stub[RedisReference]
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 1, 60) returns(Some(0))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 1, 60) returns(Some(10))

    val command = new ChangeKarma(mockKarma, mockAliases, mockRedis, rateLimitConfig)
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
      Some(GambitUser(Some(1), "user", None, None, None, None))
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(10, 60, 1, 60)

    val mockKarma = stub[KarmaReference]
    val mockAliases = stub[AliasReference]
    val mockRedis = stub[RedisReference]
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:Channel:channel", 1, 60) returns(Some(10))
    (mockRedis.incrbyex(_, _, _)) when("RateLimit:client:User:1", 1, 60) returns(Some(0))

    val command = new ChangeKarma(mockKarma, mockAliases, mockRedis, rateLimitConfig)
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
      Some(GambitUser(Some(1), "user", None, None, None, None))
    )
    val rateLimitConfig = ChangeKarmaRateLimitConfig(10, 60, 1, 60)

    val mockKarma = stub[KarmaReference]
    val mockAliases = stub[AliasReference]
    val mockRedis = stub[RedisReference]

    val command = new ChangeKarma(mockKarma, mockAliases, mockRedis, rateLimitConfig)
    command.runCommand(sampleMessage).map{ result =>
      result shouldBe None
    }
  }
}
