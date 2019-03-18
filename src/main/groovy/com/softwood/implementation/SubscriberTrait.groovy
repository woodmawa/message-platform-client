package com.softwood.implementation

import javax.jms.JMSException
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Queue
import javax.jms.QueueConnection
import javax.jms.QueueConnectionFactory
import javax.jms.QueueReceiver
import javax.jms.QueueSession
import javax.jms.Session
import javax.jms.TextMessage
import javax.jms.Topic
import javax.jms.TopicConnection
import javax.jms.TopicConnectionFactory
import javax.jms.TopicSession
import javax.jms.TopicSubscriber
import javax.naming.NamingException

/**
 *
 * reusable trait for basic controlled JMS features that based on feature groups
 *
 * this is the constrained JMS subscriber feature set
 *
 * relies on its parent implementation class, for environment variables, and queue/topic lookups
 *
 * @Author Will Woodman
 * @Copyright Softwood Consulting ltd, 2019
 *
 */

//todo potentially need to add subscriber functions for queue subscribers!
//todo however this would work for only 1:n delivery (queue semantics) whereas topic subscription provides n:n
trait SubscriberTrait {

    private ThreadLocal<TopicConnection> subscriberTconnection = new ThreadLocal<>()
    private ThreadLocal<TopicSession> subscriberTsession = new ThreadLocal<>()
    private ThreadLocal<TopicSubscriber> tSubscriber = new ThreadLocal<>()

    /**
     * use queue connection factory to create QueueConnection object for sender using appropriate
     * security principal and credentials.  queue connection created is also stored in a threadlocal
     * local variable for sender or receiver
     *
     *
     * @param type - Enum of either Sender or Receiver determines which security principal/credential are used
     * @return new QueueConnection - used to create queue related objects
     */
    TopicConnection createSubscriberTopicConnection() {

        // call dependency on implenting parent, expecting map of key message platform variables
        Map env = this.getPlatformEnvironment()

        TopicConnection stc
        //topic connection factor should have been setup by parent's implementing class constructor
        if (!tcf) {
            try {
                tcf = (TopicConnectionFactory) ctx.get().lookup(TCF_NAME)
                println("Got TopicConnectionFactory " + tcf.toString())

            }
            catch (NamingException ne) {
                ne.printStackTrace(System.err)
                System.exit(0)
            }
        }

        String subscriberPrinciple
        String subscriberCredentials
        try {
            //set thread local connection {

            if (!subscriberTconnection.get()) {
                println "createReceiverQueueConnection: no Q connection - so create one "

                subscriberPrinciple = env.get('mvaSubscriberSecurityPrincipal')
                subscriberCredentials = env.get('mvaSubscriberSecurityCredentials')
                rqc = tcf.createTopicConnection(subscriberPrinciple, subscriberCredentials)
                subscriberTconnection.set(stc)
                println("Got QueueConnection " + rqc.toString())
            } else
            //just return existing thread local version
                stc = subscriberTconnection.get()

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        stc
    }

    /**
     * need to start a connection, and any subscribers will go active  and can then receive messages on subscription from it
     */
    void subscribersStart () {

        if (!subscriberTconnection.get()) {
            createSubscriberTopicConnection()
        }
        println("start TopicConnection on ${subscriberTconnection.get()}" )

        subscriberTconnection.get()?.start()
    }

    void subscribersStop() {
        println("stop TopicConnection " )

        subscriberTconnection.get()?.stop()
    }

    /**
     * creates a new TopicSession from appropriate TopicConnection for a subscriber.  creates
     * auto acknowledge sessions by default.   Stores subscriber session in thread local variable
     *
     * A TopicSession allows the creation of multiple TopicSubscriber objects per topic. It will deliver
     * each message for a topic to each subscriber eligible to receive it. Each copy of the message is treated
     * as a completely separate message. Work done on one copy has no effect on the others;
     * acknowledging one does not acknowledge the others; one message may be delivered immediately,
     * while another waits for its subscriber to process messages ahead of it.
     *
     * @param tc - optional topic connection passed from parent
     *
     * @return
     */

    TopicSession createSubscriberTopicSession(TopicConnection tc = null) {
        // create TopicSession
        TopicSession ts
        try {
            //if no subscriber topic connection - then build one
            if (!subscriberTconnection.get()) {
                println "createSubscriberTopicSession: no existing  T connection for thread - create one "
                if (tc == null) {
                    //invoke the impl class parent service to build queueConnection for sender
                    subscriberTconnection.set(createSubscriberTopicConnection())
                } else
                    subscriberTconnection.set(tc)
            }
            if (!subscriberTsession.get()) {
                // set thread local session
                println("createReceiverQueueSession: no QueueSession created one for thread - " + qs.toString())
                subscriberTsession.set(ts = subscriberTconnection.get().createTopicSession(false, Session.AUTO_ACKNOWLEDGE))
            }else {
                //just return existing thread local Q session
                ts = subscriberTsession.get()
            }

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        ts
    }

    /**
     * A TopicSession allows the creation of multiple TopicSubscriber objects per topic.
     * Regular TopicSubscriber objects are not durable. They receive only messages that are published while they are active.
     *
     * Messages filtered out by a subscriber's message selector will never be delivered to the subscriber.
     * From the subscriber's perspective, they do not exist.
     *
     * This is creating 'non durable' subscribers - these will only get messages only whilst they are active
     *
     * @param t
     * @param messageSelector - optional filter for messages
     * @return
     */
    TopicSubscriber createTopicSubscriber(Topic t, String messageSelector = null) {

        //todo need to look at (get/set)MessageSelector to filter what gets triggered
        //todo also tSubscriber should be an array or map as there can be many per session

        TopicSubscriber subscriber
        if (!subscriberTsession.get())
            subscriberTsession.set (createSubscriberTopicSession())

        // create TopicSubscriber from the subscriber session
        try {
            def ssession = subscriberTsession.get()

            if (!tSubscriber.get()) {
                tSubscriber.set(subscriber = ssession.createSubscriber(t, messageSelector, false))
                println("Got TopicSubscriber " + subscriber.toString())
            } else {
                subscriber = tSubscriber.get()
            }

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        subscriber
    }


    /**
     * synchronous receive : delegates to subscriptionReceive (tSubscriber) using stored thread local
     * @return
     */
    String subscriptionReceiveText(Topic topicName = null) {
        def tSubscriber
        if (!this.tSubscriber.get()) {

            println("subscriptionReceiveText: no topic subscriber - create one  " )

            Topic t
            String tname
            if (topicName == null) {
                tname = getPlatformEnvironment().get('orderTopic') ?: this.DEFAULT_TOPIC
            }
            t = this.getTopic(tname)

            tSubscriber = createTopicSubscriber (t)
            this.tSubscriber.set (tSubscriber)

        }

        tSubscriber = this.tSubscriber.get()

        println("subscribing to Topic " + tSubscriber.getTopic().topicName)
        TextMessage txtMess =  subscriptionReceive(tSubscriber) as TextMessage
        String text = txtMess.getText() ?: "<nothing received>"
    }

    //synchronous receive
    Message subscriptionReceive (TopicSubscriber tSubscriber) {
        def text
        Message message
        TextMessage txtMess
        String receiveTimeout = getPlatformEnvironment().get ('defaultSubscriptionReceiveTimeout')
        try {
            println("subscribe : use  " + tSubscriber.toString() + " to subscribe to T : " + tSubscriber.getTopic().topicName)

            if (receiveTimeout)
                message = tSubscriber.receive(receiveTimeout as Long)
            else
                message = tSubscriber.receiveNoWait()
        }
        catch (JMSException jmse) {
            tidyUpSubscriber()
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        if (message == null) {
            def ssession = subscriberTsession.get()
            txtMess = subscriberTsession.get().createTextMessage()
            txtMess.setText("<Nothing to receive>")
            return txtMess
        }
        return message
    }

    //synchronous receive
    Message subscriptionReceive (TopicSubscriber tSubscriber, Long timeout) {
        def text
        Message message
        try {
            message = tSubscriber.receive(timeout)
        }
        catch (JMSException jmse) {
            tidyUpSubscriber()
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        if (message == null) {
            def ssession = subscriberTsession.get()
            message = subscriberTsession.get().createTextMessage().setText("<Nothing to receive>")
        }

        return message
    }

    /**
     * async receive : set up a listener to process messages as they arrive asynchronously
     * A MessageListener object is used to receive asynchronously delivered messages.
     *
     * Each session must insure that it passes messages serially to the listener. This means that
     * a listener assigned to one or more consumers of the same session can assume that the
     * onMessage method is not called with the next message until the session has completed the last call.
     *
     * MessageListener can be a closure morphed as a listener.
     * @param listener
     * @param tSubscriber
     */
    void setAsyncMessageListener (MessageListener listener, TopicSubscriber tSubs = null) {

        TopicSubscriber subscriber
        if (tSubs ){
            subscriber = tSubs
        } else
            subscriber = tSubscriber.get()

        try {
            subscriber.setMessageListener (listener)
        }
        catch (JMSException jmse) {
            tidyUpSubscriber()
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
    }

    def tidyUpSubscriber () {

        subscriberTconnection.get().stop()

        subscriberTsession.get()?.close()
        subscriberTconnection.get()?.close()
        subscriberTsession.set (null)
        subscriberTconnection.set (null)
    }

}
