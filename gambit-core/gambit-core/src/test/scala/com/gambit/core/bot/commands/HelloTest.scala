package com.gambit.core.bot.commands

import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.scalatest.Matchers._

import com.gambit.core.common.Types.{CoreMessage, CoreResponse}

class HelloTest extends FlatSpec with PrivateMethodTester {
  val validMessages = Seq(
    "hi test",
    "hello test!",
    "hey, test!",
    "saLuTatiOnS: tESt?"
  )
  val invalidMessages = Seq(
    "",
    "invalid hi test"
  )

  // Test runCommand
  behavior of "runCommand"

  it should "return a response for parsable messages" in {
    val validCommands = validMessages.map{ CoreMessage("user", _) }
    validCommands.foreach{ message => Hello.runCommand(message) shouldBe a [Some[_]]}
  }

  it should "return None for non-matching messages" in {
    val invalidCommands = invalidMessages.map{ CoreMessage("user", _) }
    invalidCommands.foreach{ message => Hello.runCommand(message) shouldEqual None }
  }

  // Test parse
  behavior of "parse"

  it should "return true for valid inputs" in {
    val parse = PrivateMethod[Boolean]('parse)
    validMessages.foreach{ message => Hello invokePrivate parse(message) shouldBe true}
  }

  it should "return false for invalid inputs" in {
    val parse = PrivateMethod[Boolean]('parse)
    invalidMessages.foreach{ message => Hello invokePrivate parse(message) shouldBe false}
  }

  // Test makeMessage
  "makeMessage" should "respond deterministically with a seed" in {
    val makeMessage = PrivateMethod[CoreResponse]('makeMessage)
    val actual = Hello invokePrivate makeMessage("user", Some(1L))
    actual shouldEqual CoreResponse("Greetings, user!")
  }
}
