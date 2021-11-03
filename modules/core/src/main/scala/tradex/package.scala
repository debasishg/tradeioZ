package tradex

import java.time.LocalDateTime

import squants.market._

import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.auto._

package object domain {

  def today                  = LocalDateTime.now
  final val ZERO_BIG_DECIMAL = BigDecimal(0)

  implicit val moneyContext = defaultMoneyContext

  object NewtypeRefinedOps {
    import io.estatico.newtype.Coercible
    import io.estatico.newtype.ops._

    final class NewtypeRefinedPartiallyApplied[A] {
      def apply[T, P](raw: T)(implicit
          c: Coercible[Refined[T, P], A],
          v: Validate[T, P]
      ): Option[A] =
        refineV[P](raw).toOption.map(_.coerce[A])
    }

    def validate[A]: NewtypeRefinedPartiallyApplied[A] =
      new NewtypeRefinedPartiallyApplied[A]
  }
}
