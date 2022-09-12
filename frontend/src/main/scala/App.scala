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

  def appComponent = 
    val content = Var(ZIO.succeed("ok"))
    content.set(ZIO.succeed("ok1"))
    div(
      button(
        "click me",
        onClick.map(_ => ZIO.succeed("toto")) --> content
        ),
      textArea(
        child.text <-- content.signal.map(_.unsafeRun)
      )
    )
  def main(args: Array[String]): Unit =
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _            = render(appContainer, appComponent)
    }(unsafeWindowOwner)
