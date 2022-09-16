package example

import com.raquo.laminar.api.L._
import org.scalajs.dom

import zio.*
import zio.stm._

val myRuntime = Runtime.default.unsafe

implicit class RunSyntax[E,A](io: ZIO[Any, E, A]):
  // My thanks to Adam Fraser for the code
  def unsafeRun: A =
    Unsafe.unsafeCompat { implicit u =>
      myRuntime.run(io).getOrThrowFiberFailure()
    }

object App:
  def myComponent =
    val signal:TRef[Boolean] = TRef.make(true).commit.unsafeRun
    val gate = Gate(signal)
    val fiberOptRef: zio.Ref[Option[Fiber.Runtime[Nothing,Unit]]] = zio.Ref.make(None).unsafeRun
    val content = Var("init1")
    def zioFork(zio1:ZIO[Any,Nothing,Unit]):Fiber.Runtime[Nothing,Unit] =
      Unsafe.unsafeCompat { implicit u =>
        myRuntime.fork((zio1))
      }
    def loop(n:Int,d:Duration):ZIO[Any,Nothing,Unit] =
      for
        _ <- ZIO.succeed(content.set(n.toString()))
        _ <- ZIO.sleep(d)
        _ <- gate.waitIfLocked()
        _ <- loop(n+1,d)
      yield ()
    def interrupt(): ZIO[Any,Nothing,Unit] =
      fiberOptRef.get.flatMap{
          case None => ZIO.succeed(())
          case Some(fiber) => fiber.interrupt.map(_ => ())
      }
    val zio1 = loop(10,1.second)
    val zio2 = zio1.timeout(10.second).as(())
    def doStart(clickEvent:Any): Unit = 
      val fiber = zioFork(zio2)
      fiberOptRef.set(Some(fiber)).unsafeRun
    def doStop(clickEvent:Any): Unit = 
      zioFork(interrupt())
    def doPause(clickEvent:Any): Unit = 
      zioFork(gate.lock())
    def doResume(clickEvent:Any): Unit = 
      zioFork(gate.unlock())
    div(
      button(
        "click me",
        onClick --> doStart
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
