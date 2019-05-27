package Redis

import Redis.DB.RedisDbT
import Redis.DB.RedisDbT.{AddElementRequest, EnqueueElementRequest}
import Redis.Manager.QueueManager
import Redis.worker.Worker
import Redis.listener.Listener
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.SmallestMailboxPool

import scala.concurrent.duration.FiniteDuration

object Run extends App {

  val redisHost = "localhost"
  val redisPort = 6379
  implicit val system = ActorSystem()
  val redis = new RedisDbT() {
    override val host: String = redisHost
    override val port: Int = redisPort
    override val timeout: FiniteDuration = FiniteDuration(5,"seconds")
    override implicit val _system: ActorSystem = system
  }
  val redisClient = redis.getRedisInstance
  def init() = {
    val stuffListener =
      //system.actorOf(Props(new Listener(
      Listener.createListener(
      worker = Worker.createWorker(queueName , x => println(x)),
      redis = redis,
      maxNumTries = 5,
      queueName    = queueName,
      delay        = FiniteDuration(1, "seconds")
    )
    val queueManagers = system.actorOf(QueueManager.createListener(List(stuffListener)))
  }

  val queueName = "stuff"
 // (1 to 100).foreach(x => redisClient ! EnqueueElementRequest(queueName,x.toString,None))


  init()




}
