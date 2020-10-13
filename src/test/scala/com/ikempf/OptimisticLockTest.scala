package com.ikempf

import cats.Parallel
import cats.data.EitherT
import cats.effect.{IO, Resource}
import cats.implicits._
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.hlist.HNil
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import dev.profunktor.redis4cats.transactions.{RedisTransaction, TransactionDiscarded}
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class OptimisticLockTest extends AnyFlatSpec with Matchers {

  implicit private val CS = IO.contextShift(ExecutionContext.global)
  implicit private val T  = IO.timer(ExecutionContext.global)

  private val logger       = Slf4jLogger.getLogger[IO]
  implicit private val log = log4CatsInstance(logger)

  private val redisURI     = "redis://localhost:6379/2"
  private val mkRedis      = RedisClient[IO].from(redisURI)
  private val testKey      = "test-key"
  private val InitialValue = "a"
  private val UpdatedValue = "b"
  private val Parallelism  = 100

  private def commands(client: RedisClient): Resource[IO, RedisCommands[IO, String, String]] =
    Redis[IO].fromClient(client, RedisCodec.Ascii)

  "Optimistic lock" should "allow single update" in {
    mkRedis
      .use(client =>
        setupTestData(client)
          .productR(concurrentUpdates(client))
      )
      .map { results =>
        val (left, right) = results.separate
        left should have size (Parallelism - 1)
        right should have size 1
      }
      .unsafeRunSync()
  }

  private def setupTestData(client: RedisClient): IO[Unit] =
    commands(client).use(cmds => cmds.set(testKey, InitialValue))

  private def concurrentUpdates(client: RedisClient): IO[List[Either[String, Unit]]] =
    Parallel.parSequence(List.range(0, Parallelism).as(exclusiveUpdate(client)))

  private def exclusiveUpdate(client: RedisClient): IO[Either[String, Unit]] =
    commands(client).use { cmds =>
      val tx = RedisTransaction(cmds)
      EitherT
        .right[String](cmds.watch(testKey))
        .flatMapF(_ => assertCurrent(cmds))
        .flatMapF(_ =>
          tx.exec(cmds.set(testKey, UpdatedValue) :: HNil)
            .void
            .as(Either.right[String, Unit](()))
            .recover {
              case TransactionDiscarded => Left("Discarded")
            }
        )
        .value
    }

  private def assertCurrent(cmds: RedisCommands[IO, String, String]): IO[Either[String, Unit]] =
    cmds
      .get(testKey)
      .map {
        case Some(InitialValue) => Right(())
        case Some(UpdatedValue) => Left("Already updated")
        case Some(_)            => Left("Unexpected value")
        case None               => Right(())
      }

}
