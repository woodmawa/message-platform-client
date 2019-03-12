package com.softwood.implementation


import com.softwood.client.MessageSystemClient
import weblogic.jndi.Environment
import javax.jms.*
import javax.naming.Context
import javax.naming.NamingException
import java.security.InvalidParameterException


enum JmsConnectionType {
    Sender,
    Receiver
}

/**
 * weblogic jms platform client factor, inherits base capability via traits
 * for common reusable message pattern capabilities
 */
class WlsJmsMessagePlatform implements MessageSystemClient,
        PublisherTrait, SenderTrait,
        SubscriberTrait, ReceiverTrait
{

    //private ThreadLocal<Environment> wlsenv = new ThreadLocal<>()
    private ThreadLocal<Context> ctx= new ThreadLocal<>()
    private Map operatingEnv
    private String QCF_NAME = "jms/queueConnectionFactory"
    private String QTF_NAME = "jms/topicConnectionFactory"
    private String DEFAULT_QUEUE = 'jms/workOrderQueue'
    private String DEFAULT_TOPIC = 'jms/workOrderQueue'
    private QueueConnectionFactory qcf = null
    private TopicConnectionFactory qtf = null
    private ThreadLocal<TopicConnection> sendertc = new ThreadLocal<>()
    private ThreadLocal<TopicConnection> receivertc = new ThreadLocal<>()
    private  ThreadLocal<TopicSession> publisherTsession = new ThreadLocal<>()
    private  ThreadLocal<TopicSession> subscriberTsession = new ThreadLocal<>()
    private  ThreadLocal<TopicPublisher> tpublisher = new ThreadLocal<>()
    private  ThreadLocal<TopicSubscriber> tsubscriber = new ThreadLocal<>()



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
        //wlsenv.set (new Environment ())
        //wlsenv.get().setProviderUrl (providerUrl)
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
        else
            tidyUpReceiver()

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
