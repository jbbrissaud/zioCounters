package example

import com.raquo.laminar.api.L._
import org.scalajs.dom

import zio.*
import zio.stm._
import org.w3c.dom.events.MouseEvent

val myRuntime = Runtime.default.unsafe

def zioRun[E,A](zio1:ZIO[Any,E,A]): A =
  Unsafe.unsafeCompat { implicit u =>
    myRuntime.run(zio1).getOrThrowFiberFailure()
  }

def zioFork[E,A](zio1:ZIO[Any,E,A]):Fiber.Runtime[E,A] =
  Unsafe.unsafeCompat { implicit u =>
    myRuntime.fork((zio1))
  }

trait ControlledZio:
  val uio:ZIO[Any,Nothing,Unit]
  def start(): Unit = 
    val fiber = zioFork(interrupt() *> gate.unlock() *> uio)
    zioRun(ref.set(Some(fiber)))
  def stop(): Unit = 
    zioFork(interrupt() *> gate.unlock())
  def pause(): Unit = 
    zioRun(gate.lock())
  def resume(): Unit = 
    zioRun(gate.unlock())
  def waitIfLocked(): UIO[Unit] = gate.waitIfLocked()

  private def interrupt(): ZIO[Any,Nothing,Unit] =
    ref.get.flatMap{
        case None => ZIO.succeed(())
        case Some(fiber) => fiber.interrupt.map(_ => ())
    }
  private val gate = 
    val signal:TRef[Boolean] = zioRun(TRef.make(true).commit)
    Gate(signal)
  private val ref: zio.Ref[Option[Fiber.Runtime[Nothing,Unit]]] = zioRun(zio.Ref.make(None))

object MyApp:
  def myComponent =
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
    div(
      button(
        "start",
        onClick --> ((ev:dom.MouseEvent)=>
          val n = ev.clientX.toInt
          controlledZioOpt.map(_.stop())
          val ctrlZio = controlledZio(n)
          controlledZioOpt = Some(ctrlZio)
          ctrlZio.start()
        )),
      button(
        "stop",
        onClick --> ((_:Any)=>
          controlledZioOpt.map(_.stop())
          ()
      )),
      button(
        "pause",
        onClick --> ((_:Any)=>
          controlledZioOpt.map(_.pause())
          ()
      )),
      button(
        "resume",
        onClick --> ((_:Any)=>
          controlledZioOpt.map(_.resume())
          ()
      )),
      textArea(
        child.text <-- content.signal
      ),
    )


  def appComponent = 
    val myDiv =
      div()
    for i <- 1 to 3 do
      myDiv.amend(myComponent)
    myDiv

  def main(args: Array[String]): Unit =
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _            = render(appContainer, appComponent)
    }(unsafeWindowOwner)
