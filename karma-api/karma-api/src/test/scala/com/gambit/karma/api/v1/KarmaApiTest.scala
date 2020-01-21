package com.gambit.karma.api.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.twitter.io.Buf
import io.finch._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import com.gambit.karma.models.{Karma, KarmaReference}

class KaramApiTest extends FlatSpec with MockFactory {
  behavior of "GET /v1/karma/<name>"

  it should "return the fetched karma if it exists in the database" in {
    val sampleKarma = Future(Some(Karma("test", Some(42), None, None, None)))
    val expected = Some(KarmaResponse("test", 42, None, None, None))
    val mockTable = stub[KarmaReference]
    (mockTable.getKarmaByName _) when("test") returns(sampleKarma)

    val api = new KarmaApi(mockTable)
    api.getKarma(Input.get("/v1/karma/test")).awaitValueUnsafe() shouldEqual expected
  }

  it should "return a zeroed karma object if it does not exist in the database" in {
    val sampleKarma = Future(None)
    val expected = Some(KarmaResponse("test", 0, None, None, None))
    val mockTable = stub[KarmaReference]
    (mockTable.getKarmaByName _) when("test") returns(sampleKarma)

    val api = new KarmaApi(mockTable)
    api.getKarma(Input.get("/v1/karma/test")).awaitValueUnsafe() shouldEqual expected
  }

  behavior of "GET /v1/karma/user/<userId>"

  it should "return a list of all rows with the linked user ID" in {
    val sampleKarma = Future(Seq(
      Karma("foo", Some(42), Some(1), None, None),
      Karma("bar", Some(420), Some(1), None, None)
    ))
    val expected = Some(Seq(
      KarmaResponse("foo", 42, Some(1), None, None),
      KarmaResponse("bar", 420, Some(1), None, None),
    ))
    val mockTable = stub[KarmaReference]
    (mockTable.getUserLinkedKarma _) when(1) returns(sampleKarma)

    val api = new KarmaApi(mockTable)
    api.getKarmaForUser(Input.get("/v1/karma/user/1")).awaitValueUnsafe() shouldEqual expected
  }

  it should "return an empty list of rows for user IDs that do not exist" in {
    val sampleKarma = Future(Seq.empty[Karma])
    val expected = Some(Seq.empty[KarmaResponse])
    val mockTable = stub[KarmaReference]
    (mockTable.getUserLinkedKarma _) when(1) returns(sampleKarma)

    val api = new KarmaApi(mockTable)
    api.getKarmaForUser(Input.get("/v1/karma/user/1")).awaitValueUnsafe() shouldEqual expected
  }

  behavior of "POST /v1/karma"

  it should "return a list of changed karma values" in {
    val sampleKarma = Future(Seq(
      Karma("foo", Some(42), Some(1), None, None),
      Karma("bar", Some(420), Some(1), None, None)
    ))
    val expected = Some(Seq(
      KarmaResponse("foo", 42, Some(1), None, None),
      KarmaResponse("bar", 420, Some(1), None, None)
    ))
    val jsonParams = Buf.Utf8("""{"foo": 1, "bar": -2}""")
    val params = Map("foo" -> 1, "bar" -> -2)
    val mockTable = stub[KarmaReference]
    (mockTable.incrementKarma _) when(params) returns(sampleKarma)

    val api = new KarmaApi(mockTable)
    val actual = api.updateKarma(Input.post("/v1/karma").withBody[Application.Json](jsonParams))
    actual.awaitValueUnsafe() shouldEqual expected
  }
}
