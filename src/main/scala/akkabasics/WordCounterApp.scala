package akkabasics

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import java.io.File
import java.util.UUID.randomUUID
import scala.io.Source

object WordCounterApp extends App {
  val VERSION: String = "0.0.1"
  val sourceDirectory: String = "/Users/rrajesh1979/Documents/Learn/gitrepo/word-count/java-wc-thread/src/main/resources"

  //STEP 0: get list of files from source directory
  def getListOfFiles(dir: String) : List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  val fileList = getListOfFiles(sourceDirectory)

  val wordCountActorSystem = ActorSystem("WordCountActorSystem")

  import WCMessages._
  fileList.foreach(file =>
    wordCountActorSystem.actorOf(Props(new FileReader(file)), "fileReader" + fileList.indexOf(file)) ! ReadFile
  )

  val aggregator = wordCountActorSystem.actorOf(Props[Aggregator], "aggregator")

  //STEP 1: FileReadActor - create one actor to process each file.
  //FileReadActor - reads each file and sends each line to a new WordCount actor.
  object WCMessages {
    case class FileName(fileName: String)
    case object ReadFile
    case object CountWords
    case class AggregateWords(fileName: String, wc: Int)
    case class PrintCount(fileName: String)
  }
  class FileReader(val file: File) extends Actor with ActorLogging {
    import WCMessages._

    override def receive: Receive = {
      case ReadFile =>
        log.info("Inside ReadFile for file :: {}", file)
        val sourceFile = Source.fromFile(file)
        for (line <- sourceFile.getLines())
          context.actorOf(Props(new WordCounter(file, line)), "wordCounter" + randomUUID().toString) ! CountWords
        sourceFile.close()
    }
  }

  //STEP 2: Word Count Actor - counts words and sends to AggregatorActor
  class WordCounter(val file: File, val line: String) extends Actor with ActorLogging {
    import WCMessages._

    override def receive: Receive = {
      case CountWords =>
//        log.info("Number of words in line :: {} :: is {}",line, line.split(" ").length)
        aggregator ! new AggregateWords(file.toString, line.split(" ").length)
    }
  }

  //STEP 3: Send message to AggregatorActor to print statistics
  class Aggregator extends Actor with ActorLogging {
    import WCMessages._

    override def receive: Receive = {
      case AggregateWords(file: String, wc: Int) =>
        context.become(updateCount(Map(file -> wc)))
    }

    def updateCount(wordCount: Map[String, Int]): Receive = {
      case AggregateWords(file: String, wc: Int) =>
        var newWordCount: Map[String, Int] = Map()
        if (!wordCount.contains(file)) newWordCount = wordCount.updated(file, wc)
        else newWordCount = wordCount.updated(file, wordCount(file) + wc)
        context.become(updateCount(newWordCount))
        log.info("File :: {}, line count :: {}", file, newWordCount.get(file))

      case PrintCount(file: String) =>
        log.info("File :: {}, line count :: {}", file, wordCount.get(file))
    }
  }

//  fileList.foreach(file =>
//    aggregator ! PrintCount(file.toString)
//  )

}
