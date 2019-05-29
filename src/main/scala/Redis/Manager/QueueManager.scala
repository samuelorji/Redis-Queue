package Redis.Manager

import Redis.scheduler.Scheduler.Poll
import akka.actor.{Actor, ActorLogging, Props}

object QueueManager {
  case object Start
  def createSchedulers(schedulers : List[Props]) : Props = Props(new QueueManager(schedulers))
}
private [Manager] class QueueManager(listeners : List[Props]) extends Actor with ActorLogging {
  import QueueManager._
  override def preStart(): Unit =
    listeners.foreach(context.actorOf(_))
    self ! Start

  override def receive: Receive = {
    case Start =>
      context.children.foreach( _ ! Poll)
  }
}
