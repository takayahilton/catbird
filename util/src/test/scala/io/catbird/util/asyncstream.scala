package io.catbird
package util

import cats.Eq
import cats.instances.int._
import cats.instances.tuple._
import cats.kernel.laws.discipline.SemigroupTests
import cats.laws.discipline._
import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.time._
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class AsyncStreamSuite extends FunSuite with Discipline with AsyncStreamInstances with ArbitraryInstances {

  implicit val eqAsyncStreamInt: Eq[AsyncStream[Int]] = asyncStreamEq(1.second)
  implicit val eqAsyncStreamAsyncStreamInt: Eq[AsyncStream[AsyncStream[Int]]] = asyncStreamEq(1.second)
  implicit val eqAsyncStreamIntIntInt: Eq[AsyncStream[(Int,Int,Int)]] = asyncStreamEq[(Int,Int,Int)](1.second) 

  checkAll("AsyncStream[Int]", MonadTests[AsyncStream].monad[Int, Int, Int])
  checkAll("AsyncStream[Int]", SemigroupTests[AsyncStream[Int]](asyncStreamSemigroup[Int]).semigroup)
  
}