package com.sky

import com.sky.Exception.CustomException
import com.thirdparty.movie.MovieService
import zio.console.Console
import zio.console.Console.Service
import zio.{Has, IO, UIO, ULayer, ZLayer}

object ServiceMocks {
  val silentConsole: ULayer[Has[Service]] = ZLayer.succeed {
    new Console.Service {
      def putStr(line: String): UIO[Unit] = UIO.unit
      def putStrLn(line: String): UIO[Unit] = UIO.unit
      def getStrLn: UIO[String] = UIO.succeed("")
      def putStrErr(line: String): UIO[Unit] = UIO.unit
      def putStrLnErr(line: String): UIO[Unit] = UIO.unit
    }
  }

  val mockMovieService: ULayer[Has[MovieService]] = ZLayer.succeed {
    new MovieService {
      def getParentalControlLevel(
          movieId: String
      ): IO[CustomException, String] =
        movieId match {
          case "1" => IO.succeed("PG")
          case "2" => IO.fail(new CustomException("Unknown movie"))
          case _   => IO.fail(new CustomException("Unknown error"))
        }
    }
  }

  val baseLayer
      : ZLayer[Any, Nothing, Has[MovieService] with Has[Console.Service]] =
    mockMovieService ++ silentConsole

  val parentalControlLayer
      : ZLayer[Any, Nothing, Has[ParentalControlServiceTest]] =
    baseLayer >>> ParentalControlServiceMock.live
}
