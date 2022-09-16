package example

import com.raquo.laminar.api.L._
import org.scalajs.dom

import zio.*
import zio.stm._

val myRuntime = Runtime.default.unsafe

/*implicit class RunSyntax[E,A](io: ZIO[Any, E, A]):
  // My thanks to Adam Fraser for the code
  def unsafeRun: A =
    Unsafe.unsafeCompat { implicit u =>
      myRuntime.run(io).getOrThrowFiberFailure()
    }*/

def zioRun[E,A](zio1:ZIO[Any,E,A]): A =
  Unsafe.unsafeCompat { implicit u =>
    myRuntime.run(zio1).getOrThrowFiberFailure()
  }

def zioFork[E,A](zio1:ZIO[Any,E,A]):Fiber.Runtime[E,A] =
  Unsafe.unsafeCompat { implicit u =>
    myRuntime.fork((zio1))
  }

object App:
  def myComponent =
    val gate = 
      val signal:TRef[Boolean] = zioRun(TRef.make(true).commit)
      Gate(signal)
    val ref: zio.Ref[Option[Fiber.Runtime[Nothing,Unit]]] = zioRun(zio.Ref.make(None))
    def interrupt(): ZIO[Any,Nothing,Unit] =
      ref.get.flatMap{
          case None => ZIO.succeed(())
          case Some(fiber) => fiber.interrupt.map(_ => ())
      }
    def lock(): UIO[Unit] = gate.lock()
    def unlock(): UIO[Unit] = gate.unlock()
    def waitIfLocked(): UIO[Unit] = gate.waitIfLocked()

    val content = Var("init1")
    def loop(n:Int,d:Duration):ZIO[Any,Nothing,Unit] =
      for
        _ <- ZIO.succeed(content.set(n.toString()))
        _ <- ZIO.sleep(d)
        _ <- waitIfLocked()
        _ <- loop(n+1,d)
      yield ()
    val zio1 = loop(10,100.millisecond)
    val zio2 = zio1.timeout(30.second).as(())
    def doStart(zio1:ZIO[Any,Nothing,Unit])(clickEvent:Any): Unit = 
      val fiber = zioFork(interrupt() *> unlock() *> zio1)
      zioRun(ref.set(Some(fiber)))
    def doStop(clickEvent:Any): Unit = 
      zioFork(interrupt() *> unlock())
    def doPause(clickEvent:Any): Unit = 
      zioRun(lock())
    def doResume(clickEvent:Any): Unit = 
      zioRun(unlock())
    div(
      button(
        "start",
        onClick --> doStart(zio2)
        ),
      button(
        "stop",
        onClick --> doStop
      ),
      button(
        "pause",
        onClick --> doPause
      ),
      button(
        "resume",
        onClick --> doResume
      ),
      textArea(
        child.text <-- content.signal
      ),
    )

  def appComponent = 
    div(
      myComponent,
      myComponent
    )

  def main(args: Array[String]): Unit =
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _            = render(appContainer, appComponent)
    }(unsafeWindowOwner)
