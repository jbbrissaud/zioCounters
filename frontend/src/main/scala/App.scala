package example

import com.raquo.laminar.api.L._
import org.scalajs.dom

import zio.*

val myRuntime = Runtime.default.unsafe

implicit class RunSyntax[E,A](io: ZIO[Any, E, A]) {
  def unsafeRun: A =
    Unsafe.unsafeCompat { implicit u =>
      myRuntime.run(io).getOrThrowFiberFailure()
    }
}

object App:

  def appComponent = 
    //div("Salut toi")
    val myZio = ZIO.succeed(div("salut"))
    myZio.unsafeRun
  def main(args: Array[String]): Unit =
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _            = render(appContainer, appComponent)
    }(unsafeWindowOwner)
