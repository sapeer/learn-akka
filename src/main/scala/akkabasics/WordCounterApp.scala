package akkabasics

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import org.bson.types.ObjectId
import org.mongodb.scala._

import java.io.File
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.{Failure, Success}

/**
 *
 */
object WordCounterApp extends App {
  val VERSION: String = "0.0.1"
  val sourceDirectory: String = "src/main/resources/stageFiles"
  val fileList = getListOfFiles(sourceDirectory)
  val wordCountActorSystem = ActorSystem("WordCountActorSystem")
  val aggregator = wordCountActorSystem.actorOf(Props[Aggregator], "aggregator")
  val persist = wordCountActorSystem.actorOf(Props[PersistLines], "persistor")

  //STEP 0: get list of files from source directory
  def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  import WCMessages._

  fileList.foreach(file =>
    wordCountActorSystem.actorOf(Props(new FileReader(file)), "fileReader" + fileList.indexOf(file)) ! ReadFile
  )

  /**
   *
   * @param file File Name
   */
  class FileReader(val file: File) extends Actor with ActorLogging {

    import WCMessages._

    // Use a Connection String
    val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
    val database: MongoDatabase = mongoClient.getDatabase("wordcountdb")
    val collection: MongoCollection[Document] = database.getCollection("lines")

    override def receive: Receive = {
      case ReadFile =>
        log.info("Inside ReadFile for file :: {}", file)
        val sourceFile = Source.fromFile(file)
        for (line <- sourceFile.getLines()) {
          context.actorOf(Props(new WordCounter(collection, file, line)), "wordCounter" + randomUUID().toString) ! CountWords
          persist ! InsertLine(collection, file.toString, line)

        }
        sourceFile.close()
    }
  }

  /**
   * STEP 2: Word Count Actor - counts words and sends to AggregatorActor
   *
   * @param collection Collection Name
   * @param file       File Name
   * @param line       Line in File
   */
  class WordCounter(val collection: MongoCollection[Document], val file: File, val line: String) extends Actor with ActorLogging {

    import WCMessages._

    override def receive: Receive = {
      case CountWords =>
        //        log.info("Number of words in line :: {} :: is {}",line, line.split(" ").length)
        aggregator ! AggregateWords(file.toString, line.split(" ").length)
    }
  }

  /**
   * STEP 3a : Send message to AggregatorActor to print statistics
   */
  class Aggregator extends Actor with ActorLogging {

    import WCMessages._

    override def receive: Receive = {
      case AggregateWords(file: String, wc: Int) =>
    }

    def updateCount(wordCount: Map[String, Int]): Receive = {
      case AggregateWords(file: String, wc: Int) =>
        var newWordCount: Map[String, Int] = Map()
        if (!wordCount.contains(file)) newWordCount = wordCount.updated(file, wc)
        else newWordCount = wordCount.updated(file, wordCount(file) + wc)
        context.become(updateCount(newWordCount))
      //        log.info("File :: {}, line count :: {}", file, newWordCount.get(file))

      case PrintCount(file: String) =>
        log.info("File :: {}, line count :: {}", file, wordCount.get(file))
    }
  }

  /**
   * STEP 3b : Insert into MongoDB
   */
  class PersistLines extends Actor with ActorLogging {

    import Helpers._
    import WCMessages._

    override def receive: Receive = {
      case InsertLine(collection: MongoCollection[Document], file: String, line: String) =>
        val doc: Document = Document("_id" -> new ObjectId, "file" -> file, "line" -> line)
        val ps = Future {
          Thread.sleep(10)
          collection.insertOne(doc).results()
        }
        ps.onComplete {
          case Success(_) => log.info("Persisted line :: {} :: in file :: {}", line, file)
          case Failure(e) => log.error("Failed to persist line :: {} with error :: {}", line, e.printStackTrace())
        }
    }
  }

  //STEP 1: FileReadActor - create one actor to process each file.
  //FileReadActor - reads each file and sends each line to a new WordCount actor.
  object WCMessages {
    case class FileName(fileName: String)

    case class AggregateWords(fileName: String, wc: Int)

    case class PrintCount(fileName: String)

    case class InsertLine(collection: MongoCollection[Document], file: String, line: String)

    case object ReadFile

    case object CountWords
  }

  //  fileList.foreach(file =>
  //    aggregator ! PrintCount(file.toString)
  //  )

  object Helpers {

    implicit class DocumentObservable[C](val observable: Observable[Document]) extends ImplicitObservable[Document] {
      override val converter: Document => String = doc => doc.toJson
    }

    implicit class GenericObservable[C](val observable: Observable[C]) extends ImplicitObservable[C] {
      override val converter: C => String = doc => doc.toString
    }

    trait ImplicitObservable[C] {
      val observable: Observable[C]
      val converter: C => String

      def printResults(initial: String = ""): Unit = {
        if (initial.nonEmpty) print(initial)
        results().foreach(res => println(converter(res)))
      }

      def results(): Seq[C] = Await.result(observable.toFuture(), Duration(10, TimeUnit.SECONDS))

      def printHeadResult(initial: String = ""): Unit = println(s"$initial${converter(headResult())}")

      def headResult(): C = Await.result(observable.head(), Duration(10, TimeUnit.SECONDS))
    }

  }

}
