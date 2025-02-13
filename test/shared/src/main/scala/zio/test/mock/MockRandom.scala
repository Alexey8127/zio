/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio.test.mock

import zio.{ Chunk, UIO }
import zio.random.Random

trait MockRandom extends Random {

  val random: MockRandom.Service[Any]
}

object MockRandom {

  trait Service[R] extends Random.Service[R]

  object Service {
    object nextBoolean  extends Method[Unit, Boolean]
    object nextBytes    extends Method[Int, Chunk[Byte]]
    object nextDouble   extends Method[Unit, Double]
    object nextFloat    extends Method[Unit, Float]
    object nextGaussian extends Method[Unit, Double]
    object nextInt {
      object _0 extends Method[Int, Int]
      object _1 extends Method[Unit, Int]
    }
    object nextLong {
      object _0 extends Method[Unit, Long]
      object _1 extends Method[Long, Long]
    }
    object nextPrintableChar extends Method[Unit, Char]
    object nextString        extends Method[Int, String]
    object shuffle           extends Method[List[Any], List[Any]]
  }

  implicit val mockable: Mockable[MockRandom] = (mock: Mock) =>
    new MockRandom {
      val random = new Service[Any] {
        val nextBoolean: UIO[Boolean]                = mock(Service.nextBoolean)
        def nextBytes(length: Int): UIO[Chunk[Byte]] = mock(Service.nextBytes, length)
        val nextDouble: UIO[Double]                  = mock(Service.nextDouble)
        val nextFloat: UIO[Float]                    = mock(Service.nextFloat)
        val nextGaussian: UIO[Double]                = mock(Service.nextGaussian)
        def nextInt(n: Int): UIO[Int]                = mock(Service.nextInt._0, n)
        val nextInt: UIO[Int]                        = mock(Service.nextInt._1)
        val nextLong: UIO[Long]                      = mock(Service.nextLong._0)
        def nextLong(n: Long): UIO[Long]             = mock(Service.nextLong._1, n)
        val nextPrintableChar: UIO[Char]             = mock(Service.nextPrintableChar)
        def nextString(length: Int)                  = mock(Service.nextString, length)
        def shuffle[A](list: List[A]): UIO[List[A]]  = mock(Service.shuffle, list).asInstanceOf[UIO[List[A]]]
      }
    }
}
