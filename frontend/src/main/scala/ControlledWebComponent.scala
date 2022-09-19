
package example

import com.raquo.laminar.api.L._
import com.raquo.laminar.api.L
import org.scalajs.dom

import zio.*
import zio.stm._
import org.w3c.dom.events.MouseEvent
import com.raquo.laminar.nodes.ReactiveHtmlElement

////////////////////////////////////////////// Utils

val myRuntime = Runtime.default.unsafe

def zioRun[E,A](zio1:ZIO[Any,E,A]): A =
  Unsafe.unsafeCompat { implicit u =>
    myRuntime.run(zio1).getOrThrowFiberFailure()
  }

def zioFork[E,A](zio1:ZIO[Any,E,A]):Fiber.Runtime[E,A] =
  Unsafe.unsafeCompat { implicit u =>
    myRuntime.fork((zio1))
  }

////////////////////////////////////////////// a uio with start stop pause resume

trait ControlledZio:
  val uio:ZIO[Any,Nothing,Unit]
  def start(): Unit = 
    val fiber = zioFork(interrupt() *> gate.unlock() *> uio)
    opt = Some(fiber)
  def stop(): Unit = 
    zioFork(interrupt() *> gate.unlock())
  def pause(): Unit = 
    zioRun(gate.lock())
  def resume(): Unit = 
    zioRun(gate.unlock())
  def waitIfLocked(): UIO[Unit] = gate.waitIfLocked()

  private def interrupt(): ZIO[Any,Nothing,Unit] =
    opt match
        case None => ZIO.succeed(())
        case Some(fiber) => fiber.interrupt.as(())
  private val gate = 
    val signal:TRef[Boolean] = zioRun(TRef.make(true).commit)
    Gate(signal)
  private var opt: Option[Fiber.Runtime[Nothing,Unit]] = None

//////////////////////////////////////////////

trait WebComponent[A <:L.HtmlElement]:
  val elem: A

trait ControlledWebComponent[A <:L.HtmlElement,B] extends WebComponent[A]:
  self =>
  def getControlledZio(x:B): ControlledZio
  var controlledZioOpt: Option[ControlledZio] = None
  def start(x:B): Unit =
    controlledZioOpt.map(_.stop())
    val ctrlZio = getControlledZio(x)
    controlledZioOpt = Some(ctrlZio)
    ctrlZio.start()
  def stop() =
    controlledZioOpt.map(_.stop())
    ()
  def pause() =
    controlledZioOpt.map(_.pause())
    ()
  def resume() =
    controlledZioOpt.map(_.resume())
    ()
  // composition
  def zipPar[E <:L.HtmlElement,F](other:ControlledWebComponent[E,F]) =
    ???

