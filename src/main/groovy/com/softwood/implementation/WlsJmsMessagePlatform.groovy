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

/**
 * weblogic jms platform client factor
 */
class WlsJmsMessagePlatform implements MessageSystemClient {

    private static Context senderCtx  //two contexts required?
    private static Context receiverCtx
    static Environment senderWlsenv
    static Environment receiverWlsenv
    private static String QCF_NAME = "jms/queueConnectionFactory"
    private static String QTF_NAME = "jms/topicConnectionFactory"
    private static QueueConnectionFactory qcf = null
    private static TopicConnectionFactory qtf = null
    private static ThreadLocal<QueueConnection> qc = null
    private static ThreadLocal<TopicConnection> tc = null
    private static ThreadLocal<QueueSession> qsession = null
    private static ThreadLocal<TopicSession> tsession = null
    private static ThreadLocal<QueueSender> qsendr = null
    private static ThreadLocal<QueueReceiver> qrecvr = null



    WlsJmsMessagePlatform (Map env) {
        // create InitialContext
        Hashtable properties = new Hashtable();
        /*properties.put(Context.INITIAL_CONTEXT_FACTORY,
                "weblogic.jndi.WLInitialContextFactory")*/

        String providerUrl = env.providerUrl ?: env.defaultProviderUrl
        String senderPrincipal = env.senderSecurityPrincipal
        String senderCredentials = env.senderSecurityCredentials

        senderWlsenv = new Environment()
        senderWlsenv.setProviderUrl(providerUrl)
        senderWlsenv.setSecurityPrincipal(senderPrincipal)
        senderWlsenv.setSecurityCredentials(senderCredentials)
        try {
            senderCtx =  senderWlsenv.getInitialContext()//new InitialContext(properties)
        } catch (NamingException ne) {
            ne.printStackTrace(System.err)
            System.exit(0)
        }

        providerUrl = env.providerUrl ?: env.defaultProviderUrl
        String receiverPrincipal = env.receiverSecurityPrincipal
        String receiverCredentials = env.receiverSecurityCredentials

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

    QueueConnection createQueueConnection () {
        if (!qcf) {
            try {
                qcf = (QueueConnectionFactory)senderCtx.lookup(QCF_NAME)
            }
            catch (NamingException ne) {
                ne.printStackTrace(System.err)
                System.exit(0)
            }
        }

        try {
            //set thread local connection
            qc.set(qcf.createQueueConnection())
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        qc.get()
    }

    QueueSession createQueueSession () {
        // create QueueSession
        try {
            // set thread local session
            qsession.set (qc.get().createQueueSession(false, Session.AUTO_ACKNOWLEDGE))
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        qsession.get()
    }

    Queue getQueue (String queueName) {
        // lookup Queue
        Queue queue
        try {
            queue = (Queue) senderCtx.lookup(queueName)
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
            qsession.get().createReceiver(q)
            qrecvr.set (receiver =  qsession.get().createReceiver(q))
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
