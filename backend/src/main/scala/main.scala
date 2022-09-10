package example

import zhttp.http._
import zhttp.service.Server
import zio.*
import zio.json.*

import java.io.File

object WebServer extends ZIOAppDefault:

  def getFile(filename:String) =
    val file = new File(filename)
    Http.fromFile(file)
  
  private val appBase = 
    Http.collectHttp[Request] {
      case Method.GET -> !! =>
        val filename = "frontend/static/index.html"
        getFile(filename)
      case Method.GET -> !! / "favicon.ico"=>
        val filename = "frontend/static/plante.ico"
        getFile(filename)
      case Method.GET -> !! / "static" / "main.js" =>
        val filename = "frontend/target/scala-3.1.1/frontend-fastopt/main.js"
        getFile(filename)
      case Method.GET -> !! / "static" / name =>
        val filename = s"frontend/static/$name"
        getFile(filename)
    }
  private val appHello: Http[Any, Nothing, Request, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "hello1" / name  => 
        //ZIO.succeed(Response.text(s"Hello $name!"))
        ZIO.succeed(Response.text(s"Bonjour ${name}!"))
    }

  private val app = appHello ++ appBase
  def run = Server.start(8090, app)

end WebServer
