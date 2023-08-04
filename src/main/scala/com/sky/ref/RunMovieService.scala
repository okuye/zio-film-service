package com.sky.ref

import zio._
import com.sky.Exception.CustomException
import com.thirdparty.movie.MovieService

class RunMovieService extends MovieService {
  def getParentalControlLevel(movieId: String): IO[CustomException, String] = {
    ZIO
      .effect(movieId match {
        case "1" => "18"
        case "2" => "15"
        case "3" => "12"
        case _ => throw new CustomException("Sorry, we could not find the movie you are looking for.")
      })
      .mapError {
        case customException: CustomException => customException
        case other                            => new CustomException(s"Unexpected error: ${other.getMessage}")
      }
  }
}
