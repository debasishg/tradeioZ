package tradex.domain
package repository.inmemory

import zio._
import zio.prelude.NonEmptyList
import model.execution._
import repository.ExecutionRepository

final case class ExecutionRepositoryInMemory(state: Ref[Map[ExecutionReferenceNo, Execution]])
    extends ExecutionRepository {
  def store(exe: Execution): Task[Execution] = state.update(m => m + ((exe.executionRefNo.get, exe))).map(_ => exe)

  def storeMany(executions: NonEmptyList[Execution]): Task[Unit] =
    state.update(m => m ++ (executions.map(exe => (exe.executionRefNo.get, exe))))
}

object ExecutionRepositoryInMemory {
  val layer: ULayer[Has[ExecutionRepository]] =
    Ref.make(Map.empty[ExecutionReferenceNo, Execution]).map(r => ExecutionRepositoryInMemory(r)).toLayer
}
