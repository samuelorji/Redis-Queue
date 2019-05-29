package Redis

import Redis.DB.RedisDbT
import Redis.DB.RedisDbT.EnqueueElementRequest
import Redis.Manager.QueueManager
import Redis.scheduler.Scheduler
import Redis.service.MessagingService
import Redis.service.MessagingService.{QueueElement, SendMessageRequest, SendMessageResponse}
import Redis.worker.Worker
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import spray.json._
import util.JsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

object Run extends App
 with JsonHelper{

  val queueName : String = "Support"

  val redisHost : String = "localhost"
  val redisPort : Int = 6379

  implicit val system : ActorSystem = ActorSystem()
  implicit val timeout : Timeout    = FiniteDuration(5,"seconds")

  val redis = new RedisDbT() {
    override val host: String = redisHost
    override val port: Int = redisPort
    override val timeout: FiniteDuration = FiniteDuration(5,"seconds")
    override implicit val _system: ActorSystem = system
  }

  val redisClient       : ActorRef = redis.getRedisInstance
  val messagingService  : ActorRef = system.actorOf(Props(new MessagingService(){
    override def getRedisClient: RedisDbT = redis
  }))

  (1 to 10).foreach(x => messagingService ! SendMessageRequest(x.toString,x.toString,true)) //this is simulating sending 10 messages to the messaging service

  /*
  this is the worker function that we will supply when creating our worker,
   */
  val workerFunc : String => Unit = element => {
    try {
      val queuedData  = element.parseJson.convertTo[QueueElement[SendMessageRequest]]
      def shouldRetry(numTimes: Int): Boolean  = numTimes >= 1
      def handleQueuedElement(num: Int) : Unit  = if (shouldRetry(num)) {
        println(s"Data is ${queuedData.data} and number of times tried is ${queuedData.numRetry}")
        redisClient ! EnqueueElementRequest(queueName, queuedData.copy(numRetry = num - 1).toJson.toString())
      } else {
        //LOG Error That the user cannot be reached after trying NumTimes
        println(s"Not Enqeueing ${queuedData.data} again,since it has been enqueued ${queuedData.numRetry} time(s)")
      }

      (messagingService ? queuedData.data.copy(enqueue = false)).onComplete{
        case Success(res) =>
          res match {
            case SendMessageResponse(true)  =>
            //Yaay...our message was delivered, on to other things
            case SendMessageResponse(false) =>
              handleQueuedElement(queuedData.numRetry)
          }
        case Failure(ex)  =>
          handleQueuedElement(queuedData.numRetry)
      }
    }
    catch {
      case ex : ClassCastException =>
        println(s"Error received : ${ex.getMessage}")
    }
  }

  val worker = Worker.createWorker(queueName , x => workerFunc(x))

  val SupportListener = Scheduler.createScheduler(
                            worker     = worker,
                            redis      = redis,
                            maxNumDeq  = 3,
                            queueName  = queueName,
                            delay      = FiniteDuration(10, "seconds")
                          )

  val queueManagers = system.actorOf(QueueManager.createSchedulers(List(SupportListener)))

}
