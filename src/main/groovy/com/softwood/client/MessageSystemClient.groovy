package com.softwood.client

import com.softwood.implementation.JmsConnectionType

import javax.jms.QueueReceiver
import javax.jms.Message
import javax.jms.Queue
import javax.jms.QueueSender

interface MessageSystemClient {
    void send (String stringMessage)
    void send (Message message)
    void send (QueueSender sender, Message message)

    Message receive ()
    Message receive (QueueReceiver qrecvr)
    Message receive (QueueReceiver qrecvr, Long timeout)
    Message browse (String queueName)
    Message browse (Queue queue)
    def withQueue (JmsConnectionType type, Queue queue, Closure closure)
    def tidyUp()

    def onMessage ()
}