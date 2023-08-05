package com.sky.ref

import com.sky.ConfigLoader
import com.sky.Services._
import com.thirdparty.movie.MovieService
import zio._
import zio.console._

object StartService extends App {
  private type AppEnvironment = Has[CmdClient] with Console


  val movieServiceLayer: ZLayer[Any, Nothing, Has[MovieService]] =
    ZLayer.succeed[MovieService](new RunMovieService)

  val fullLayer: ZLayer[Console, Nothing, AppEnvironment] =
    ((movieServiceLayer >>> ParentalControlService.live) ++ Console.live) >+> CmdClient.live

  val app: ZIO[AppEnvironment, Throwable, Unit] = for {
    config      <- ConfigLoader.loadConfigFromFile
    validLevels <- ConfigLoader.parseConfig(config)

    _         <- putStrLn("Welcome to the Parental Control Service!")
    _         <- putStrLn(s"Available movie ids are : 1, 2, 3")
    _         <- putStrLn("Valid Parental control levels are :")
    _         <- putStrLn(validLevels.mkString("\n"))
    _         <- putStrLn("\n############################################\n")
    cmdClient <- ZIO.service[CmdClient]
    _ <- cmdClient.start(validLevels)
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    app.provideCustomLayer(fullLayer).exitCode
}
