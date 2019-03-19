package com.softwood.implementation

import org.slf4j.Logger

import javax.jms.JMSException
import javax.jms.Message
import javax.jms.Queue
import javax.jms.QueueConnection
import javax.jms.QueueConnectionFactory
import javax.jms.QueueReceiver
import javax.jms.QueueSender
import javax.jms.QueueSession
import javax.jms.Session
import javax.jms.TextMessage

import javax.naming.NamingException

/**
 *
 * reusable trait for basic controlled JMS features that based on feature groups
 *
 * this is the constrained JMS receiver feature set
 *
 * relies on its parent implementation class, for environment variables, and queue/topic lookups
 *
 * @Author Will Woodman
 * @Copyright Softwood Consulting ltd, 2019
 *
 */
trait ReceiverTrait {

    ThreadLocal<QueueConnection> receiverQconnection = new ThreadLocal<>()
    ThreadLocal<QueueSession> receiverQsession = new ThreadLocal<>()
    private ThreadLocal<QueueReceiver> qReceiver = new ThreadLocal<>()

    Logger log = this.getLogger()  //get implementing classes logger
    /**
     * use queue connection factory to create QueueConnection object for sender using appropriate
     * security principal and credentials.  queue connection created is also stored in a threadlocal
     * local variable for sender or receiver
     *
     *
     * @param type - Enum of either Sender or Receiver determines which security principal/credential are used
     * @return new QueueConnection - used to create queue related objects
     */
    QueueConnection createReceiverQueueConnection() {

        // call dependency on implenting parent, expecting map of key message platform variables
        Map env = this.getPlatformEnvironment()

        QueueConnection rqc
        //queue connection factor should have been setup by parent's implementing class constructor
        if (!qcf) {
            try {
                qcf = (QueueConnectionFactory) ctx.get().lookup(QCF_NAME)
                log.debug("Got QueueConnectionFactory " + qcf.toString())

            }
            catch (NamingException ne) {
                ne.printStackTrace(System.err)
                System.exit(0)
            }
        }

        String receiverPrinciple
        String receiverCredentials
        try {
            //set thread local connection {

            if (!receiverQconnection.get()) {
                log.debug "createReceiverQueueConnection: no Q connection - so create one "

                receiverPrinciple = env.get('mvaReceiverSecurityPrincipal')
                receiverCredentials = env.get('mvaReceiverSecurityCredentials')
                rqc = qcf.createQueueConnection(receiverPrinciple, receiverCredentials)
                receiverQconnection.set(rqc)
                log.debug ("Got QueueConnection " + rqc.toString())
            } else
                //just return existing thread local version
                rqc = receiverQconnection.get()

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        rqc
    }

    /**
     * need to start a connection before you can get messages from it
     */
    void receiverStart () {

        if (!receiverQconnection.get()) {
            createReceiverQueueConnection()
        }
        log.debug("start QueueConnection on ${receiverQconnection.get()}" )

        receiverQconnection.get()?.start()
    }

    void receiverStop() {
        log .debug("stop QueueConnection " )

        receiverQconnection.get()?.stop()
    }

    /**
     * creates a new QueueSession from appropriate QueueConnection for either a receiver.  creates
     * auto acknowledge sessions by default.   Stores receiver session in thread local variable
     *
     * @param qc - optional queue connection passed from parent
     *
     * @return
     */

    QueueSession createReceiverQueueSession(QueueConnection qc = null) {
        // create QueueSession
        QueueSession qs
        try {
            //if no sender queue connection - then build one
            if (!receiverQconnection.get()) {
                log.debug() "createReceiverQueueSession: no existing  Q connection for thread - create one "
                if (qc == null) {
                    //invoke the impl class parent service to build queueConnection for sender
                    receiverQconnection.set(createReceiverQueueConnection())
                } else
                    receiverQconnection.set(qc)
            }
            if (!receiverQsession.get()) {
                // set thread local session
                log.debug("createReceiverQueueSession: no QueueSession created one for thread - " + qs.toString())
                receiverQsession.set(qs = receiverQconnection.get().createQueueSession(false, Session.AUTO_ACKNOWLEDGE))
            }else {
                //just return existing thread local Q session
                qs = receiverQsession.get()
            }

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        qs
    }

    QueueReceiver createQueueReceiver(Queue q) {

        QueueReceiver receiver
        if (!receiverQsession.get())
            receiverQsession.set (createReceiverQueueSession())

        // create QueueReceiver from the receiver session
        try {
            def qsession = receiverQsession.get()

            if (!qReceiver.get()) {
                qReceiver.set(receiver = qsession.createReceiver(q))
                log.debug ("Got QueueReceiver " + receiver.toString())
            } else {
                receiver = qReceiver.get()
            }

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        receiver
    }


    /**
     * delegates to receive (recvr) using stored thread local
     * @return
     */
    String receiveText(Queue queueName = null) {
        def qrecvr
        if (!qReceiver.get()) {

            log.debug ("receiveText no receiver - create one  " )

            Queue q
            String qname
            if (queueName == null) {
                qname = this.operatingEnv.get('orderQueue') ?: this.DEFAULT_QUEUE
            }
            q = this.getQueue(qname)

            qrecvr = createQueueReceiver (q)
            qReceiver.set (qrecvr)

        }

        qrecvr = qReceiver.get()

        log.debug ("reading from Queue " + qrecvr.getQueue().queueName)
        TextMessage txtMess =  receive(qrecvr) as TextMessage
        String text = txtMess.getText()
    }


    Message receive(QueueReceiver qrecvr) {
        def text
        Message message
        TextMessage txtMess
        try {
            log.debug("receive : use  " + qrecvr.toString() + " to read from Q : " + qrecvr.getQueue().queueName)

            message = qrecvr.receive(1L)
        }
        catch (JMSException jmse) {
            tidyUpReceiver()
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        if (message == null) {
            def qsession = receiverQsession.get()
            txtMess = receiverQsession.get().createTextMessage()
            txtMess.setText("<Timed Out>")
            return txtMess
        }
        return message
    }

    Message receive(QueueReceiver qrecvr, Long timeout) {
        def text
        Message message
        try {
            message = qrecvr.receive(timeout)
        }
        catch (JMSException jmse) {
            tidyUpReceiver()
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        if (message == null) {
            def qsession = receiverQsession.get()
            message = receiverQsession.get().createTextMessage().setText("<Timed Out>")
        }

        return message
    }

    def tidyUpReceiver () {
        qReceiver.set (null)
        receiverQconnection.get().stop()

        receiverQsession.get()?.close()
        receiverQconnection.get()?.close()
        receiverQsession.set (null)
        receiverQconnection.set (null)
    }
}
