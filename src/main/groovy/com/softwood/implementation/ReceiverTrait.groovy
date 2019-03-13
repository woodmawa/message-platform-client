package com.softwood.implementation

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

trait ReceiverTrait {

    private ThreadLocal<QueueConnection> receiverQconnection = new ThreadLocal<>()
    private ThreadLocal<QueueSession> receiverQsession = new ThreadLocal<>()
    private ThreadLocal<QueueReceiver> qReceiver = new ThreadLocal<>()

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
                println("Got QueueConnectionFactory " + qcf.toString())

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
                println "createReceiverQueueConnection: no Q connection - so create one "

                receiverPrinciple = env.get('mvaReceiverSecurityPrincipal')
                receiverCredentials = env.get('mvaReceiverSecurityCredentials')
                rqc = qcf.createQueueConnection(receiverPrinciple, receiverCredentials)
                receiverQconnection.set(rqc)
                println("Got QueueConnection " + rqc.toString())
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
        println("start QueueConnection on ${receiverQconnection.get()}" )

        receiverQconnection.get()?.start()
    }

    void receiverStop() {
        println("stop QueueConnection " )

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
                println "createReceiverQueueSession: no existing  Q connection for thread - create one "
                if (qc == null) {
                    //invoke the impl class parent service to build queueConnection for sender
                    receiverQconnection.set(createReceiverQueueConnection())
                } else
                    receiverQconnection.set(qc)
            }
            if (!receiverQsession.get()) {
                // set thread local session
                println("createReceiverQueueSession: no QueueSession created one for thread - " + qs.toString())
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
                println("Got QueueReceiver " + receiver.toString())
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

            println("receiveText no receiver - create one  " )

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

        println("reading from Queue " + qrecvr.getQueue().queueName)
        TextMessage txtMess =  receive(qrecvr) as TextMessage
        String text = txtMess.getText()
    }

    /* test hack
    String readQ () {
        println "got queue connection factory $qcf"
        QueueConnection qc= createReceiverQueueConnection()
        createReceiverQueueSession()
        Queue q = getQueue (this.operatingEnv.get('orderQueue'))
        println "got q ${q.queueName}"
        def recvr = createQueueReceiver(q)
        qc.start()
        println "start connection $qc"
        def mess = receive (recvr)
        println "read : " + mess.getText()
    }
    */

    Message receive(QueueReceiver qrecvr) {
        def text
        Message message
        TextMessage txtMess
        try {
            println("receive : use  " + qrecvr.toString() + " to read from Q : " + qrecvr.getQueue().queueName)

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
        receiverQconnection.get().stop()

        receiverQsession.get()?.close()
        receiverQconnection.get()?.close()
        receiverQsession.set (null)
        receiverQconnection.set (null)
    }
}
