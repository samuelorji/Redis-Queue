package Redis.scheduler

import Redis.DB.RedisDbT
import Redis.DB.RedisDbT._
import Redis.worker.Worker.RedisElement
import akka.actor.{Actor, ActorLogging, Cancellable, Props}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

object Scheduler{
  case object Poll

  def createScheduler(
    worker : Props,
   redis : RedisDbT,
   maxNumDeq : Int,
   queueName : String,
   delay : FiniteDuration
  ) = {
    Props(new Scheduler(worker,redis,maxNumDeq,queueName,delay))
  }
}

 private[scheduler] class Scheduler(
    worker : Props,
    redis : RedisDbT,
    maxNumDeq : Int,
    queueName : String,
    delay : FiniteDuration
) extends Actor
  with ActorLogging {
   assert(maxNumDeq > 1)
   val workerActor = context.actorOf(worker, queueName)
   import Scheduler._

   private val redisClient = redis.getRedisInstance
   private var numTimes = 0
   override def receive: Receive = {

     case Poll =>
       redisClient ! DequeueElementRequest(queueName)

     case req : DequeueElementResult =>
       req.result match {
         case Some(res) =>
           workerActor ! RedisElement(res)
           numTimes += 1
           if(numTimes < maxNumDeq){
             redisClient ! DequeueElementRequest(queueName)
           }else{
             scheduleFetch
           }
         case None      =>
           scheduleFetch
       }
   }
   override def postStop(): Unit = if(scheduler != null && !scheduler.isCancelled) scheduler.cancel()
   private var scheduler : Cancellable = null

   private def scheduleFetch = {
      numTimes  = 0
      scheduler = context.system.scheduler.scheduleOnce(delay, self, Poll)

   }

   override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
     log.error(s"Actor ${self.path} Died, message received : {} , error Message : {}",reason.getMessage,message)
   }
 }
