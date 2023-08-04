package com.thirdparty.movie

import com.sky.Exception.CustomException
import zio.IO

trait MovieService {
  def getParentalControlLevel(movieId: String): IO[CustomException, String]
}
