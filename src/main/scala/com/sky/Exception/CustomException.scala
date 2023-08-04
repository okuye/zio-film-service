package com.sky.Exception

sealed trait AppError {
  def getMessage: String
}

final case class CustomException(msg: String, cause: Option[Throwable] = None) extends Throwable(msg) with AppError {

  cause.foreach(initCause)
}
