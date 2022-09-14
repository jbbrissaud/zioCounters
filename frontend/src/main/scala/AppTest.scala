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
  def myComponent =
    val fiberOptRef: zio.Ref[Option[Fiber.Runtime[Nothing,Option[Unit]]]] = zio.Ref.make(None).unsafeRun
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
      val zio3 =
        fiberOptRef.get.flatMap(opt =>
          opt match
            case None => ZIO.succeed(())
            case Some(fiber) => 
              for
                _ <-fiber.interrupt
              yield ()
          )
      val zio4 = 
        for
          _ <- zio3
          fiber <- zio2.fork
          _ <- fiberOptRef.set(Some(fiber))
          _ <-fiber.join
        yield ()
      zio4.unsafeRun
          




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
