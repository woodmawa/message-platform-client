package com.softwood.client

import javax.jms.QueueReceiver
import javax.jms.Message
import javax.jms.Queue

interface MessageSystemClient {
    void send (String stringMessage)
    void send (Message message)
    Message receive ()
    Message receive (QueueReceiver qrecvr)
    Message receive (QueueReceiver qrecvr, Long timeout)
    Message browse (String queueName)
    Message browse (Queue queue)
    def withQueue (Queue queue, Closure closure)
    def tidyUp()

    def onMessage ()
}