package com.gambit.core.bot.commands

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.scalatest.Matchers._

import com.gambit.core.common.{CoreMessage, CoreResponse}

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
    val validCommands = validMessages.map{ CoreMessage("userId", "user", _, "client") }
    val hello = new Hello
    validCommands.foreach{ message =>
      hello.runCommand(message).map{ result =>
        result shouldBe a [Some[_]]
      }
    }
  }

  it should "return None for non-matching messages" in {
    val invalidCommands = invalidMessages.map{ CoreMessage("userId", "user", _, "client") }
    val hello = new Hello
    invalidCommands.foreach{ message =>
      hello.runCommand(message).map{ result =>
        result shouldEqual None
      }
    }
  }

  // Test parse
  behavior of "parse"

  it should "return true for valid inputs" in {
    val parse = PrivateMethod[Boolean]('parse)
    val hello = new Hello
    validMessages.foreach{ message => hello invokePrivate parse(message) shouldBe true}
  }

  it should "return false for invalid inputs" in {
    val parse = PrivateMethod[Boolean]('parse)
    val hello = new Hello
    invalidMessages.foreach{ message => hello invokePrivate parse(message) shouldBe false}
  }

  // Test makeMessage
  "makeMessage" should "respond deterministically with a seed" in {
    val makeMessage = PrivateMethod[CoreResponse]('makeMessage)
    val hello = new Hello
    val actual = hello invokePrivate makeMessage("user", Some(1L))
    actual shouldEqual CoreResponse("Greetings, user!")
  }
}
