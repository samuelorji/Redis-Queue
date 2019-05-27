package Redis.listener

import Redis.DB.RedisDbT
import Redis.DB.RedisDbT._
import Redis.worker.Worker.RedisElement
import Redis.worker
import Redis.worker
import akka.actor.{Actor, ActorRef, Cancellable, Props}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

object Listener{

  case object Poll

  def createListener(
                      worker : Props,
                     redis : RedisDbT,
                     maxNumTries : Int,
                     queueName : String,
                     delay : FiniteDuration
                    ) = {
    Props(new Listener(worker,redis,maxNumTries,queueName,delay))
  }
}

 private[listener] class Listener (
                  worker : Props,
                  redis : RedisDbT,
                  maxNumTries : Int,
                  queueName : String,
                  delay : FiniteDuration
) extends Actor {


   val workerActor = context.actorOf(worker)
   import Listener._
   override def preStart(): Unit = {
     self ! Poll
   }

   private val redisClient = redis.getRedisInstance
   private var numTimes = 0
   override def receive: Receive = {

     case Poll =>
       redisClient ! DequeueElementRequest(queueName)

     case req : DequeueElementResult =>
       req.result match {
         case Some(res) =>
           workerActor ! RedisElement(req.result.get)
           numTimes +=1
           if(numTimes <= maxNumTries){
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
      scheduler = context.system.scheduler.scheduleOnce(delay, self, Poll)

   }


}
