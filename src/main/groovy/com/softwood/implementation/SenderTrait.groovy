package com.softwood.implementation

import javax.jms.JMSException
import javax.jms.Message
import javax.jms.Queue
import javax.jms.QueueConnection
import javax.jms.QueueConnectionFactory
import javax.jms.QueueSender
import javax.jms.QueueSession
import javax.jms.Session
import javax.naming.NamingException

/**
 *
 * reusable trait for basic controlled JMS features that based on feature groups
 *
 * this is the constrained JMS sender feature set
 *
 * relies on its parent implementation class, for environment variables, and queue/topic lookups
 *
 * @Author Will Woodman
 * @Copyright Softwood Consulting ltd, 2019
 *
 */
trait SenderTrait {

    // make sender queue connection public in the implementing class - expect impl class to set this
    ThreadLocal<QueueConnection> senderQconnection = new ThreadLocal<>()
    ThreadLocal<QueueSession> senderQsession = new ThreadLocal<>()

    private  ThreadLocal<QueueSender> qSender = new ThreadLocal<>()

    /**
     * use queue connection factory to create QueueConnection object for sender using appropriate
     * security principal and credentials.  queue connection created is also stored in a threadlocal
     * local variable for sender or receiver
     *
     *
     * @param type - Enum of either Sender or Receiver determines which security principal/credential are used
     * @return new QueueConnection - used to create queue related objects
     */
    QueueConnection createSenderQueueConnection () {

        // call dependency on implenting parent, expecting map of key message platform variables
        Map env = this.getPlatformEnvironment()

        QueueConnection qc
        //queue connection factor should have been setup by parent's implementing class constructor
        if (!qcf) {
            try {
                qcf = (QueueConnectionFactory)ctx.get().lookup(QCF_NAME)
            }
            catch (NamingException ne) {
                ne.printStackTrace(System.err)
                System.exit(0)
            }
        }

        String senderPrinciple
        String senderCredentials
        try {
            if (!senderQconnection.get()) {
                //set thread local connection {

                senderPrinciple = env.get('mvaSenderSecurityPrincipal')
                senderCredentials = env.get('mvaSenderSecurityCredentials')
                qc = qcf.createQueueConnection(senderPrinciple, senderCredentials)
            } else
                qc = senderQconnection.get()

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        qc
    }


    /**
     * creates a new QueueSession from appropriate QueueConnection for either a sender .  creates
     * auto acknowledge sessions by default.   Stores sender session in thread local variable
     *
     * @param qc - optional queue connection passed from parent
     *
     * @return
     */

    QueueSession createSenderQueueSession (QueueConnection qc = null) {
        // create QueueSession
        QueueSession qs
        try {
            //if no sender queue connection - then build one
            if (!senderQconnection.get()) {
                if (qc == null) {
                    //invoke the impl class parent service to build queueConnection for sender
                    senderQconnection.set(createSenderQueueConnection ())
                } else
                    senderQconnection.set (qc)
            }

            if (!senderQsession.get()) {
                // set thread local session
                senderQsession.set(qs = senderQconnection.get().createQueueSession(false, Session.AUTO_ACKNOWLEDGE))
            } else
                qs = senderQsession.get()
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        qs
    }

    QueueSender createQueueSender (Queue q) {
        // create QueueSender
        QueueSender sender
        if (!senderQsession.get())
            senderQsession.set (createSenderQueueSession())

        try {
            if (!qSender.get()) {
                qSender.set(sender = senderQsession.get().createSender(q))
            } else
                sender = qSender.get()
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        sender
    }

    void send(Message message) {
        // send message - use the default qSender from thread local
        // if not set create one and save in thread local
        if (!qSender.get()) {
            String qname = this.operatingEnv.get('orderQueue')  ?: this.DEFAULT_QUEUE
            Queue q = this.getQueue(qname)
            qSender.set (createQueueSender (q))
        }

        try {
            qSender.get().send(message)
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }

    }

    void send(QueueSender qsender, Message message) {
        // send message
        try {
            if (!qsender)
                throw new JMSException("missing QueueSender, received null")
            else
                qsender?.send(message)
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
    }

    void sendText (String text) {

        if (!senderQsession.get()) {
            createSenderQueueSession()
        }

        Message message
        try {
            message = senderQsession.get().createTextMessage()
            message.setText (text)
            send (message)
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }

    }

    def tidyUpSender () {
        senderQsession.get()?.close()
        senderQconnection.get()?.close()
        senderQsession.set (null)
        senderQconnection.set (null)
    }
}
