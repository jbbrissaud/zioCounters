package example

import com.raquo.laminar.api.L._
import com.raquo.laminar.api.L
import org.scalajs.dom

import zio.*
import zio.stm._
import org.w3c.dom.events.MouseEvent
import com.raquo.laminar.nodes.ReactiveHtmlElement

//////////////////////////////////////////////

val myRuntime = Runtime.default.unsafe

def zioRun[E,A](zio1:ZIO[Any,E,A]): A =
  Unsafe.unsafeCompat { implicit u =>
    myRuntime.run(zio1).getOrThrowFiberFailure()
  }

def zioFork[E,A](zio1:ZIO[Any,E,A]):Fiber.Runtime[E,A] =
  Unsafe.unsafeCompat { implicit u =>
    myRuntime.fork((zio1))
  }

//////////////////////////////////////////////

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

class MyCounter extends WebComponent[Div]:
  val content = Var("init1")
  def controlledZio(n:Int) = new ControlledZio {
    val uio =
      def loop(n:Int,d:Duration):ZIO[Any,Nothing,Unit] =
        for
          _ <- ZIO.succeed(content.set(n.toString())) *> ZIO.sleep(d)
          _ <- waitIfLocked()
          _ <- loop(n+1,d)
        yield ()
      val zio1 = loop(n*100,100.millisecond)
      val zio2 = zio1.timeout(30.second).as(())
      zio2
  }
  var controlledZioOpt:Option[ControlledZio] = None
  def doStart(ev:dom.MouseEvent): Unit =
    val n = ev.clientX.toInt
    start(n)
  def start(n:Int): Unit =
    controlledZioOpt.map(_.stop())
    val ctrlZio = controlledZio(n)
    controlledZioOpt = Some(ctrlZio)
    ctrlZio.start()
  def doStop(ev:dom.MouseEvent) = stop()
  def stop() =
    controlledZioOpt.map(_.stop())
    ()
  def doPause(ev:dom.MouseEvent) = pause()
  def pause() =
    controlledZioOpt.map(_.pause())
    ()
  def doResume(ev:dom.MouseEvent) = resume()
  def resume() =
    controlledZioOpt.map(_.resume())
    ()
          
  val elem:Div = div(
    button(
      "start",
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
    )
  )

//////////////////////////////////////////////

object MyApp:
  def myComponent = MyCounter()
  val nbrComponent = 20
  val myComponents = for i <- 1 to nbrComponent yield myComponent

  def appComponent = 
    val myDiv =
      div()
    for myComponent <- myComponents do
      myDiv.amend(myComponent.elem)
    myDiv
    
  def test() =
    val r = scala.util.Random
    val nbr = 40
    for i <- 1 to nbr do
      val n = r.nextInt(nbrComponent)
      val myComponent = myComponents(n)
      r.nextInt(4) match
        case 0 => myComponent.start(100*i+n*10)
        case 1 => myComponent.stop()
        case 2 => myComponent.pause()
        case 3 => myComponent.resume()

  def main(args: Array[String]): Unit =
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _            = render(appContainer, appComponent)
    }(unsafeWindowOwner)
    test()

