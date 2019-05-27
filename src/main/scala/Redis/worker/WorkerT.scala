package Redis.worker

import akka.actor.{Actor, ActorRef, Props}

object Worker {
  case class RedisElement(element : String)
  def createWorker(queueName : String, useElement : String => Unit) : Props = Props(new Worker(
    queueName,useElement
  ))
}

private[worker] class Worker(queueName : String, useElement : String => Unit) extends Actor  {
  import Worker._
  override def receive: Receive = specificReceive orElse genericReceive
  def specificReceive : Receive = Map.empty
  private def genericReceive  : Receive  = {
    case req : RedisElement =>
      useElement(req.element)
  }

}
