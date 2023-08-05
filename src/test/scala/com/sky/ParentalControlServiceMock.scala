package com.sky

import com.sky.Exception.CustomException
import zio.{Has, IO, UIO, ULayer, ZLayer}

object ParentalControlServiceMock {
  private var expectedResponse: IO[CustomException, Boolean] =
    IO.fail(CustomException("No expectation set"))

  val live: ULayer[Has[ParentalControlServiceTest]] =
    ZLayer.succeed(
      new ParentalControlServiceTest {
        override def isAllowedToWatchMovie(
            preferredLevel: String,
            movieId: String
        ): IO[CustomException, Boolean] =
          expectedResponse

        override def setAllowance(
            preferredLevel: String,
            movieId: String,
            response: Either[CustomException, Boolean]
        ): UIO[Unit] =
          UIO {
            expectedResponse = response.fold(IO.fail(_), IO.succeed(_))
          }

        def reset(): UIO[Unit] = UIO {
          expectedResponse = IO.fail(CustomException("No expectation set"))
        }
      }
    )
}
