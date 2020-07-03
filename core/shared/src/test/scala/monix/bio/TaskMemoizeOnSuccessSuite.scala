/*
 * Copyright (c) 2019-2020 by The Monix Project Developers.
 * See the project homepage at: https://monix.io
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

package monix.bio

import monix.execution.exceptions.DummyException
import monix.execution.internal.Platform

import scala.concurrent.Promise
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object TaskMemoizeOnSuccessSuite extends BaseTestSuite {
  test("Task.memoizeOnSuccess should work asynchronously for first subscriber") { implicit s =>
    var effect = 0
    val task = Task.evalAsync { effect += 1; effect }.memoizeOnSuccess
      .flatMap(Task.now)
      .flatMap(Task.now)

    val f = task.attempt.runToFuture
    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.memoizeOnSuccess should work synchronously for next subscribers") { implicit s =>
    var effect = 0
    val task = Task.evalAsync { effect += 1; effect }.memoizeOnSuccess
      .flatMap(Task.now)
      .flatMap(Task.now)

    task.attempt.runToFuture
    s.tick()

    val f1 = task.attempt.runToFuture
    assertEquals(f1.value, Some(Success(Right(1))))
    val f2 = task.attempt.runToFuture
    assertEquals(f2.value, Some(Success(Right(1))))
  }

  test("Task.memoizeOnSuccess should be stack safe") { implicit s =>
    val count = if (Platform.isJVM) 50000 else 5000
    var task = Task.evalAsync(1)
    for (i <- 0 until count) task = task.memoizeOnSuccess

    val f = task.attempt.runToFuture
    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.flatMap.memoizeOnSuccess should be stack safe, test 1") { implicit s =>
    val count = if (Platform.isJVM) 50000 else 5000
    var task = Task.evalAsync(1)

    for (i <- 0 until count) {
      task = task.memoizeOnSuccess.flatMap(x => Task.now(x))
    }

    val f = task.attempt.runToFuture
    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.flatMap.memoizeOnSuccess should be stack safe, test 2") { implicit s =>
    val count = if (Platform.isJVM) 50000 else 5000
    var task = Task.evalAsync(1)
    for (i <- 0 until count) {
      task = task.memoizeOnSuccess.flatMap(x => Task.evalAsync(x))
    }

    val f = task.attempt.runToFuture
    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.raiseError(error).memoizeOnSuccess should not be idempotent") { implicit s =>
    var effect = 0
    val dummy = "dummy"
    val task = Task
      .suspendTotal(Task.raiseError { effect += 1; dummy })
      .memoizeOnSuccess
      .flatMap(Task.now[Int])
      .flatMap(Task.now[Int])

    val f1 = task.attempt.runToFuture; s.tick()
    assertEquals(f1.value, Some(Success(Left(dummy))))
    assertEquals(effect, 1)

    val f2 = task.attempt.runToFuture; s.tick()
    assertEquals(f2.value, Some(Success(Left(dummy))))
    assertEquals(effect, 2)
  }

  test("Task.terminate(error).memoizeOnSuccess should not be idempotent") { implicit s =>
    var effect = 0
    val dummy = DummyException("dummy")
    val task = Task
      .suspendTotal(Task.terminate { effect += 1; dummy })
      .memoizeOnSuccess
      .flatMap(Task.now[Int])
      .flatMap(Task.now[Int])

    val f1 = task.runToFuture; s.tick()
    assertEquals(f1.value, Some(Failure(dummy)))
    assertEquals(effect, 1)

    val f2 = task.runToFuture; s.tick()
    assertEquals(f2.value, Some(Failure(dummy)))
    assertEquals(effect, 2)
  }

  test("Task.memoizeOnSuccess.materialize") { implicit s =>
    val f = Task.evalAsync(10).memoizeOnSuccess.materialize.attempt.runToFuture
    s.tick()
    assertEquals(f.value, Some(Success(Right(Success(10)))))
  }

  test("Task.raiseError(error).memoizeOnSuccess.materialize") { implicit s =>
    val dummy = DummyException("dummy")
    val f = Task.raiseError(dummy).memoizeOnSuccess.materialize.attempt.runToFuture
    s.tick()
    assertEquals(f.value, Some(Success(Right(Failure(dummy)))))
  }

  test("Task.terminate(error).memoizeOnSuccess.materialize") { implicit s =>
    val dummy = DummyException("dummy")
    val f = Task.terminate(dummy).memoizeOnSuccess.materialize.attempt.runToFuture
    s.tick()
    assertEquals(f.value, Some(Failure(dummy)))
  }

  test("Task.eval.memoizeOnSuccess should work for first subscriber") { implicit s =>
    var effect = 0
    val task = Task.eval { effect += 1; effect }.memoizeOnSuccess

    val f = task.attempt.runToFuture; s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.eval.memoizeOnSuccess should work synchronously for next subscribers") { implicit s =>
    var effect = 0
    val task = Task.eval { effect += 1; effect }.memoizeOnSuccess
    task.attempt.runToFuture
    s.tick()

    val f1 = task.attempt.runToFuture
    assertEquals(f1.value, Some(Success(Right(1))))
    val f2 = task.attempt.runToFuture
    assertEquals(f2.value, Some(Success(Right(1))))
  }

  test("Task.eval(error).memoizeOnSuccess should not be idempotent") { implicit s =>
    var effect = 0
    val dummy = DummyException("dummy")
    val task = Task.eval[Int] { effect += 1; throw dummy }.memoizeOnSuccess

    val f1 = task.attempt.runToFuture; s.tick()
    assertEquals(f1.value, Some(Success(Left(dummy))))
    assertEquals(effect, 1)

    val f2 = task.attempt.runToFuture; s.tick()
    assertEquals(f2.value, Some(Success(Left(dummy))))
    assertEquals(effect, 2)
  }

  test("Task.eval.memoizeOnSuccess") { implicit s =>
    var effect = 0
    val task = Task.eval { effect += 1; effect }.memoizeOnSuccess

    val r1 = task.attempt.runToFuture
    val r2 = task.attempt.runToFuture
    val r3 = task.attempt.runToFuture

    s.tickOne()
    assertEquals(r1.value, Some(Success(Right(1))))
    assertEquals(r2.value, Some(Success(Right(1))))
    assertEquals(r3.value, Some(Success(Right(1))))
  }

  test("Task.eval.memoizeOnSuccess should be stack safe") { implicit s =>
    val count = if (Platform.isJVM) 50000 else 5000
    var task = Task.eval(1)
    for (i <- 0 until count)
      task = task.memoizeOnSuccess

    val f = task.attempt.runToFuture; s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.eval.flatMap.memoizeOnSuccess should be stack safe") { implicit s =>
    val count = if (Platform.isJVM) 50000 else 5000
    var task = Task.eval(1)
    for (i <- 0 until count) {
      task = task.memoizeOnSuccess.flatMap(x => Task.eval(x))
    }

    val f = task.attempt.runToFuture
    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.defer(evalAlways).memoizeOnSuccess") { implicit s =>
    var effect = 0
    val task = Task.defer(Task.eval { effect += 1; effect }).memoizeOnSuccess

    val r1 = task.attempt.runToFuture
    val r2 = task.attempt.runToFuture
    val r3 = task.attempt.runToFuture

    s.tick()
    assertEquals(r1.value, Some(Success(Right(1))))
    assertEquals(r2.value, Some(Success(Right(1))))
    assertEquals(r3.value, Some(Success(Right(1))))
  }

  test("Task.evalOnce.memoizeOnSuccess should work for first subscriber") { implicit s =>
    var effect = 0
    val task = Task.evalOnce { effect += 1; effect }.memoizeOnSuccess

    val f = task.attempt.runToFuture; s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.evalOnce.memoizeOnSuccess should work synchronously for next subscribers") { implicit s =>
    var effect = 0
    val task = Task.evalOnce { effect += 1; effect }.memoizeOnSuccess
    task.attempt.runToFuture
    s.tick()

    val f1 = task.attempt.runToFuture
    assertEquals(f1.value, Some(Success(Right(1))))
    val f2 = task.attempt.runToFuture
    assertEquals(f2.value, Some(Success(Right(1))))
  }

  test("Task.evalOnce(error).memoizeOnSuccess should work") { implicit s =>
    var effect = 0
    val dummy = DummyException("dummy")
    val task = Task.evalOnce[Int] { effect += 1; throw dummy }.memoizeOnSuccess

    val f1 = task.attempt.runToFuture; s.tick()
    assertEquals(f1.value, Some(Success(Left(dummy))))
    assertEquals(effect, 1)

    val f2 = task.attempt.runToFuture; s.tick()
    assertEquals(f2.value, Some(Success(Left(dummy))))
    assertEquals(effect, 1)
  }

  test("Task.evalOnce.memoizeOnSuccess should be stack safe") { implicit s =>
    val count = if (Platform.isJVM) 50000 else 5000
    var task = Task.eval(1)
    for (i <- 0 until count) {
      task = task.memoizeOnSuccess
    }

    val f = task.attempt.runToFuture; s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.evalOnce.flatMap.memoizeOnSuccess should be stack safe") { implicit s =>
    val count = if (Platform.isJVM) 50000 else 5000
    var task = Task.eval(1)
    for (i <- 0 until count) {
      task = task.memoizeOnSuccess.flatMap(x => Task.evalOnce(x))
    }

    val f = task.attempt.runToFuture
    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.now.memoizeOnSuccess should work synchronously for first subscriber") { implicit s =>
    var effect = 0
    val task = Task.now { effect += 1; effect }.memoizeOnSuccess

    val f = task.attempt.runToFuture
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.now.memoizeOnSuccess should work synchronously for next subscribers") { implicit s =>
    var effect = 0
    val task = Task.now { effect += 1; effect }.memoizeOnSuccess

    task.attempt.runToFuture
    s.tick()

    val f1 = task.attempt.runToFuture
    assertEquals(f1.value, Some(Success(Right(1))))
    val f2 = task.attempt.runToFuture
    assertEquals(f2.value, Some(Success(Right(1))))
  }

  test("Task.raiseError.memoizeOnSuccess should work") { implicit s =>
    val dummy = "dummy"
    val task = Task.raiseError(dummy).memoizeOnSuccess

    val f1 = task.attempt.runToFuture
    assertEquals(f1.value, Some(Success(Left(dummy))))
    val f2 = task.attempt.runToFuture
    assertEquals(f2.value, Some(Success(Left(dummy))))
  }

  test("Task.terminate.memoizeOnSuccess should work") { implicit s =>
    val dummy = DummyException("dummy")
    val task = Task.terminate(dummy).memoizeOnSuccess

    val f1 = task.runToFuture
    assertEquals(f1.value, Some(Failure(dummy)))
    val f2 = task.runToFuture
    assertEquals(f2.value, Some(Failure(dummy)))
  }

  test("Task.now.memoizeOnSuccess should be stack safe") { implicit s =>
    val count = if (Platform.isJVM) 50000 else 5000
    var task = Task.now(1)
    for (i <- 0 until count) {
      task = task.memoizeOnSuccess
    }

    val f = task.attempt.runToFuture
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.now.flatMap.memoizeOnSuccess should be stack safe") { implicit s =>
    val count = if (Platform.isJVM) 50000 else 5000
    var task = Task.now(1)
    for (i <- 0 until count) {
      task = task.memoizeOnSuccess.flatMap(x => Task.now(x))
    }

    val f = task.attempt.runToFuture
    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.suspend.memoizeOnSuccess should be stack safe") { implicit s =>
    val count = if (Platform.isJVM) 50000 else 5000
    var task = Task.defer(Task.now(1))
    for (i <- 0 until count) {
      task = task.memoizeOnSuccess.map(x => x)
    }

    val f = task.attempt.runToFuture; s.tick()
    assertEquals(f.value, Some(Success(Right(1))))
  }

  test("Task.memoizeOnSuccess effects, sequential") { implicit s =>
    var effect = 0
    val task1 = Task.evalAsync { effect += 1; 3 }.memoizeOnSuccess
    val task2 = task1.map { x =>
      effect += 1; x + 1
    }

    val result1 = task2.attempt.runToFuture; s.tick()
    assertEquals(effect, 2)
    assertEquals(result1.value, Some(Success(Right(4))))

    val result2 = task2.attempt.runToFuture; s.tick()
    assertEquals(effect, 3)
    assertEquals(result2.value, Some(Success(Right(4))))
  }

  test("Task.memoizeOnSuccess effects, parallel") { implicit s =>
    var effect = 0
    val task1 = Task.evalAsync { effect += 1; 3 }.memoizeOnSuccess
    val task2 = task1.map { x =>
      effect += 1; x + 1
    }

    val result1 = task2.attempt.runToFuture
    val result2 = task2.attempt.runToFuture

    assertEquals(result1.value, None)
    assertEquals(result2.value, None)

    s.tick()
    assertEquals(effect, 3)
    assertEquals(result1.value, Some(Success(Right(4))))
    assertEquals(result2.value, Some(Success(Right(4))))
  }

  test("Task.suspend.memoizeOnSuccess effects") { implicit s =>
    var effect = 0
    val task1 = Task.defer { effect += 1; Task.now(3) }.memoizeOnSuccess
    val task2 = task1.map { x =>
      effect += 1; x + 1
    }

    val result1 = task2.attempt.runToFuture; s.tick()
    assertEquals(effect, 2)
    assertEquals(result1.value, Some(Success(Right(4))))

    val result2 = task2.attempt.runToFuture; s.tick()
    assertEquals(effect, 3)
    assertEquals(result2.value, Some(Success(Right(4))))
  }

  test("Task.suspend.flatMap.memoizeOnSuccess effects") { implicit s =>
    var effect = 0
    val task1 = Task.defer { effect += 1; Task.now(2) }
      .flatMap(x => Task.now(x + 1))
      .memoizeOnSuccess
    val task2 = task1.map { x =>
      effect += 1; x + 1
    }

    val result1 = task2.attempt.runToFuture; s.tick()
    assertEquals(effect, 2)
    assertEquals(result1.value, Some(Success(Right(4))))

    val result2 = task2.attempt.runToFuture; s.tick()
    assertEquals(effect, 3)
    assertEquals(result2.value, Some(Success(Right(4))))

    val result3 = task2.attempt.runToFuture; s.tick()
    assertEquals(effect, 4)
    assertEquals(result3.value, Some(Success(Right(4))))
  }

  test("Task.memoizeOnSuccess should make subsequent subscribers wait for the result, as future") { implicit s =>
    var effect = 0
    val task = Task.evalAsync { effect += 1; effect }.delayExecution(1.second).map(_ + 1).memoizeOnSuccess

    val first = task.attempt.runToFuture
    s.tick()
    assertEquals(first.value, None)

    val second = task.attempt.runToFuture
    val third = task.attempt.runToFuture

    s.tick()
    assertEquals(second.value, None)
    assertEquals(third.value, None)

    s.tick(1.second)
    assertEquals(first.value, Some(Success(Right(2))))
    assertEquals(second.value, Some(Success(Right(2))))
    assertEquals(third.value, Some(Success(Right(2))))
  }

  test("Task.memoizeOnSuccess should make subsequent subscribers wait for the result, as callback") { implicit s =>
    var effect = 0
    val task = Task.evalAsync { effect += 1; effect }.delayExecution(1.second).map(_ + 1).memoizeOnSuccess

    val first = Promise[Either[Throwable, Int]]()
    task.runAsync(BiCallback.fromPromise(first))

    s.tick()
    assertEquals(first.future.value, None)

    val second = Promise[Either[Throwable, Int]]()
    task.runAsync(BiCallback.fromPromise(second))
    val third = Promise[Either[Throwable, Int]]()
    task.runAsync(BiCallback.fromPromise(third))

    s.tick()
    assertEquals(second.future.value, None)
    assertEquals(third.future.value, None)

    s.tick(1.second)
    assertEquals(first.future.value, Some(Success(Right(2))))
    assertEquals(second.future.value, Some(Success(Right(2))))
    assertEquals(third.future.value, Some(Success(Right(2))))
  }

  test("Task.memoizeOnSuccess should be synchronous for subsequent subscribers, as callback") { implicit s =>
    var effect = 0
    val task = Task.evalAsync { effect += 1; effect }.delayExecution(1.second).map(_ + 1).memoizeOnSuccess

    val first = Promise[Either[Throwable, Int]]()
    task.runAsync(BiCallback.fromPromise(first))

    s.tick()
    assertEquals(first.future.value, None)

    s.tick(1.second)
    assertEquals(first.future.value, Some(Success(Right(2))))

    val second = Promise[Either[Throwable, Int]]()
    task.runAsync(BiCallback.fromPromise(second))
    val third = Promise[Either[Throwable, Int]]()
    task.runAsync(BiCallback.fromPromise(third))
    assertEquals(second.future.value, Some(Success(Right(2))))
    assertEquals(third.future.value, Some(Success(Right(2))))
  }

  test("Task.memoizeOnSuccess should be cancellable (future)") { implicit s =>
    var effect = 0
    val task = Task.evalAsync { effect += 1; effect }.delayExecution(1.second).map(_ + 1).memoizeOnSuccess

    val first = task.attempt.runToFuture
    s.tick()
    assertEquals(first.value, None)

    val second = task.attempt.runToFuture
    val third = task.attempt.runToFuture

    s.tick()
    assertEquals(second.value, None)
    assertEquals(third.value, None)

    third.cancel()
    s.tick()
    assert(s.state.tasks.nonEmpty, "tasks.nonEmpty")

    s.tick(1.second)
    assertEquals(first.value, Some(Success(Right(2))))
    assertEquals(second.value, Some(Success(Right(2))))
    assertEquals(third.value, None)
    assertEquals(effect, 1)
  }

  test("Task.memoizeOnSuccess should be cancellable (callback #1)") { implicit s =>
    var effect = 0
    val task = Task.evalAsync { effect += 1; effect }.delayExecution(1.second).map(_ + 1).memoizeOnSuccess

    val first = Promise[Either[Throwable, Int]]()
    task.runAsync(BiCallback.fromPromise(first))

    s.tick()
    assertEquals(first.future.value, None)

    val second = Promise[Either[Throwable, Int]]()
    val c2 = task.runAsync(BiCallback.fromPromise(second))
    val third = Promise[Either[Throwable, Int]]()
    val c3 = task.runAsync(BiCallback.fromPromise(third))

    s.tick()
    assertEquals(second.future.value, None)
    assertEquals(third.future.value, None)

    c3.cancel()
    s.tick()
    assert(s.state.tasks.nonEmpty, "tasks.nonEmpty")

    s.tick(1.second)
    assertEquals(first.future.value, Some(Success(Right(2))))
    assertEquals(second.future.value, Some(Success(Right(2))))
    assertEquals(third.future.value, None)
    assertEquals(effect, 1)
  }

  test("Task.memoizeOnSuccess should be cancellable (callback #2)") { implicit s =>
    var effect = 0
    val task = Task.evalAsync { effect += 1; effect }.delayExecution(1.second).map(_ + 1).memoizeOnSuccess.map(x => x)

    val first = Promise[Either[Throwable, Int]]()
    task.runAsync(BiCallback.fromPromise(first))

    s.tick()
    assertEquals(first.future.value, None)

    val second = Promise[Either[Throwable, Int]]()
    task.runAsync(BiCallback.fromPromise(second))
    val third = Promise[Either[Throwable, Int]]()
    val c3 = task.runAsync(BiCallback.fromPromise(third))

    s.tick()
    assertEquals(second.future.value, None)
    assertEquals(third.future.value, None)

    c3.cancel()
    s.tick()
    assert(s.state.tasks.nonEmpty, "tasks.nonEmpty")

    s.tick(1.second)
    assertEquals(first.future.value, Some(Success(Right(2))))
    assertEquals(second.future.value, Some(Success(Right(2))))
    assertEquals(third.future.value, None)
    assertEquals(effect, 1)
  }

  test("Task.memoizeOnSuccess should not be cancelable") { implicit s =>
    var effect = 0
    val task = Task.evalAsync { effect += 1; effect }.delayExecution(1.second).map(_ + 1).memoizeOnSuccess
    val first = task.attempt.runToFuture
    val second = task.attempt.runToFuture

    s.tick()
    assertEquals(first.value, None)
    assertEquals(second.value, None)
    first.cancel()

    s.tick()
    assert(s.state.tasks.nonEmpty, "tasks.nonEmpty")
    assertEquals(first.value, None)
    assertEquals(second.value, None)
    assertEquals(effect, 0)

    // -- Second wave:
    val third = task.attempt.runToFuture
    val fourth = task.attempt.runToFuture

    s.tick(1.second)
    assertEquals(first.value, None)
    assertEquals(second.value, Some(Success(Right(2))))
    assertEquals(third.value, Some(Success(Right(2))))
    assertEquals(fourth.value, Some(Success(Right(2))))
    assert(s.state.tasks.isEmpty, "tasks.isEmpty")
  }

  test("Task.evalAsync(error).memoizeOnSuccess can register multiple listeners") { implicit s =>
    val dummy = DummyException("dummy")
    var effect = 0

    val task = Task[Int] { effect += 1; throw dummy }.delayExecution(1.second).map(_ + 1).memoizeOnSuccess

    val first = task.attempt.runToFuture
    s.tick()
    assertEquals(first.value, None)
    assertEquals(effect, 0)

    val second = task.attempt.runToFuture
    val third = task.attempt.runToFuture

    s.tick()
    assertEquals(second.value, None)
    assertEquals(third.value, None)

    s.tick(1.second)
    assertEquals(first.value, Some(Success(Left(dummy))))
    assertEquals(second.value, Some(Success(Left(dummy))))
    assertEquals(third.value, Some(Success(Left(dummy))))
    assertEquals(effect, 1)

    val fourth = task.attempt.runToFuture
    val fifth = task.attempt.runToFuture
    s.tick()
    assertEquals(fourth.value, None)
    assertEquals(fifth.value, None)

    s.tick(1.second)
    assertEquals(fourth.value, Some(Success(Left(dummy))))
    assertEquals(fifth.value, Some(Success(Left(dummy))))
    assertEquals(effect, 2)
  }

  test("Task.evalOnce eq Task.evalOnce.memoizeOnSuccess") { implicit s =>
    val task = Task.evalOnce(1)
    assertEquals(task, task.memoizeOnSuccess)
  }

  test("Task.eval.memoizeOnSuccess eq Task.eval.memoizeOnSuccess.memoizeOnSuccess") { implicit s =>
    val task = Task.eval(1).memoizeOnSuccess
    assertEquals(task, task.memoizeOnSuccess)
  }

  test("Task.eval.memoize eq Task.eval.memoize.memoizeOnSuccess") { implicit s =>
    val task = Task.eval(1).memoize
    assertEquals(task, task.memoizeOnSuccess)
  }

  test("Task.eval.map.memoize eq Task.eval.map.memoize.memoizeOnSuccess") { implicit s =>
    val task = Task.eval(1).map(_ + 1).memoize
    assertEquals(task, task.memoizeOnSuccess)
  }

  test("Task.now.memoizeOnSuccess eq Task.now") { implicit s =>
    val task = Task.now(1)
    assertEquals(task, task.memoizeOnSuccess)
  }

  test("Task.raiseError.memoizeOnSuccess eq Task.raiseError") { implicit s =>
    val task = Task.raiseError("dummy")
    assertEquals(task, task.memoizeOnSuccess)
  }

  test("Task.terminate.memoizeOnSuccess eq Task.terminate") { implicit s =>
    val task = Task.terminate(DummyException("dummy"))
    assertEquals(task, task.memoizeOnSuccess)
  }
}