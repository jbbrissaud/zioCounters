package example

import zio._
import zio.stm._

trait Gate:
  def lock(): UIO[Unit]

  def unlock(): UIO[Unit]

  def waitIfLocked(): UIO[Unit]


object Gate:
  def apply(signal: TRef[Boolean]): Gate = {
    new Gate {
      override def lock(): UIO[Unit] =
        signal.set(false).commit

      override def unlock(): UIO[Unit] =
        signal.set(true).commit

      override def waitIfLocked(): UIO[Unit] =
        STM.atomically {
          signal.get.flatMap(STM.check(_))
        }
    }
  }


