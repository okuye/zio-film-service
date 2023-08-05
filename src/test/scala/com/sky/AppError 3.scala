package com.sky

import com.sky.Exception.CustomException

import java.io.IOException

sealed trait AppError

object AppError {
  case class CustomError(cause: CustomException) extends AppError
  case class IOError(cause: IOException) extends AppError
}
