package com.gambit.core.clients

import com.typesafe.scalalogging.Logger

trait Client{
  val apiName: String
  val logger: Logger

  /** Unpack Response
   *  Convert an STTP Response object containing the value we want to unpack
   *  @param response the object to unpack the response from
   *  @return the response if possible otherwise none
   */
  protected def unpackResponse[T](response: Response[T]): Option[T] = {
    if (response.isSuccess) {
      response.body match {
        case Right(data) => Some(data)
        case Left(message) => {
          logger.warn(s"Failed to parse response: ${message}")
          None
        }
      }
    }
    else {
      logger.warn(s"${apiName} responded with error code: ${response.code}")
      None
    }
  }
}