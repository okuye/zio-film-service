package com.sky

import com.sky.Exception.CustomException
import com.sky.Services.ParentalControlService
import zio.UIO

trait ParentalControlServiceTest extends ParentalControlService {
  def setAllowance(
      preferredLevel: String,
      movieId: String,
      response: Either[CustomException, Boolean]
  ): UIO[Unit]
}
