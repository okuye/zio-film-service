package com.sky

import com.sky.Exception.CustomException
import com.sky.Services.ParentalControlService
import zio._
import zio.test.Assertion._
import zio.test._

object ServicesTest extends DefaultRunnableSpec {
  def spec = suite("ServicesTest")(
    suite("ParentalControlService")(
      testM("successfully matches preferred and movie levels") {
        val result = for {
          service <- ZIO.service[ParentalControlServiceTest]
          _ <- service.setAllowance("PG", "1", Right(true))
          allowed <- service.isAllowedToWatchMovie("PG", "1")
        } yield allowed

        assertM(result.provideLayer(ServiceMocks.parentalControlLayer))(isTrue)
      },
      testM("fails when preferred level doesn't match movie level") {
        val result = for {
          service <- ZIO.service[ParentalControlServiceTest]
          _ <- service.setAllowance("U", "1", Right(false))
          allowed <- service.isAllowedToWatchMovie("U", "1")
        } yield allowed

        assertM(result.provideLayer(ServiceMocks.parentalControlLayer))(isFalse)
      },
      testM("propagates error when movie is not found") {
        val result = for {
          service <- ZIO.service[ParentalControlService]
          _ <- service.isAllowedToWatchMovie("PG", "2")
        } yield ()

        assertM(
          result
            .provideLayer(
              ServiceMocks.mockMovieService >>> Services.ParentalControlService.live
            )
            .run
        )(
          fails(
            isSubtype[CustomException](hasMessage(equalTo("Unknown movie")))
          )
        )
      }
    )
  )
}
