package com.gambit.user.api.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.twitter.finagle.http.Status
import com.twitter.io.Buf
import io.finch._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import com.gambit.user.models.{GambitUser, GambitUserReference}

class GambitUserApiTest extends FlatSpec with MockFactory {
  behavior of "GET /v1/gambit-users/<id>"

  it should "return the fetched gambit user if the ID exists in the database" in {
    val sampleUser = Future(Some(GambitUser(Some(1), "test", None, None, None, None)))
    val expected = Some(GambitUserResponse(1, "test", false, "", None, None))
    val mockTable = stub[GambitUserReference]
    (mockTable.getUserById _) when (1) returns(sampleUser)

    val api = new GambitUserApi(mockTable)
    api.getGambitUser(Input.get("/v1/gambit-users/1")).awaitValueUnsafe() shouldEqual expected
  }

  it should "return a NotFound if the ID does not exist in the database" in {
    val mockTable = stub[GambitUserReference]
    (mockTable.getUserById _) when (1) returns(Future(None))

    val api = new GambitUserApi(mockTable)
    val actual = api.getGambitUser(Input.get("/v1/gambit-users/1"))
                    .awaitOutputUnsafe()
                    .map{ _.status }
    actual shouldEqual Some(Status.NotFound)
  }

  behavior of "GET /v1/gambit-users/nickname/<nickname>"

  it should "return the fetched gambit user if the nickname exists in the database" in {
    val sampleUser = Future(Some(GambitUser(Some(1), "test", None, None, None, None)))
    val expected = Some(GambitUserResponse(1, "test", false, "", None, None))
    val mockTable = stub[GambitUserReference]
    (mockTable.getUserByNickname _) when ("test") returns(sampleUser)

    val api = new GambitUserApi(mockTable)
    val actual = api.getGambitUserByNickname(Input.get("/v1/gambit-users/nickname/test"))
                    .awaitValueUnsafe()
    actual shouldEqual expected
  }

  it should "return a NotFound if the nickname does not exist in the database" in {
    val expected = Some(GambitUserResponse(1, "test", false, "", None, None))
    val mockTable = stub[GambitUserReference]
    (mockTable.getUserByNickname _) when ("test") returns(Future(None))

    val api = new GambitUserApi(mockTable)
    val actual = api.getGambitUserByNickname(Input.get("/v1/gambit-users/nickname/test"))
                    .awaitOutputUnsafe()
                    .map{ _.status }
    actual shouldEqual Some(Status.NotFound)
  }

  behavior of "POST /v1/gambit-users"

  it should "return the created gambit user on success" in {
    val sampleUser = Future(GambitUser(Some(1), "test", None, None, None, None))
    val expected = Some(GambitUserResponse(1, "test", false, "", None, None))
    val jsonParams = Buf.Utf8("""{"nickname": "test"}""")
    val mockTable = stub[GambitUserReference]
    (mockTable.createGambitUser _) when ("test") returns(sampleUser)

    val api = new GambitUserApi(mockTable)
    val actual = api.createGambitUser(Input.post("/v1/gambit-users")
                    .withBody[Application.Json](jsonParams))
                    .awaitValueUnsafe()
    actual shouldEqual expected
  }
}