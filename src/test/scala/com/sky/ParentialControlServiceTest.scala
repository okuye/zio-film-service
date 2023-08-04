package com.sky

import com.sky.Exception.CustomException
import com.sky.Services._
import com.thirdparty.movie.MovieService
import zio.console.Console
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestConsole
import zio.{ Has, IO, ULayer, ZIO, ZLayer, console }
import com.sky.Services.ParentalControlService
import net.liftweb.util.Maker.vToMake
import zio._
import zio._
import zio.test._
import zio.console._

import java.io.IOException

object ServicesTest extends DefaultRunnableSpec {
  sealed trait AppError

  object AppError {
    case class CustomError(cause: CustomException) extends AppError

    case class IOError(cause: IOException) extends AppError
  }

  import zio._
  import com.sky.Exception.CustomException

//  object ParentalControlServiceMock {
//
//    // Create a Mock ref to simulate the behavior of your service
//    val mock: UIO[Ref[Map[(String, String), Either[CustomException, Boolean]]]] = Ref.make(Map.empty)
//
//    // Define the setter for the mock which allows you to inject behavior for specific inputs
//    def setAllowance(preferredLevel: String, movieId: String, allowed: Either[CustomException, Boolean]): UIO[Unit] =
//      mock.flatMap(ref => ref.update(store => store + ((preferredLevel, movieId) -> allowed)))
//
////    // Create the live layer for the mock that utilizes the mock ref
////    val live: ZLayer[Nothing, Nothing, Services.ParentalControlServiceEnv] = ZLayer.fromEffect {
////      mock.map(ref =>
////        new Services.ParentalControlService {
////          override def isAllowedToWatchMovie(preferredLevel: String, movieId: String): IO[CustomException, Boolean] =
////            ref.get.flatMap(store =>
////              IO.fromEither(store.getOrElse((preferredLevel, movieId), Left(new CustomException("Unexpected Error"))))
////            )
////        }
////      )
////    }
//    val live: ZLayer[Nothing, CustomException, Services.ParentalControlServiceEnv] = ZLayer.fromEffect {
//      mock.map(ref =>
//        new Services.ParentalControlService {
//          override def isAllowedToWatchMovie(preferredLevel: String, movieId: String): IO[CustomException, Boolean] =
//            ref.get.flatMap(store =>
//              IO.fromEither(store.getOrElse((preferredLevel, movieId), Left(new CustomException("Unexpected Error"))))
//            )
//        }
//      )
//    }
//
//  }

  object ParentalControlServiceMock {
    object TestService extends ParentalControlService {
      var expectedResponse: IO[CustomException, Boolean] = IO.fail(CustomException("No expectation set"))

      def setAllowance(preferredLevel: String, movieId: String, response: Either[CustomException, Boolean]): UIO[Unit] =
        UIO {
          expectedResponse = response.fold(IO.fail(_), IO.succeed(_))
        }

      override def isAllowedToWatchMovie(preferredLevel: String, movieId: String): IO[CustomException, Boolean] =
        expectedResponse
    }

    val live: ULayer[ParentalControlServiceEnv] = ZLayer.succeed(TestService)
  }

  // Mocks
  val mockMovieService: ULayer[Has[MovieService]] = ZLayer.succeed {
    new MovieService {
      def getParentalControlLevel(movieId: String): IO[CustomException, String] =
        movieId match {
          case "1" => IO.succeed("PG")
          case "2" => IO.fail(new CustomException("Unknown movie"))
          case _   => IO.fail(new CustomException("Unknown error"))
        }
    }
  }

  def mockConsole(inputResponses: List[String]): ULayer[TestConsole] =
    ZLayer.succeed(new TestConsole.Service {
      private val inputs = scala.collection.mutable.Queue[String](inputResponses: _*)

      def putStr(line: String): UIO[Unit] = UIO.unit

      def putStrLn(line: String): UIO[Unit] = UIO.unit

      def getStrLn: UIO[String] = UIO(inputs.dequeue())

      def putStrErr(line: String): UIO[Unit] = UIO.unit

      def putStrLnErr(line: String): UIO[Unit] = UIO.unit

      def clearInput: UIO[Unit] = UIO.unit

      def feedLines(lines: String*): UIO[Unit] = UIO(inputs ++= lines)

      def output: UIO[Vector[String]] = UIO(Vector.empty)

      def clearOutput: UIO[Unit] = UIO.unit

      def outputErr: UIO[Vector[String]] = UIO(Vector.empty)

      def silent[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] = zio

      def debug[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] = zio

      val save: UIO[UIO[Unit]] = UIO(UIO.unit)

    })

  val testConsole: ULayer[TestConsole] = mockConsole(List("PG", "1"))

  val commonEnv: ULayer[Has[MovieService] with TestConsole] = mockMovieService ++ testConsole

  val testConsoleToConsoleBridge: ZLayer[TestConsole, Nothing, Console] =
    ZLayer.fromFunction { (testEnv: Has[TestConsole.Service]) =>
      val testConsole = testEnv.get[TestConsole.Service]
      new Console.Service {
        override def putStr(line: String): IO[IOException, Unit] =
          testConsole.feedLines(line).as(())

        override def putStrErr(line: String): IO[IOException, Unit] =
          testConsole.feedLines(line).as(())

        override def putStrLn(line: String): IO[IOException, Unit] =
          testConsole.feedLines(s"$line\n").as(())

        override def putStrLnErr(line: String): IO[IOException, Unit] =
          testConsole.feedLines(s"$line\n").as(())

        override def getStrLn: IO[IOException, String] =
          testConsole.output.flatMap {
            case head +: _ => ZIO.succeed(head)
            case _         => ZIO.fail(new IOException("No input available"))
          }
      }
    }

  val consoleLayer: ZLayer[TestConsole, Nothing, Console] =
    testConsoleToConsoleBridge

  val baseLayer: ZLayer[Any, Nothing, Has[MovieService] with TestConsole] =
    mockMovieService ++ mockConsole(List("Invalid", "PG", "1"))

  val cmdClientEnv: ZLayer[Any, Nothing, Has[MovieService] with TestConsole with Console] =
    baseLayer ++ (baseLayer >>> testConsoleToConsoleBridge)

  val baseEnv: ZLayer[Any, Nothing, Has[MovieService] with TestConsole] =
    mockMovieService ++ mockConsole(List("Invalid", "PG", "1"))

  val parentalControlLayer: ZLayer[Any, Nothing, Has[ParentalControlService]] =
    baseEnv >>> ParentalControlService.live

  val cmdClientLayer: ZLayer[Has[MovieService] with TestConsole with Console, Nothing, Has[CmdClient]] =
    (parentalControlLayer ++ (ZLayer.identity[TestConsole] ++ testConsoleToConsoleBridge)) >>> CmdClient.live

  val cmdClientWithEnv: ZLayer[Has[MovieService] with TestConsole with Console, Nothing, Has[CmdClient]] =
    (ParentalControlService.live ++ ZLayer.identity[TestConsole] ++ testConsoleToConsoleBridge) >>> CmdClient.live

  val combinedLayer: ZLayer[Has[MovieService] with TestConsole with Console, Nothing, Has[
    ParentalControlService
  ] with Has[CmdClient]] =
    parentalControlLayer ++ cmdClientLayer

  val baseEnvForTest: ZLayer[Any, Nothing, Has[MovieService] with TestConsole] =
    mockMovieService ++ mockConsole(List("Invalid", "PG", "1"))

  val enrichedEnvForTest: ZLayer[Any, Nothing, Has[MovieService] with TestConsole with Console] =
    baseEnvForTest ++ (baseEnvForTest >>> testConsoleToConsoleBridge)

  val enhancedEnv: ZLayer[Any, Nothing, Has[MovieService] with TestConsole with Console] =
    baseEnv ++ (baseEnv >>> testConsoleToConsoleBridge)

  val fullEnv: ZLayer[Any, Nothing, Has[ParentalControlService] with Console] =
    parentalControlLayer ++ (baseEnv >>> testConsoleToConsoleBridge)

  val cmdClientLayerForTest: ZLayer[Any, Nothing, Has[CmdClient]] =
    fullEnv >>> CmdClient.live

  val parentalControlAndConsole: ZLayer[Any, Nothing, Has[ParentalControlService] with Console] = {
    baseLayer >>> ParentalControlService.live ++ (baseLayer >>> testConsoleToConsoleBridge)
  }

  val defaultTestEnv: ULayer[TestConsole] = mockConsole(List.empty)
  val testEnv: ULayer[TestConsole]        = defaultTestEnv

  val consoleEnv: ZLayer[TestConsole, Nothing, TestConsole with Console] = testEnv ++ testConsoleToConsoleBridge
  val tempEnv = ((mockMovieService ++ mockConsole(
    List("Invalid", "PG", "1")
  )) >+> testConsoleToConsoleBridge) >+> cmdClientLayerForTest

  def spec =
    suite("ServicesTest")(
      suite("ParentalControlService")(
        testM("successfully matches preferred and movie levels") {
          val result = for {
            _       <- ParentalControlServiceMock.TestService.setAllowance("PG", "1", Right(true))
            service <- ZIO.service[ParentalControlService]
            allowed <- service.isAllowedToWatchMovie("PG", "1")
          } yield allowed

          assertM(result.provideLayer(ParentalControlServiceMock.live))(isTrue)
        },
        testM("fails when preferred level doesn't match movie level") {
          (for {
            _       <- ParentalControlServiceMock.TestService.setAllowance("U", "1", Right(false))
            service <- ZIO.service[ParentalControlService]
            allowed <- service.isAllowedToWatchMovie("U", "1")
          } yield assert(allowed)(isFalse)).provideLayer(ParentalControlServiceMock.live)
        },
        testM("propagates error when movie is not found") {
          val result = for {
            service <- ZIO.service[Services.ParentalControlService]
            _       <- service.isAllowedToWatchMovie("PG", "2")
          } yield ()

          assertM(result.provideLayer(mockMovieService >>> Services.ParentalControlService.live).run)(
            fails(isSubtype[CustomException](hasMessage(equalTo("Unknown movie"))))
          )
        }
      )
    )
}
