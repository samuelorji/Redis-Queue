package Redis

import Redis.DB.RedisDbT
import Redis.DB.RedisDbT.EnqueueElementRequest
import Redis.Manager.QueueManager
import Redis.listener.Listener
import Redis.service.MessagingService
import Redis.service.MessagingService.{ QueueElement, SendMessage}
import Redis.worker.Worker
import akka.actor.{ActorSystem, Props}
import util.JsonHelper
import spray.json._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Run extends App
 with JsonHelper{

  val redisHost = "localhost"
  val redisPort = 6379
  implicit val system = ActorSystem()
  implicit val timeout : Timeout = FiniteDuration(5,"seconds")
  val redis = new RedisDbT() {
    override val host: String = redisHost
    override val port: Int = redisPort
    override val timeout: FiniteDuration = FiniteDuration(5,"seconds")
    override implicit val _system: ActorSystem = system
  }
  val redisClient = redis.getRedisInstance

  val messagingService = system.actorOf(Props(new MessagingService(){
    override def getRedisClient: RedisDbT = redis
  }))

  val workerFunc : String => Unit = element => {
    try {
      val data = element.parseJson.convertTo[QueueElement[SendMessage]]
      println(s"Data is ${data.data} and number of times tried is ${data.numRetry}")
       def shouldRetry(numTimes : Int) = numTimes >= 1
      def handleQueue(num : Int) = if(shouldRetry(num)) {
        redisClient ! EnqueueElementRequest(queueName, data.copy(numRetry = num - 1).toJson.toString())
      }else{
        //LOG Error That the user cannot be reached after trying NumTimes
        println("Dequeueing since we have maxed out at 5 times")
        println("Error Contacting Client ")
      }

      (messagingService ? data.data.copy(retry = false)).onComplete{
        case Success(res) =>
          res match {
            case true  =>
              //Since the message has been sent we don't need to bother
              println("sent Message ")
            case false =>
              handleQueue(data.numRetry)
          }
        case Failure(ex)  =>
          handleQueue(data.numRetry)
      }
    }
    catch {
      case ex : ClassCastException =>
        println(s"Error received : ${ex.getMessage}")

    }
  }

  val queueName = "Support"

    val SupportListener =
      Listener.createListener(
      worker = Worker.createWorker(queueName , x => workerFunc(x)),
      redis = redis,
      maxNumTries = 5,
      queueName    = queueName,
      delay        = FiniteDuration(1, "seconds")
    )
    val queueManagers = system.actorOf(QueueManager.createListener(List(SupportListener)))


 // (1 to 10).foreach(x => redisClient ! EnqueueElementRequest(queueName,QueueElement(SendMessage(x.toString, x.toString,true)).toJson.toString()))









}
