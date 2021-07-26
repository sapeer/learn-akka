package mongodb

import com.mongodb.client.result.InsertOneResult
import com.mongodb.connection.ClusterSettings
import org.mongodb.scala._

import scala.collection.JavaConverters._

object MongoScala extends App {
//  val mongoClient: MongoClient = MongoClient("mongodb+srv://m001-student:m001-mongodb-basics@sandbox.1dhkw.mongodb.net")
  val mongoClient: MongoClient = MongoClient(
    MongoClientSettings.builder()
      .applyToClusterSettings((builder: ClusterSettings.Builder) => builder.hosts(List(new ServerAddress("127.0.0.1", 27017)).asJava))
      .build())

  val database: MongoDatabase = mongoClient.getDatabase("mydb")
  val collection: MongoCollection[Document] = database.getCollection("test")
  val doc: Document = Document("_id" -> 0, "name" -> "MongoDB", "type" -> "database",
    "count" -> 1, "info" -> Document("x" -> 203, "y" -> 102))

  collection.insertOne(doc)

  val observable: Observable[InsertOneResult] = collection.insertOne(doc)

  observable.subscribe(new Observer[InsertOneResult] {
    override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
    override def onNext(result: InsertOneResult): Unit = println(s"onNext $result")
    override def onError(e: Throwable): Unit = println("Failed")
    override def onComplete(): Unit = println("Completed")
  })

}
