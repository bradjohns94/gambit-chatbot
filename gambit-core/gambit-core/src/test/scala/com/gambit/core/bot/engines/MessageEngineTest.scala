package com.gambit.core.bot.engines

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.scalatest.Matchers._

import com.gambit.core.bot.commands.Command
import com.gambit.core.common.{ClientMessage, ClientMessageResponse, CoreMessage, CoreResponse}
import com.gambit.core.clients.{GambitUserClient, UserClient}

case class Config(
  override val unregisteredCommands: Seq[Command],
  override val registeredCommands: Seq[Command],
  override val adminCommands: Seq[Command],
  override val clientMapping: Map[String, UserClient],
  override val gambitUserClient: GambitUserClient
) extends MessageConfig

class MessageEngineTest extends FlatSpec with PrivateMethodTester with MockFactory {
  behavior of "parseMessage"

  it should "return an empty sequence when no user is resolved" in {
    val sampleMessage = ClientMessage("userId", "username", "channel", "message", "client")
    val mockGambitClient = stub[GambitUserClient]
    val config = new Config(Seq(), Seq(), Seq(), Map(), mockGambitClient)
    val engine = new MessageEngine(config)
    engine.parseMessage(sampleMessage).map{ result =>
      result shouldEqual ClientMessageResponse(Seq.empty[CoreResponse])
    }
  }

  it should "return an empty sequence when no messages parse" in {
    val sampleMessage = ClientMessage("userId", "username", "channel", "message", "client")
    val sampleCoreMessage = CoreMessage(
      "userId", "username", "channel", "message", "client", None)

    val mockGambitClient = stub[GambitUserClient]
    val mockUserClient = stub[UserClient]
    val mockCommand = stub[Command]
    (mockUserClient.getGambitUserId _) when("userId") returns(Future(None))
    (mockCommand.runCommand _) when(sampleCoreMessage) returns(Future(None))

    val config = new Config(Seq(mockCommand), Seq(), Seq(), Map("client" -> mockUserClient), mockGambitClient)
    val engine = new MessageEngine(config)

    engine.parseMessage(sampleMessage).map{ result =>
      result shouldEqual ClientMessageResponse(Seq.empty[CoreResponse])
    }
  }

  it should "return a sequence of responses on successful parse" in {
    val sampleMessage = ClientMessage("userId", "username", "channel", "message", "client")
    val sampleCoreMessage = CoreMessage(
      "userId", "username", "channel", "message", "client", None)
    val sampleResponse = CoreResponse("test", "channel")

    val mockGambitClient = stub[GambitUserClient]
    val mockUserClient = stub[UserClient]
    val mockCommand = stub[Command]
    (mockUserClient.getGambitUserId _) when("userId") returns(Future(None))
    (mockCommand.runCommand _) when(sampleCoreMessage) returns(Future(Some(sampleResponse)))

    val config = new Config(Seq(mockCommand), Seq(), Seq(), Map("client" -> mockUserClient), mockGambitClient)
    val engine = new MessageEngine(config)

    engine.parseMessage(sampleMessage).map{ result =>
      result shouldEqual ClientMessageResponse(Seq(sampleResponse))
    }
  }
}
