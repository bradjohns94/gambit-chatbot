package com.gambit.core.bot.engines

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import com.gambit.core.bot.commands._
import com.gambit.core.common.Types.{CoreMessage, CoreResponse}

class MessageEngineTest extends FlatSpec {
  behavior of "parseMessage"

  it should "return the passed responses on successful parse" in {
    val futureResult = MessageEngine.parseMessage(CoreMessage("user", "hi gambit"))
    futureResult map { result =>
      result shouldBe a [Seq[_]]
      result should not be empty
      result(0) shouldBe a [CoreResponse]
    }
  }

  it should "return an empty sequence when no messages parse" in {
    val futureResult = MessageEngine.parseMessage(CoreMessage("user", ""))
    futureResult map { result =>
      result shouldBe a [Seq[_]]
      result shouldBe empty
    }
  }

}
