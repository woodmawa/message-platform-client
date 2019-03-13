package com.softwood.implementation


import com.softwood.client.MessageSystemClient
import weblogic.jndi.Environment
import javax.jms.*
import javax.naming.Context
import javax.naming.NamingException
import java.security.InvalidParameterException


enum JmsConnectionType {
    Sender,
    Receiver,
    Publisher,
    Subscriber
}

/**
 * weblogic jms platform client factor, inherits base capability via traits
 * for common reusable message pattern capabilities
 *
 * attempted to be concurrent aware using thread local pattern
 *
 * usage : create one or more instances for each controlled group of senders/receivers or
 * publisher/subscribers
 *
 * can be same system - or remote ends using the Queue/topic as the integration point
 *
 * not using durable processes yet - so system failure would lose work
 *
 */
//Todo more work for durable subscriptions and secure SSL connections

class WlsJmsMessagePlatform implements MessageSystemClient,
        PublisherTrait, SenderTrait,
        SubscriberTrait, ReceiverTrait
{

    //private ThreadLocal<Environment> wlsenv = new ThreadLocal<>()
    private ThreadLocal<Context> ctx= new ThreadLocal<>()  //heavy weight object - may want to make static at some point
    private Map operatingEnv
    private String QCF_NAME = "jms/queueConnectionFactory"
    private String QTF_NAME = "jms/topicConnectionFactory"
    private String DEFAULT_QUEUE = 'jms/workOrderQueue'
    private String DEFAULT_TOPIC = 'jms/workOrderQueue'
    private QueueConnectionFactory qcf = null
    private TopicConnectionFactory qtf = null

    WlsJmsMessagePlatform (final Map env) {

        String providerUrl = env?.providerUrl ?: env?.defaultProviderUrl
        //store local operating environment variables
        operatingEnv = env

        if (!providerUrl) {
            throw new InvalidParameterException("providerUrl ($providerUrl) is not a valid JMS provider")
            System.exit(-1)
        }

        Environment wlsenv =  new Environment ()
        wlsenv.setProviderUrl (providerUrl)
        try {
            ctx.set (wlsenv.getInitialContext())//new InitialContext(properties)
        } catch (NamingException ne) {
            ne.printStackTrace(System.err)
            System.exit(0)
        }

    }

    //called by jms traits to get required details for platform
    Map getPlatformEnvironment () {
        operatingEnv
    }

    void setQueueConnectionfactory (String connectionFactoryName ) {
        QCF_NAME = connectionFactoryName
    }

    String getQueueConnectionfactory () {
        return QCF_NAME
    }

    void setTopicConnectionfactory (String connectionFactoryName ) {
        QTF_NAME = connectionFactoryName
    }

    String getTopicConnectionfactory () {
        return QTF_NAME
    }


    /**
     * parent service to lookup a queue name from the message naming context
     *
     * @param queueName
     * @return queue
     */
    Queue getQueue (String queueName) {
        // lookup Queue
        Queue queue
        try {
            queue = (Queue) ctx.get().lookup(queueName)
        }
        catch (NamingException ne) {
            ne.printStackTrace(System.err)
            System.exit(0)
        }
        return queue
    }

    /**
     * parent service to lookup a topic name from the message naming context
     *
     * @param topicName
     * @return topic
     */
    Topic getTopic (String topicName) {
        // lookup Queue
        Topic topic
        try {
            topic = (Topic) ctx.get().lookup(topicName)
        }
        catch (NamingException ne) {
            ne.printStackTrace(System.err)
            System.exit(0)
        }
        return topic
    }

    /**
     * resource handler for either sending or receiving
     * at end will ensure that the seession and connections have been closed
     *
     *
     * @param queue
     * @param closure
     * @return
     */
    def withQueue (JmsConnectionType type, Queue queue, Closure closure ) {
        Closure clos = closure?.clone()
        clos.delegate = this  //set the closure delegate to this platform implementation instance

        QueueConnection qc
        QueueSession qs
        if (type == JmsConnectionType.Sender) {
            qc = senderQconnection.get() ?: createSenderQueueConnection()
            qs = senderQsession.get() ?: createSenderQueueSession()
        } else if (type == JmsConnectionType.Receiver) {
            qc = receiverQconnection.get() ?: createReceiverQueueConnection()
            qs = receiverQsession.get() ?: createReceiverQueueSession()
            qc.start()

        }

            //call users closure with queue session
            def result = clos(qs)

        //reset the thread local variables
        if (type == JmsConnectionType.Sender)
            tidyUpSender()
        else if (type == JmsConnectionType.Receiver)
            tidyUpReceiver()

        result
    }

    /**
     * resource handler for either publishing or subscribing
     * at end will ensure that the seession and connections have been closed
     *
     *
     * @param topic
     * @param closure
     * @return
     */
    def withTopic (JmsConnectionType type, Topic topic, Closure closure ) {
        Closure clos = closure?.clone()
        clos.delegate = this  //set the closure delegate to this platform implementation instance

        TopicConnection tc
        TopicSession ts
        if (type == JmsConnectionType.Publisher) {
            tc = publisherTconnection.get() ?: createPublisherTopicConnection()
            ts = publisherTsession.get() ?: createPublisherSession()
        } else if (type == JmsConnectionType.Subscriber) {
            tc = subscriberTconnection.get() ?: createPublisherTopicConnection()
            ts = subscriberTsession.get() ?: createSubscriberTopicSession()
            tc.start()

        }

        //call users closure with topic session
        def result = clos(qs)

        //reset the thread local variables
        if (type == JmsConnectionType.Publisher)
            tidyUpPublisher()
        else if (type == JmsConnectionType.Subscriber)
            tidyUpSubscriber()

        result
    }

    @Override
    Message browse(Queue queue) {
        return null
    }

    @Override
    Message browse(String queueName) {
        return null
    }

    @Override
    def onMessage() {
        return null
    }


}
