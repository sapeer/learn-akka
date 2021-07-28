package mongodb

import org.mongodb.scala._

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AnotherMongoDBScala extends App {

  // Use a Connection String
  val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
  val database: MongoDatabase = mongoClient.getDatabase("mydb")
  val collection: MongoCollection[Document] = database.getCollection("test")
  val doc: Document = Document("_id" -> 0, "name" -> "MongoDB", "type" -> "database",
    "count" -> 1, "info" -> Document("x" -> 203, "y" -> 102))
  // now, lets add lots of little documents to the collection so we can explore queries and cursors
  val documents: IndexedSeq[Document] = (1 to 100) map { i: Int => Document("i" -> i) }

  import Helpers._

  collection.insertOne(doc).results()
  val insertObservable = collection.insertMany(documents)
  val insertAndCount = for {
    insertResult <- insertObservable
    countResult <- collection.countDocuments()
  } yield countResult

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

  println(s"total # of documents after inserting 100 small ones (should be 101):  ${insertAndCount.headResult()}")

  /*collection.find().first().printHeadResult()

  // Query Filters
  // now use a query to get 1 document out
  collection.find(equal("i", 71)).first().printHeadResult()

  // now use a range query to get a larger subset
  collection.find(gt("i", 50)).printResults()

  // range query with multiple constraints
  collection.find(and(gt("i", 50), lte("i", 100))).printResults()

  // Sorting
  collection.find(exists("i")).sort(descending("i")).first().printHeadResult()

  // Projection
  collection.find().projection(excludeId()).first().printHeadResult()

  //Aggregation
  collection.aggregate(Seq(
    filter(gt("i", 0)),
    project(Document("""{ITimes10: {$multiply: ["$i", 10]}}"""))
  )).printResults()

  // Update One
  collection.updateOne(equal("i", 10), set("i", 110)).printHeadResult("Update Result: ")

  // Update Many
  collection.updateMany(lt("i", 100), inc("i", 100)).printHeadResult("Update Result: ")

  // Delete One
  collection.deleteOne(equal("i", 110)).printHeadResult("Delete Result: ")

  // Delete Many
  collection.deleteMany(gte("i", 100)).printHeadResult("Delete Result: ")

  collection.drop().results()

  // ordered bulk writes
  val writes: List[WriteModel[_ <: Document]] = List(
    InsertOneModel(Document("_id" -> 4)),
    InsertOneModel(Document("_id" -> 5)),
    InsertOneModel(Document("_id" -> 6)),
    UpdateOneModel(Document("_id" -> 1), set("x", 2)),
    DeleteOneModel(Document("_id" -> 2)),
    ReplaceOneModel(Document("_id" -> 3), Document("_id" -> 3, "x" -> 4))
  )

  collection.bulkWrite(writes).printHeadResult("Bulk write results: ")

  collection.drop().results()

  collection.bulkWrite(writes, BulkWriteOptions().ordered(false)).printHeadResult("Bulk write results (unordered): ")

  collection.find().printResults("Documents in collection: ")*/

  // Clean up
  collection.drop().results()

  // release resources
  mongoClient.close()

}
