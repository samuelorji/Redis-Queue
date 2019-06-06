package Redis.worker

import akka.actor.{Actor, ActorRef, Props}

object Worker {
  case class RedisElement(element : String)
  def createWorker(useElement : String => Unit) : Props = Props(new Worker(
    useElement
  ))
}

private[worker] class Worker(useElement : String => Unit) extends Actor  {
  import Worker._
  override def receive: Receive = specificReceive orElse genericReceive
  def specificReceive : Receive = Map.empty
  private def genericReceive  : Receive  = {
    case req : RedisElement =>
      useElement(req.element)
  }

}
