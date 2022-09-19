package example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import zio.*

////////////////////////////////////////////// A ControlledWebComponent example

def myCounter: ControlledWebComponent[Div,Int] = new ControlledWebComponent[Div,Int] {
  val period = 100.millisecond
  val timeout = 30.second
  val content = Var("not started yet")
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
  def getControlledZio(n:Int): ControlledZio = new ControlledZio {
    val uio =
      def loop(n:Int):ZIO[Any,Nothing,Unit] =
        for
          _ <- ZIO.succeed(content.set(n.toString())) *> ZIO.sleep(period)
          _ <- waitIfLocked()
          _ <- loop(n+1)
        yield ()
      val zio1 = loop(n)
      zio1.timeout(timeout).as(())
  }
  def doStart(ev:dom.MouseEvent): Unit =
    val n = ev.clientX.toInt
    start(n)
  def doStop(ev:dom.MouseEvent) = stop()
  def doPause(ev:dom.MouseEvent) = pause()
  def doResume(ev:dom.MouseEvent) = resume()
}

////////////////////////////////////////////// Main

object MyApp:
  val nbrComponent = 200
  val myComponents = for i <- 1 to nbrComponent yield myCounter

  def appComponent = 
    val myDiv =
      div()
    for myComponent <- myComponents do
      myDiv.amend(myComponent.elem)
    myDiv
    
  def main(args: Array[String]): Unit =
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _            = render(appContainer, appComponent)
    }(unsafeWindowOwner)
    //test()

////////////////////////////////////////////// Test
  def test() =
    val nbrClick = 400
    val r = scala.util.Random
    for i <- 1 to nbrClick do
      val n = r.nextInt(nbrComponent)
      val myComponent = myComponents(n)
      r.nextInt(4) match
        case 0 => myComponent.start(100*i+n*10)
        case 1 => myComponent.stop()
        case 2 => myComponent.pause()
        case 3 => myComponent.resume()

