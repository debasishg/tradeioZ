package tradex.domain
package repository

import zio._
import zio.prelude.NonEmptyList
import model.execution._

trait ExecutionRepository {

  /** store */
  def store(exe: Execution): Task[Execution]

  /** store many executions */
  def storeMany(executions: NonEmptyList[Execution]): Task[Unit]
}

object ExecutionRepository {
  def store(exe: Execution): RIO[Has[ExecutionRepository], Execution] =
    ZIO.serviceWith[ExecutionRepository](_.store(exe))

  def storeMany(executions: NonEmptyList[Execution]): RIO[Has[ExecutionRepository], Unit] =
    ZIO.serviceWith[ExecutionRepository](_.storeMany(executions))
}
