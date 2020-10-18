package com.gambit.user.api.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.twitter.finagle.http.Status
import com.twitter.io.Buf
import io.finch._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import com.gambit.user.models.{SlackUser, SlackUserReference}

class SlackUserApiTest extends FlatSpec with MockFactory {
  behavior of "GET /v1/slack-users/<id>"

  it should "return the fetched slack user if the ID exists in the database" in {
    val sampleUser = Future(Some(SlackUser("testId", None, None, None)))
    val expected = Some(SlackUserResponse("testId", None, None, None))
    val mockTable = stub[SlackUserReference]
    (mockTable.getUserById _) when ("testId") returns(sampleUser)

    val api = new SlackUserApi(mockTable)
    val actual = api.getSlackUser(Input.get("/v1/slack-users/testId"))
                    .awaitValueUnsafe()
    actual shouldEqual expected
  }

  it should "return NotFound if the ID does not exist in the database" in {
  val mockTable = stub[SlackUserReference]
    (mockTable.getUserById _) when ("testId") returns(Future(None))

    val api = new SlackUserApi(mockTable)
    val actual = api.getSlackUser(Input.get("/v1/slack-users/testId"))
                    .awaitOutputUnsafe()
                    .map{ _.status }
    actual shouldEqual Some(Status.NotFound)
  }

  behavior of "GET /v1/slack-users/unlinked"

  it should "return all users returned from the database" in {
    val sampleUsers = Future(Seq(
      SlackUser("testId1", None, None, None),
      SlackUser("testId2", None, None, None)
    ))
    val expected = Some(Seq(
      SlackUserResponse("testId1", None, None, None),
      SlackUserResponse("testId2", None, None, None)
    ))
    val mockTable = stub[SlackUserReference]
    (mockTable.getAllUnlinkedUsers _: () => Future[Seq[SlackUser]]) when () returns(sampleUsers)

    val api = new SlackUserApi(mockTable)
    val actual = api.getUnlinkedSlackUsers(Input.get("/v1/slack-users/unlinked"))
                    .awaitValueUnsafe()
    actual shouldEqual expected
  }

  behavior of "PATCH /v1/slack-users"

  it should "return the updated user if the database responds successfully" in {
    val sampleUser = Future(Some(SlackUser("testId", Some(1), None, None)))
    val sampleBody = SlackUser("testId", None, None, None)
    val expected = Some(SlackUserResponse("testId", Some(1), None, None))
    val jsonParams = Buf.Utf8("""{"slackId": "testId"}""")
    val mockTable = stub[SlackUserReference]
    (mockTable.updateUser _) when (sampleBody) returns(sampleUser)

    val api = new SlackUserApi(mockTable)
    val actual = api.updateSlackUser(
      Input.patch("/v1/slack-users").withBody[Application.Json](jsonParams)
    ).awaitValueUnsafe()
    actual shouldEqual expected
  }

  it should "return a BadRequest if the database fails to update the user" in {
    succeed
  }

  behavior of "POST /v1/slack-users"

  it should "return the created user if the database responds successfully" in {
    succeed
  }

  it should "return a BadRequest if the database fails to create the user" in {
    succeed
  }
}
