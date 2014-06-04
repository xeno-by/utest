package utest

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.scalajs.js.annotation.{JSExportDescendentObjects, JSExport}
import scala.scalajs.js
import scala.scalajs.runtime.StackTrace.ColumnStackTraceElement

/**
 * Platform specific stuff that differs between JVM and JS
 */
@JSExport
object PlatformShims {
  def flatten[T](f: Future[Future[T]]): Future[T] = {
    f.value.get.map(_.value.get) match {
      case Success(Success(v)) => Future.successful(v)
      case Success(Failure(e)) => Future.failed(e)
      case Failure(e) => Future.failed(e)
    }
  }

  def await[T](f: Future[T]): T = f.value.get.get

  def escape(s: String) = {
    s.replace("\\", "\\\\").replace("\t", "\\t")
  }

  def printTrace(e: Throwable): Unit = {
    def bundle(s: StackTraceElement) = {
      Seq(s.getClassName, s.getMethodName, s.getFileName, s.getLineNumber, s.getColumnNumber)
        .map(_ + "")
        .map(escape)
        .mkString("\t")
    }

    println(
      e.getStackTrace
        .map(s => s"XXSecretXX/trace/${bundle(s)}")
        .mkString("\n")
    )
  }

  @JSExport
  def runSuite(suite: TestSuite,
               path: js.Array[String],
               args: js.Array[String]) = {
    val res = utest.runSuite(
      suite,
      path,
      args,
      s => println("XXSecretXX/addCount/" + s),
      s => println("XXSecretXX/log/" + s),
      s => println("XXSecretXX/addTotal/" + s)
    )
    println("XXSecretXX/result/" + res.replace("\n", "ZZZZ"))
  }

  @JSExportDescendentObjects
  class Test
}
object Main extends js.JSApp{
  def main(): Unit = {
    val e = new ArrayIndexOutOfBoundsException().getStackTrace
    println(e(0).getFileName)
    0 match {case 2 => }
  }
}

