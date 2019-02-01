package com.gambit.core.api

import cats.effect.IO
import io.finch.{Bootstrap, Endpoint}

trait GambitEndpoint[A] {
  val endpoints: Endpoint[IO,A]
}
