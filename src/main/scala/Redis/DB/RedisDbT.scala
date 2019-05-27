package Redis.DB

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import redis.RedisClient

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}


trait RedisDbT {
  val host: String
  val port: Int
  val timeout: FiniteDuration

  implicit val _system : ActorSystem
  def getRedisInstance = _system.actorOf(Props(new RedisDbService(host,port,timeout)))

}
object RedisDbT {

  case class AddElementRequest(key : String, value : String, duration : Option[FiniteDuration] = None)
  case class AddElementResult(status : Boolean)

  case class DeleteElementRequest(key : String)
  case class DeleteElementResult(status : Boolean)

  case class FetchElementQuery(key : String)
  case class FetchElementResult(result : Option[String])

  case class EnqueueElementRequest(queueName : String, value : String , duration : Option[FiniteDuration] = None )
  case class EnqueueElementResponse(status : Boolean)

  case class DequeueElementRequest(queueName : String)
  case class DequeueElementResult(result : Option[String])

}

private[DB] class RedisDbService(
 val host: String,
 val port: Int,
 val timeout: FiniteDuration
) extends Actor
  with ActorLogging {


  implicit val actorSystem = context.system
  lazy val client  = RedisClient(host,port)
  implicit val _timeout = timeout

  import RedisDbT._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive: Receive = {
    case req : AddElementRequest =>
      val currentSender = sender()
     val addFut =  req.duration match {
        case Some(dur) =>
          client.set(
            key       = req.key,
            value     = req.value,
            exSeconds = Some(dur.toSeconds)
          )
        case None      =>
          client.set(
            key       = req.key,
            value     = req.value
          )
      }
      addFut.onComplete{
        case Success(res) =>
          currentSender ! AddElementResult(res)
        case Failure(ex) =>
          log.error("Error Adding To Redis : {}",ex.getMessage)
          currentSender ! AddElementResult(false)
      }

    case req : DeleteElementRequest =>

      val currentSender = sender()
      val delFut = client.del(req.key)
      delFut.onComplete{
        case Success(res) => res match {
          case x  if x < 1 => currentSender ! DeleteElementResult(false)
          case _            => currentSender ! DeleteElementResult(true)
        }
        case Failure(ex)  =>
          log.error("Error Deleting from Redis : {}",ex.getMessage)
          currentSender ! DeleteElementResult(true)
      }

    case req : FetchElementQuery =>
      val currentSender = sender()
      val fetchFut = client.get(req.key)
      fetchFut.onComplete{
        case Success(res) =>
          currentSender ! FetchElementResult(res.map(_.utf8String))
        case Failure(ex)  =>
          log.error("Error Fetching from Redis : {}",ex.getMessage)
          currentSender ! FetchElementResult(None)
      }

    case req : EnqueueElementRequest =>
      val currentSender = sender()

      val enqueueFut = client.rpush(req.queueName,req.value)
      enqueueFut.onComplete{
        case Success(res) =>
          res match {
            case x if x < 1 => currentSender ! EnqueueElementResponse(false)
            case _          => currentSender ! EnqueueElementResponse(true)
          }
        case Failure(ex)  =>
          log.error("Error Enqueuing in Redis : {}",ex.getMessage)
          currentSender ! EnqueueElementResponse(false)
      }

    case req : DequeueElementRequest =>
      val currentSender = sender()
      val dequeueFut = client.lpop(req.queueName)
      dequeueFut.onComplete{
        case Success(res) =>
          currentSender ! DequeueElementResult(res.map(_.utf8String))
        case Failure(ex)  =>
          log.error("Error Dequeuing in Redis : {}",ex.getMessage)
          currentSender ! DequeueElementResult(None)
      }
  }
}
