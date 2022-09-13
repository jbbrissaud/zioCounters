package example

import com.raquo.laminar.api.L._
import org.scalajs.dom

import zio.*

val myRuntime = Runtime.default.unsafe

object App:
  def myComponent =
    val content = Var("init1")
    def loop(n:Int,d:Duration):ZIO[Any,Nothing,Unit] =
      for
        _ <- ZIO.succeed(content.set(n.toString()))
        _ <- ZIO.sleep(d)
        _ <- loop(n+1,d)
      yield ()
    def doClick(clickEvent:Any): Unit = 
      val zio1 = loop(10,1.second)
      val zio2 = zio1.timeout(10.second)
      Unsafe.unsafeCompat { implicit u =>
        myRuntime.run(zio2)
      }
    div(
      button(
        "click me",
        onClick --> doClick
        ),
      textArea(
        child.text <-- content.signal
      ),
    )

  def appComponent = 
    myComponent

  def main(args: Array[String]): Unit =
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _            = render(appContainer, appComponent)
    }(unsafeWindowOwner)
