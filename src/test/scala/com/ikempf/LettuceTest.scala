package com.ikempf

import java.util.concurrent.{CompletableFuture, CompletionStage}

import io.lettuce.core.RedisClient
import io.lettuce.core.api.async.RedisAsyncCommands
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LettuceTest extends AnyFlatSpec with Matchers {

  private val redisClient: RedisClient = RedisClient.create("redis://localhost:6379/3");

  private val testKey      = "lettuce-test-key"
  private val InitialValue = "lettuce-initial"
  private val UpdatedValue = "lettuce-updated"
  private val Parallelism  = 100

  "foo" should "bar" in {
    // Given
    commands(_.set(testKey, InitialValue)).get()

    // When
    val updates = Array.fill(Parallelism)(exclusiveUpdate)
    val result =
      CompletableFuture
        .allOf(updates: _*)
        .thenApply(_ => updates.map(_.get()))
        .get

    // Then
    result.count(_ != "null") should equal(Parallelism - 1)
  }

  private def exclusiveUpdate: CompletableFuture[String] =
    commands(cmds =>
      cmds
        .watch(testKey)
        .thenCompose(_ => cmds.get(testKey))
        .thenCompose(value =>
          if (value == InitialValue)
            cmds
              .multi()
              .thenAccept(_ => cmds.set(testKey, UpdatedValue))
              .thenCompose(_ => cmds.exec())
              .thenApply(result =>
                if (result.wasDiscarded())
                  "Discarded"
                else
                  "null"
              )
          else {
            val value1 = new CompletableFuture[String]()
            value1.complete("Already updated")
            value1
          }
        )
    )

  def commands[A](block: RedisAsyncCommands[String, String] => CompletionStage[A]): CompletableFuture[A] =
    use(redisClient)(block)

  def use[A](
    client: RedisClient
  )(block: RedisAsyncCommands[String, String] => CompletionStage[A]): CompletableFuture[A] = {
    val connection = client.connect()
    block(connection.async())
      .whenComplete((_, _) => connection.close())
      .toCompletableFuture
  }

}
