package tradex.domain
package repository

import zio._
import zio.blocking.Blocking
import zio.interop.catz._
import doobie.hikari._

import config._

object CatzInterop {
  import zio.interop.catz.implicits._

  implicit val zioRuntime: zio.Runtime[zio.ZEnv] = zio.Runtime.default

  implicit val dispatcher: cats.effect.std.Dispatcher[zio.Task] =
    zioRuntime
      .unsafeRun(
        cats.effect.std
          .Dispatcher[zio.Task]
          .allocated
      )
      ._1

  def mkTransactor(cfg: DBConfig): ZManaged[Blocking, Throwable, HikariTransactor[Task]] =
    for {
      rt <- ZIO.runtime[Any].toManaged_
      xa <-
        HikariTransactor
          .newHikariTransactor[Task](
            cfg.driver,
            cfg.url,
            cfg.user,
            cfg.password,
            rt.platform.executor.asEC
          )
          .toManaged
    } yield xa
}
