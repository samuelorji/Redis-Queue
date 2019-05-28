package util

import Redis.service.MessagingService.{ QueueElement, SendMessageRequest}
import spray.json._
import DefaultJsonProtocol._

trait JsonHelper {

  implicit val messageFormat = jsonFormat3(SendMessageRequest)
//  implicit val enqueueFormat = jsonFormat2(EnqueuedMessage)
  implicit val queueElementFormat1  = jsonFormat2(QueueElement[SendMessageRequest])
//  implicit val queueElementFormat2 = jsonFormat2(QueueElement[EnqueuedMessage])

}
