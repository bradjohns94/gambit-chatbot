package com.gambit.core.bot.engines

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.scalatest.Matchers._

import com.gambit.core.bot.commands.Command
import com.gambit.core.common.{CoreMessage, CoreResponse, PermissionLevel}
import com.gambit.core.models.{ClientReference, ClientUser}

case class Config(
  override val unregisteredCommands: Seq[Command],
  override val registeredCommands: Seq[Command],
  override val adminCommands: Seq[Command],
  override val clientMapping: Map[String, ClientReference]
) extends MessageConfig

case class User(override val gambitUserId: Option[Int]) extends ClientUser {
  val createdAt = None
  val updatedAt = None
}

class MessageEngineTest extends FlatSpec with PrivateMethodTester with MockFactory {
  behavior of "parseMessage"

  it should "return an empty sequence when no user is resolved" in {
    val sampleMessage = CoreMessage("userId", "username", "message", "client")
    val config = new Config(Seq(), Seq(), Seq(), Map())
    val engine = new MessageEngine(config)
    engine.parseMessage(sampleMessage).map{ result =>
      result shouldEqual Seq.empty[CoreResponse]
    }
  }

  it should "return an empty sequence when no messages parse" in {
    val sampleMessage = CoreMessage("userId", "username", "message", "client")
    val user = User(Some(1))

    val mockReference = stub[ClientReference]
    val mockCommand = stub[Command]
    (mockReference.getUserById _) when("userId") returns(Future(Some(user)))
    (mockReference.getPermissionLevel _) when(user) returns(Future(PermissionLevel.Unregistered))
    (mockCommand.runCommand _) when(sampleMessage) returns(Future(None))

    val config = new Config(Seq(mockCommand), Seq(), Seq(), Map("client" -> mockReference))
    val engine = new MessageEngine(config)

    engine.parseMessage(sampleMessage).map{ result =>
      result shouldEqual Seq.empty[CoreResponse]
    }
  }

  it should "return a sequence of responses on successful parse" in {
    val sampleMessage = CoreMessage("userId", "username", "message", "client")
    val sampleResponse = CoreResponse("test")
    val user = User(Some(1))

    val mockReference = stub[ClientReference]
    val mockCommand = stub[Command]
    (mockReference.getUserById _) when("userId") returns(Future(Some(user)))
    (mockReference.getPermissionLevel _) when(user) returns(Future(PermissionLevel.Unregistered))
    (mockCommand.runCommand _) when(sampleMessage) returns(Future(Some(sampleResponse)))

    val config = new Config(Seq(mockCommand), Seq(), Seq(), Map("client" -> mockReference))
    val engine = new MessageEngine(config)

    engine.parseMessage(sampleMessage).map{ result =>
      result shouldEqual Seq(sampleResponse)
    }
  }
}
