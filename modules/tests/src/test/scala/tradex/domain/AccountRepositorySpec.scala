package tradex.domain

import zio.test._
import zio.test.Assertion._

import generators._
import model.account._
import repository.AccountRepository
import repository.inmemory.AccountRepositoryInMemory

object AccountRepositorySpec extends DefaultRunnableSpec {
  val spec = suite("AccountRepository")(
    testM("successfully stores an account") {
      checkM(accountGen) { account =>
        for {
          stored <- AccountRepository.store(account)
        } yield assert(stored.accountType)(
          isOneOf(AccountType.values)
        )
      }
    }.provideCustomLayer(AccountRepositoryInMemory.layer),
    testM("successfully stores an account and fetch the same") {
      checkM(accountGen) { account =>
        for {
          stored  <- AccountRepository.store(account)
          fetched <- AccountRepository.queryByAccountNo(stored.no)
        } yield assertTrue(stored.no == fetched.get.no)
      }
    }.provideCustomLayer(AccountRepositoryInMemory.layer),
    testM("successfully stores multiple accounts") {
      checkM(Gen.listOfN(5)(accountWithNamePatternGen("debasish"))) { accounts =>
        for {
          _           <- AccountRepository.store(accounts)
          allAccounts <- AccountRepository.all
        } yield assertTrue(allAccounts.forall(_.name.value.value.startsWith("debasish")))
      }
    }.provideCustomLayer(AccountRepositoryInMemory.layer)
  )
}
