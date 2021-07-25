package akkabasics

import akka.actor.{Actor, ActorLogging}

import java.io.File

object WordCounterApp extends App {
  val VERSION: String = "0.0.1"
  val sourceDirectory: String = "/Users/rrajesh1979/Documents/Learn/gitrepo/word-count/java-wc-thread/src/main/resources/"

  def getListOfFiles(dir: String) : List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  val fileList = getListOfFiles(sourceDirectory)

//  class FileActor extends Actor {
//    override def receive: Receive = addFilesToList(null)
//
//    case object AddFile
//
//    def addFilesToList(fileName: String): Receive = {
//      case AddFile =>
//        context.become()
//    }
//  }
//
//
//  fileList.foreach(file => fileActor ! file)

}
