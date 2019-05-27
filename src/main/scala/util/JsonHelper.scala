package util

import Redis.service.MessagingService.{ QueueElement, SendMessage}
import spray.json._
import DefaultJsonProtocol._

trait JsonHelper {

  implicit val messageFormat = jsonFormat3(SendMessage)
//  implicit val enqueueFormat = jsonFormat2(EnqueuedMessage)
  implicit val queueElementFormat1  = jsonFormat2(QueueElement[SendMessage])
//  implicit val queueElementFormat2 = jsonFormat2(QueueElement[EnqueuedMessage])

}
