package zio.test

import zio.random.Random
import zio.test.Assertion._
import zio.test.BoolAlgebraSpecHelper._

object BoolAlgebraSpec
    extends ZIOBaseSpec(
      suite("BoolAlgebraSpec")(
        test("all returns conjunction of values") {
          assert(BoolAlgebra.all(List(success1, failure1, failure2)), isSome(isFailure))
        },
        testM("and distributes over or") {
          zio.test.check(boolAlgebra, boolAlgebra, boolAlgebra) { (a, b, c) =>
            assert(a && (b || c), equalTo((a && b) || (a && c)))
          }
        },
        testM("and is associative") {
          zio.test.check(boolAlgebra, boolAlgebra, boolAlgebra) { (a, b, c) =>
            assert((a && b) && c, equalTo(a && (b && c)))
          }
        },
        testM("and is commutative") {
          zio.test.check(boolAlgebra, boolAlgebra) { (a, b) =>
            assert(a && b, equalTo(b && a))
          }
        },
        test("any returns disjunction of values") {
          assert(BoolAlgebra.any(List(success1, failure1, failure2)), isSome(isSuccess))
        },
        test("as maps values to constant value") {
          assert(
            (success1 && success2).as("value"),
            equalTo(BoolAlgebra.success("value") && BoolAlgebra.success("value"))
          )
        },
        test("both returns conjunction of two values") {
          assert(success1 && success2, isSuccess) &&
          assert(success1 && failure1, isFailure) &&
          assert(failure1 && success1, isFailure) &&
          assert(failure1 && failure2, isFailure)
        },
        test("collectAll combines multiple values") {
          assert(
            BoolAlgebra.collectAll(List(success1, failure1, failure2)),
            isSome(equalTo(success1 && failure1 && failure2))
          )
        },
        testM("De Morgan's laws") {
          zio.test.check(boolAlgebra, boolAlgebra) { (a, b) =>
            assert(!(a && b), equalTo(!a || !b)) &&
            assert(!a || !b, equalTo(!(a && b))) &&
            assert(!(a || b), equalTo(!a && !b)) &&
            assert(!a && !b, equalTo(!(a || b)))
          }
        },
        testM("double negative") {
          zio.test.check(boolAlgebra) { a =>
            assert(!(!a), equalTo(a)) &&
            assert(a, equalTo(!(!a)))
          }
        },
        test("either returns disjunction of two values") {
          assert(success1 || success2, isSuccess) &&
          assert(success1 || failure1, isSuccess) &&
          assert(failure1 || success1, isSuccess) &&
          assert(failure1 || failure2, isFailure)
        },
        testM("hashCode is consistent with equals") {
          zio.test.checkSome(equalBoolAlgebraOfSize(4))(n = 10) { pair =>
            val (a, b) = pair
            assert(a.hashCode, equalTo(b.hashCode))
          }
        },
        test("failures collects failures") {
          val actual   = (success1 && success2 && failure1 && failure2).failures.get
          val expected = !failure1 && !failure2
          assert(actual, equalTo(expected))
        },
        test("foreach combines multiple values") {
          def isEven(n: Int): BoolAlgebra[String] =
            if (n % 2 == 0) BoolAlgebra.success(s"$n is even")
            else BoolAlgebra.failure(s"$n is odd")

          val actual = BoolAlgebra.foreach(List(1, 2, 3))(isEven)
          val expected = BoolAlgebra.failure("1 is odd") &&
            BoolAlgebra.success("2 is even") &&
            BoolAlgebra.failure("3 is odd")

          assert(actual, isSome(equalTo(expected)))
        },
        test("implies returns implication of two values") {
          assert(success1 ==> success2, isSuccess) &&
          assert(success1 ==> failure1, isFailure) &&
          assert(failure1 ==> success1, isSuccess) &&
          assert(failure1 ==> failure2, isSuccess)
        },
        test("isFailure returns whether result is failure") {
          assert(!success1.isFailure && failure1.isFailure, isTrue)
        },
        test("isSuccess returns whether result is success") {
          assert(success1.isSuccess && !failure1.isSuccess, isTrue)
        },
        test("map transforms values") {
          val actual   = (success1 && failure1 && failure2).map(_.split(" ").head)
          val expected = BoolAlgebra.success("first") && BoolAlgebra.failure("first") && BoolAlgebra.failure("second")
          assert(actual, equalTo(expected))
        },
        testM("or distributes over and") {
          zio.test.check(boolAlgebra, boolAlgebra, boolAlgebra) { (a, b, c) =>
            val left  = a || (b && c)
            val right = (a || b) && (a || c)
            assert(left, equalTo(right))
          }
        },
        testM("or is associative") {
          zio.test.check(boolAlgebra, boolAlgebra, boolAlgebra) { (a, b, c) =>
            val left  = (a || b) || c
            val right = a || (b || c)
            assert(left, equalTo(right))
          }
        },
        testM("or is commutative") {
          zio.test.check(boolAlgebra, boolAlgebra) { (a, b) =>
            assert(a || b, equalTo(b || a))
          }
        }
      )
    )

object BoolAlgebraSpecHelper {
  val value1 = "first success"
  val value2 = "second success"
  val value3 = "first failure"
  val value4 = "second failure"

  val success1 = BoolAlgebra.success(value1)
  val success2 = BoolAlgebra.success(value2)
  val failure1 = BoolAlgebra.failure(value3)
  val failure2 = BoolAlgebra.failure(value4)

  val isSuccess: Assertion[BoolAlgebra[_]] = assertion("isSuccess")()(_.isSuccess)
  val isFailure: Assertion[BoolAlgebra[_]] = assertion("isFailure")()(_.isFailure)

  def boolAlgebra: Gen[Random with Sized, BoolAlgebra[Int]] = Gen.small(s => boolAlgebraOfSize(s), 1)

  def boolAlgebraOfSize(size: Int): Gen[Random, BoolAlgebra[Int]] =
    if (size == 1) {
      Gen.int(0, 9).map(BoolAlgebra.success)
    } else if (size == 2) {
      boolAlgebraOfSize(size - 1).map(!_)
    } else {
      for {
        n <- Gen.int(1, size - 2)
        gen <- Gen.oneOf(
                (boolAlgebraOfSize(n) <*> boolAlgebraOfSize(size - n - 1)).map(p => p._1 && p._2),
                (boolAlgebraOfSize(n) <*> boolAlgebraOfSize(size - n - 1)).map(p => p._1 || p._2),
                boolAlgebraOfSize(size - 1).map(!_)
              )
      } yield gen
    }

  def equalBoolAlgebraOfSize(size: Int): Gen[Random, (BoolAlgebra[Int], BoolAlgebra[Int])] =
    for {
      a <- boolAlgebraOfSize(size)
      b <- boolAlgebraOfSize(size)
      if a == b
    } yield (a, b)
}
