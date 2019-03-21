package com.softwood.client

import com.softwood.implementation.JmsConnectionType

import javax.jms.MessageListener
import javax.jms.QueueBrowser
import javax.jms.QueueConnection
import javax.jms.QueueReceiver
import javax.jms.Message
import javax.jms.Queue
import javax.jms.QueueSender
import javax.jms.QueueSession
import javax.jms.Topic
import javax.jms.TopicPublisher
import javax.jms.TopicSubscriber

/**
 * this is an abstraction of messaging platform with simplified options for basic use patterns
 *
 *
 */
interface MessageSystemClient {

    //todo later flexibility by providing config map /headers to refine behaviour
    //todo get unmanaged JMS sessions - users do as they please with these and responsible for own tidy up

    //lookups
    Queue getQueue (String queueName)
    Topic getTopic (String topicName)
    def getPlatformEnvironmentProperty (String propertyName)
    Map getPlatformEnvironment ()

    //Q senders
    void sendText (String stringMessage)
    void send (Message message)
    void send (QueueSender sender, Message message)

    //Q receivers
    String receiveText ()
    Message receive (QueueReceiver qrecvr)
    Message receive (QueueReceiver qrecvr, Long timeout)
    void receiverStart()
    void receiverStop()

    //Topic publishers
    void publishText (String stringMessage)
    void publish (Message message)
    void publish (TopicPublisher publisher, Message message)

    //Topic subscribers
    String subscriptionReceiveText ()
    Message subscriptionReceive (TopicSubscriber tSubscriber)
    Message subscriptionReceive (TopicSubscriber tSubscriber, Long timeout)
    void setAsyncMessageListener (MessageListener listener)
    void setAsyncMessageListener (MessageListener listener, TopicSubscriber tSubscriber)
    void subscribersStart()
    void subscribersStop()

    //Q browse
    int browseQueueSize (Queue queue)
    int browseQueueSize ()
    String browseTopOfQueue(Queue queue)
    String browseTopOfQueue()
    List<Message> browse ()
    List<Message> browse (String queueName)
    List<Message> browse (QueueBrowser browser)
    List<Message> browse (Queue queue)


    // excecute users closure asa resource with auto cleanup at end
    def withQueue (JmsConnectionType type, Queue queue, Closure closure)
    def withTopic (JmsConnectionType type, Topic topic, Closure closure)

    //manual tidyup options
    def tidyUpSender ()
    def tidyUpReceiver()
    def tidyUpPublisher()
    def tidyUpBrowser ()

}