package tradex.domain
package repository

import java.time.{ LocalDate, LocalDateTime }
import squants.market._
import zio._
import zio.prelude._
import zio.blocking.Blocking
import zio.interop.catz._

import doobie._
import doobie.hikari._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._

import config._
import model.order._
import model.account._
import model.instrument._

final class DoobieOrderRepository(xa: Transactor[Task]) {
  import DoobieOrderRepository._

  implicit val OrderAssociative: Associative[Order] =
    new Associative[Order] {
      def combine(left: => Order, right: => Order): Order =
        Order(left.no, left.date, left.accountNo, left.items ++ right.items)
    }

  val orderRepository: OrderRepository.Service = new OrderRepository.Service {
    def queryByOrderNo(no: OrderNo): Task[Option[Order]] = {
      SQL
        .getByOrderNo(no.value.value)
        .to[List]
        .map(_.groupBy(_.no))
        .map {
          _.map { case (_, lis) =>
            lis.reduce((o1, o2) => Associative[Order].combine(o1, o2))
          }.headOption
        }
        .transact(xa)
        .orDie
    }
    def queryByOrderDate(date: LocalDate): Task[List[Order]] = {
      SQL
        .getByOrderDate(date)
        .to[List]
        .map(_.groupBy(_.no))
        .map {
          _.map { case (_, lis) =>
            lis.reduce((o1, o2) => Associative[Order].combine(o1, o2))
          }.toList
        }
        .transact(xa)
        .orDie
    }
    def store(ord: Order): Task[Order]                 = ???
    def store(orders: NonEmptyList[Order]): Task[Unit] = ???
  }
}

object DoobieOrderRepository {
  object SQL {

    implicit val orderLineItemRead: Read[Order] =
      Read[
        (
            String,
            LocalDateTime,
            String,
            String,
            BigDecimal,
            BigDecimal,
            String
        )
      ].map { case (no, dt, ano, isin, qty, up, bs) =>
        val vorder = for {
          lineItems <- Order.makeLineItem(no, isin, qty, up, bs)
          order     <- Order.makeOrder(no, dt, ano, NonEmptyList(lineItems))
        } yield order
        vorder
          .fold(exs => throw new Exception(exs.toList.mkString("/")), identity)
      }

    def getByOrderNo(orderNo: String): Query0[Order] =
      sql"""
        SELECT o.no, o.dateOfOrder, o.accountNo, l.isinCode, l.quantity, l.unitPrice, l.buySellFlag
        FROM orders o, lineItems l
        WHERE o.no = $orderNo
        AND   o.no = l.orderNo
       """.query[Order]

    def getByOrderDate(orderDate: LocalDate): Query0[Order] =
      sql"""
        SELECT o.no, o.dateOfOrder, o.accountNo, l.isinCode, l.quantity, l.unitPrice, l.buySellFlag
        FROM orders o, lineItems l
        WHERE Date(o.dateOfOrder) = $orderDate
        AND   o.no = l.orderNo
       """.query[Order]
  }
}
