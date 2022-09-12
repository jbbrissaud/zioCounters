package example

import com.raquo.laminar.api.L._
import org.scalajs.dom

import zio.*

val myRuntime = Runtime.default.unsafe

implicit class RunSyntax[E,A](io: ZIO[Any, E, A]) {
  // My thanks to Adam Fraser for the code
  def unsafeRun: A =
    Unsafe.unsafeCompat { implicit u =>
      myRuntime.run(io).getOrThrowFiberFailure()
    }
}

object App:
  def doPar(content1:Var[String],content2:Var[String]):Unit = 
    def loop(n:Int,d:Duration,content:Var[String]):ZIO[Any,Nothing,Unit] =
      for
        _ <- ZIO.succeed(content.set(n.toString()))
        _ <- ZIO.sleep(d)
        _ <- loop(n+1,d,content)
      yield ()
    val zio1 = loop(10,1.second,content1)
    val zio2 = loop(100,500.millisecond,content2)
    val zio = 
      for
        fiber1 <- zio1.fork
        _ <- zio2
        _ <- fiber1.join
      yield ()
    zio.timeout(5.second).unsafeRun


  def appComponent = 
    val content1 = Var("init1")
    val content2 = Var("init2")
    val useless = Var(())
    div(
      button(
        "click me",
        onClick.map(_ => doPar(content1,content2)) --> useless
        ),
      textArea(
        child.text <-- content1.signal
      ),
      textArea(
        child.text <-- content2.signal
      )
    )
  def main(args: Array[String]): Unit =
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _            = render(appContainer, appComponent)
    }(unsafeWindowOwner)
