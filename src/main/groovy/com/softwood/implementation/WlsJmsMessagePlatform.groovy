package com.softwood.implementation

import com.softwood.client.AbstractMessagePlatformFactory
import com.softwood.client.MessageSystemClient
import weblogic.jndi.Environment
import javax.jms.*

import javax.jms.JMSException
import javax.jms.QueueConnection
import javax.jms.QueueConnectionFactory
import javax.jms.QueueSession
import javax.jms.Session
import javax.jms.TopicConnection
import javax.jms.TopicConnectionFactory
import javax.jms.TopicSession
import javax.naming.Context
import javax.naming.NamingException
import java.security.InvalidParameterException


enum JmsConnectionType {
    Sender,
    Receiver
}

/**
 * weblogic jms platform client factor
 */
class WlsJmsMessagePlatform implements MessageSystemClient {

    private ThreadLocal<Environment> wlsenv
    private ThreadLocal<Context> ctx
    private static Context senderCtx  //two contexts required?
    private static Context receiverCtx
    static Environment senderWlsenv
    static Environment receiverWlsenv
    private String senderPrinciple
    private String receiverPrinciple
    private String QCF_NAME = "jms/queueConnectionFactory"
    private String QTF_NAME = "jms/topicConnectionFactory"
    private QueueConnectionFactory qcf = null
    private TopicConnectionFactory qtf = null
    private ThreadLocal<QueueConnection> senderqc = null
    private ThreadLocal<TopicConnection> sendertc = null
    private ThreadLocal<QueueConnection> receiverqc = null
    private ThreadLocal<TopicConnection> receivertc = null
    private ThreadLocal<QueueSession> senderQsession = null
    private ThreadLocal<QueueSession> receiverQsession = null
    private  ThreadLocal<TopicSession> publisherTsession = null
    private  ThreadLocal<TopicSession> subscriberTsession = null
    private  ThreadLocal<QueueSender> qsender = null
    private  ThreadLocal<QueueReceiver> qreceiver = null
    private  ThreadLocal<TopicPublisher> tpublisher = null
    private  ThreadLocal<TopicSubscriber> tsubscriber = null



    WlsJmsMessagePlatform (Map env) {
        // create InitialContext
        Hashtable properties = new Hashtable();
        /*properties.put(Context.INITIAL_CONTEXT_FACTORY,
                "weblogic.jndi.WLInitialContextFactory")*/

        String providerUrl = env?.providerUrl ?: env?.defaultProviderUrl
        if (!providerUrl) {
            throw new InvalidParameterException("providerUrl ($providerUrl) is not a valid JMS provider")
            System.exit(-1)
        }
        senderPrinciple = env.mvaSenderSecurityPrincipal
        String senderCredentials = env.mvaSenderSecurityCredential

        wlsenv.set(new Environment ())
        wlsenv.get().setProviderUrl (providerUrl)
        try {
            ctx.set (wlsenv.get().getInitialContext())//new InitialContext(properties)
        } catch (NamingException ne) {
            ne.printStackTrace(System.err)
            System.exit(0)
        }

/*        senderWlsenv = new Environment()
        senderWlsenv.setProviderUrl(providerUrl)
        senderWlsenv.setSecurityPrincipal(senderPrincipal)
        senderWlsenv.setSecurityCredentials(senderCredentials)
        try {
            senderCtx =  senderWlsenv.getInitialContext()//new InitialContext(properties)
            senderCtx
        } catch (NamingException ne) {
            ne.printStackTrace(System.err)
            System.exit(0)
        }

        providerUrl = env.providerUrl ?: env.defaultProviderUrl
        receiverPrinciple = env.mvaReceiverSecurityPrincipal
        String receiverCredentials = env.mvaReceiverSecurityCredentials

        receiverWlsenv = new Environment()
        receiverWlsenv.setProviderUrl(providerUrl)
        receiverWlsenv.setSecurityPrincipal(receiverPrincipal)
        receiverWlsenv.setSecurityCredentials(receiverCredentials)
        try {
            receiverCtx =  receiverWlsenv.getInitialContext()//new InitialContext(properties)
        } catch (NamingException ne) {
            ne.printStackTrace(System.err)
            System.exit(0)
        }
        */

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

    QueueConnection createQueueConnection (JmsConnectionType type) {
        QueueConnection qc
        if (!qcf) {
            try {
                qcf = (QueueConnectionFactory)ctx.get().lookup(QCF_NAME)
            }
            catch (NamingException ne) {
                ne.printStackTrace(System.err)
                System.exit(0)
            }
        }

        try {
            //set thread local connection {
            if (type == JmsConnectionType.Sender) {
                String senderCredential = wlsenv.get().getProperty('mvaSenderSecurityCredentials')
                qc = senderqc.set(qcf.createQueueConnection(senderPrinciple, senderCredential))
            } else if (type == JmsConnectionType.Receiver) {
                String receiverCredential = wlsenv.get().getProperty('mvaRecieverSecurityCredentials')
                qc = receiverqc.set(qcf.createQueueConnection(receiverPrinciple, receiverCredential))
            }
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        qc
    }

    QueueSession createQueueSession (JmsConnectionType type) {
        // create QueueSession
        QueueSession qs
        try {
            // set thread local session
            if (type == JmsConnectionType.Sender) {
                senderQsession.set(qs = senderqc.get().createQueueSession(false, Session.AUTO_ACKNOWLEDGE))
            } else if (type == JmsConnectionType.Receiver) {
                receiverQsession.set(qs= receiverqc.get().createQueueSession(false, Session.AUTO_ACKNOWLEDGE))
            }
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        qs
    }

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



    QueueReceiver createQueueReceiver (Queue q) {
        // create QueueReceiver
        QueueReceiver receiver
        try {
            qreceiver.set (receiver =  receiverQsession.get().createReceiver(q))
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        receiver
    }

    /**
     * resource handler
     * @param queue
     * @param closure
     * @return
     */
    def withQueue (Queue queue, Closure closure ) {
        Closure clos = closure?.clone()
        clos.delegate = this  //set the closure delegate to this platform implementation instance
        QueueConnection qc = qc.get() ?: createQueueConnection()
        qc.start()
        QueueSession qs = qsession.get() ?: createQueueSession()
            clos(qs)  //call users closure
        qs.close()
        qc.close()

    }

    QueueSender createQueueSender (Queue q) {
        // create QueueSender
        QueueSender sender
        if (!qsession.get())
            qsession.set (createQueueSession())
        try {
            qsendr.set( sender = qsession.get().createSender (q))
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        sender
    }

    @Override
    void send(Message message) {
        // send message
        try {
            qsendr.get().send(message)
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }

    }

    @Override
    void send (String text) {

        Message message
        try {
            message = qsession.get().createTextMessage()
            message.setText (text)
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        send (message)

    }


    @Override
    Message receive() {
        if (!qrecvr.get()) {
            qrecvr.set (createQueueReceiver(qc.get()))
        }
        return receive (qrecvr.get())
    }

    @Override
    Message receive(QueueReceiver qrecvr) {
        def text
        TextMessage message
        try {
            message = qrecvr.receive(1L) as TextMessage
            text = message?.text ?: "<Timed Out>"
        }
        catch (JMSException jmse) {
            tidyUp()
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        return text
    }

    @Override
    Message receive(QueueReceiver qrecvr, Long timeout) {
        def text
        TextMessage message
        try {
            message = qrecvr.receive(timeout) as TextMessage
            text = message?.text ?: "<Timed Out>"
        }
        catch (JMSException jmse) {
            tidyUp()
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        return text    }


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

    def tidyUp() {
        QueueConnection connection = qc.get()
        QueueSession session = qsession.get()
        session?.close()
        connection?.close()
    }
}
