package Redis.service

import Redis.DB.RedisDbT
import Redis.DB.RedisDbT.EnqueueElementRequest
import akka.actor.{Actor, ActorLogging}
import spray.json._
import util.JsonHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object MessagingService {
  case class QueueElement[T](data : T , numRetry : Int = 1)
  case class SendMessageRequest(phoneNumber : String, msg : String, enqueue : Boolean)
  case class SendMessageResponse(status : Boolean)
}
trait MessagingService extends Actor
  with ActorLogging
  with JsonHelper {

  val redisClient = getRedisClient.getRedisInstance
  def getRedisClient : RedisDbT

   private def sendMsg(number: String, message: String ): Future[Boolean] = {
    Future.failed(new Exception("Messaging Gateway Not found"))
  }

  import MessagingService._
  override def receive: Receive = {
    case req : SendMessageRequest =>
      val currentSender = sender()
     val sendMsgFut =  sendMsg(
        number  = req.phoneNumber,
        message = req.msg
      )
      sendMsgFut onComplete{
        case Success(res) =>
          currentSender ! SendMessageResponse(res)
        case Failure(ex)  =>
          log.error("Error occured while sending a message, error Message : {} ", ex.getMessage)
          //here we now queue it up in redis
          val queueName = "Support"
          val msgJson   = QueueElement(req).toJson.toString()
          if(req.enqueue) {
            redisClient ! EnqueueElementRequest(queueName, msgJson)
          }
          currentSender ! SendMessageResponse(false)

      }
  }
}
