package tradex.domain

import zio.test._
import zio.test.Assertion._

import generators._
import model.account._
import repository.AccountRepository
import tradex.domain.repository.inmemory.AccountRepositoryInMemory

object AccountRepositorySpec extends DefaultRunnableSpec {
  val spec = suite("AccountRepository")(
    testM("successfully store an account") {
      checkM(accountGen) { account =>
        for {
          stored <- AccountRepository.store(account)
        } yield assert(stored.accountType)(
          isOneOf(List(AccountType.Trading, AccountType.Settlement, AccountType.Both))
        )
      }
    }
  ).provideCustomLayer(AccountRepositoryInMemory.layer)
}
