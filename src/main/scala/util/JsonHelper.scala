package util

import Redis.service.MessagingService.{ QueueElement, SendMessageRequest}
import spray.json._
import DefaultJsonProtocol._

trait JsonHelper {

  implicit val messageFormat        = jsonFormat4(SendMessageRequest)
  implicit val queueElementFormat1  = jsonFormat2(QueueElement[SendMessageRequest])


}
