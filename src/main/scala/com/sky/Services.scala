package com.sky

import com.thirdparty.movie.MovieService
import java.io.IOException
import com.sky.Exception.CustomException
import zio._
import zio.console._

object Services {

  trait ParentalControlService {
    def isAllowedToWatchMovie(
        preferredLevel: String,
        movieId: String
    ): IO[CustomException, Boolean]
  }

  type ParentalControlServiceEnv = Has[ParentalControlService]
  private type CmdClientEnv = Has[CmdClient]

  object ParentalControlService {
    val live: ZLayer[Has[MovieService], Nothing, ParentalControlServiceEnv] =
      ZLayer.fromService[MovieService, ParentalControlService](movieService =>
        new ParentalControlService {
          override def isAllowedToWatchMovie(
              preferredLevel: String,
              movieId: String
          ): IO[CustomException, Boolean] = {

            movieService.getParentalControlLevel(movieId).map { movieLevel =>
              println(
                s"Preferred Level: $preferredLevel, Movie Level: $movieLevel"
              )
              val levels = List("U", "PG", "12", "15", "18")
              levels.indexOf(preferredLevel) >= levels.indexOf(movieLevel)
            }
          }
        }
      )
  }

  trait CmdClient {
    def start(validLevels: List[String]): ZIO[Console, IOException, Unit]
  }

  object CmdClient {
    val live: ZLayer[
      ParentalControlServiceEnv with Console,
      Nothing,
      CmdClientEnv
    ] =
      ZLayer.fromServices[ParentalControlService, Console.Service, CmdClient] {
        (parentalService, console) =>
          def getPreferredLevel(
              validLevels: List[String]
          ): ZIO[Console, IOException, String] =
            for {
              _ <- console.putStrLn(
                "Please enter your Parental Control Level preference:"
              )
              preferredLevel <- console.getStrLn
              result <-
                if (validLevels.contains(preferredLevel))
                  ZIO.succeed(preferredLevel)
                else
                  console.putStrLn(
                    "Choose from the Parental Control Levels displayed"
                  ) *> getPreferredLevel(validLevels)
            } yield result

          new CmdClient {
            def start(
                validLevels: List[String]
            ): ZIO[Console, IOException, Unit] = {
              for {
                preferredLevel <- getPreferredLevel(validLevels)
                _ <- console.putStrLn(
                  "Please enter the movie id you would like to view:"
                )
                movieId <- console.getStrLn
                allowed <- parentalService
                  .isAllowedToWatchMovie(preferredLevel, movieId)
                  .catchAll { err =>
                    console
                      .putStrLn(s"An error occurred: ${err.getMessage}")
                      .as(false)
                  }
                _ <- console.putStrLn(
                  if (allowed) "You may watch this movie!"
                  else "You are not allowed to watch this movie."
                )
              } yield ()
            }
          }
      }
  }
}
