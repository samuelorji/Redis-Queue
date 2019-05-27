package Redis.Manager

import Redis.listener.Listener.Poll
import akka.actor.{Actor, ActorRef, Props}

object QueueManager {
  case object Start

  def createListener(listeners : List[Props]) : Props = Props(new QueueManager(listeners))
}
private [Manager] class QueueManager(listeners : List[Props]) extends Actor {
  import QueueManager._
  override def preStart(): Unit =
    listeners.foreach(context.actorOf(_))
    self ! Start

  override def receive: Receive = {
    case Start =>
      context.children.foreach( _ ! Poll)
//    listeners.foreach(
//      _ ! Poll
//    )
  }

}
