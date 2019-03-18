package com.softwood.implementation

import javax.jms.JMSException
import javax.jms.Message
import javax.jms.Queue
import javax.jms.QueueBrowser
import javax.jms.QueueConnection
import javax.jms.QueueConnectionFactory
import javax.jms.QueueReceiver
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

trait BrowserTrait {

    private ThreadLocal<QueueConnection> browserQconnection = new ThreadLocal<>()
    private ThreadLocal<QueueSession> browserQsession = new ThreadLocal<>()
    private ThreadLocal<QueueBrowser> qBrowser = new ThreadLocal<>()

    /**
     * use queue connection factory to create QueueConnection object for sender using appropriate
     * security principal and credentials.  queue connection created is also stored in a threadlocal
     * local variable for sender or receiver
     *
     *
     * @param type - Enum of either Sender or Receiver determines which security principal/credential are used
     * @return new QueueConnection - used to create queue related objects
     */
    QueueConnection createBrowserQueueConnection() {

        // call dependency on implenting parent, expecting map of key message platform variables
        Map env = this.getPlatformEnvironment()

        QueueConnection bqc
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

            if (!browserQconnection.get()) {
                println "createBrowserQueueConnection: no Q connection - so create one "

                receiverPrinciple = env.get('mvaBrowserSecurityPrincipal')
                receiverCredentials = env.get('mvaBrowserSecurityCredentials')
                bqc = qcf.createQueueConnection(receiverPrinciple, receiverCredentials)
                browserQconnection.set(bqc)
                println("Got QueueConnection " + bqc.toString())
            } else
            //just return existing thread local version
                bqc = browserQconnection.get()

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        bqc
    }

    /**
     * do i need to start q to peek at it ??  - needs a test
     * need to start a connection before you can get messages from it
     */
    void browserStart () {

        if (!browserQconnection.get()) {
            createBrowserQueueConnection()
        }
        println("start browser QueueConnection on ${browserQconnection.get()}" )

        browserQconnection.get()?.start()
    }

    void browserStop() {
        println("stop browser QueueConnection " )

        browserQconnection.get()?.stop()
    }

    /**
     * creates a new QueueSession from appropriate QueueConnection for either browsing a queue.
     * Stores browser session in thread local variable
     *
     * @param qc - optional queue connection passed from parent
     *
     * @return
     */

    QueueSession createBrowserQueueSession(QueueConnection qc = null) {
        // create QueueSession
        QueueSession qs
        try {
            //if no sender queue connection - then build one
            if (!browserQconnection.get()) {
                println "createBrowserQueueSession: no existing  Q connection for thread - create one "
                if (qc == null) {
                    //invoke the impl class parent service to build queueConnection for sender
                    browserQconnection.set(createReceiverQueueConnection())
                } else
                    browserQconnection.set(qc)
            }
            if (!browserQsession.get()) {
                // set thread local session
                println("createBrowserQueueSession: no QueueSession created one for thread - " + qs.toString())
                browserQsession.set(qs = browserQconnection.get().createQueueSession(false, Session.AUTO_ACKNOWLEDGE))
            }else {
                //just return existing thread local Q session
                qs = browserQsession.get()
            }

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        qs
    }

    QueueReceiver createQueueBrowser(Queue q) {

        QueueBrowser browser
        if (!browserQsession.get())
            browserQsession.set (createBrowserQueueSession())

        // create QueueBrowser from the receiver session
        try {
            def qbsession = browserQsession.get()

            if (!qBrowser.get()) {
                qBrowser.set(browser = qbsession.createBrowser(q))
                println("Got QueueBrowser " + browser.toString())
            } else {
                browser = qBrowser.get()
            }

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        browser
    }

    /**
     * delegates to browse to get list and then calls size()
     * @return
     */
    int browseQueueSize (Queue queueName = null) {
        def browser
        if (!qBrowser.get()) {

            println("browseQueue, no browser defined - create one  " )

            Queue q
            String qname
            if (queueName == null) {
                qname = this.operatingEnv.get('orderQueue') ?: this.DEFAULT_QUEUE
            }
            q = this.getQueue(qname)

            browser = createQueueBrowser (q)
            qBrowser.set (browser)

        }

        browser = qBrowser.get()

        println("browsing Queue " + browser.getQueue().queueName)
        Enumeration list =  browse(browser)
        //not sure what order asc/desc
        list?.toList().size() ?: 0
    }


    /**
     * delegates to browse (browser) using stored thread local
     *
     * @return toString on the message
     */
    String browseTopOfQueue (Queue queueName = null) {
        def browser
        if (!qBrowser.get()) {

            println("browseQueue, no browser defined - create one  " )

            Queue q
            String qname
            if (queueName == null) {
                qname = this.operatingEnv.get('orderQueue') ?: this.DEFAULT_QUEUE
            }
            q = this.getQueue(qname)

            browser = createQueueBrowser (q)
            qBrowser.set (browser)

        }


        browser = qBrowser.get()

        println("browsing Queue " + browser.getQueue().queueName)
        Enumeration list =  browse(browser) as TextMessage
        //not sure what order asc/desc
        Message m
        if (list?.hasMoreElements()) {
            m = list.nextElement()
        } else
            return null //

        def body = m?.getBody (Object)
        body?.toString()
    }

    /**
     * delegates to browse (browser) using stored thread local
     *
     * @return toString on the message
     */
    String browseTopOfQueue (QueueBrowser browser) {
        def queueBrowser

        if (browser == null) {
            queueBrowser = qBrowser.get()
        }

        println("browsing Queue " + queueBrowser.getQueue().queueName)
        Enumeration list =  browse(queueBrowser)
        //not sure what order asc/desc
        Message m
        if (list?.hasMoreElements()) {
            m = list.nextElement()
        } else
            return null //

        def body = m?.getBody (Object)
        body?.toString()
    }

    /**
     * get list of messages from Queue using the referencing qBrowser
     * @param qBrowser
     * @return
     */
    Enumeration<Message> browse(QueueBrowser qBrowser) {
        def text
        Enumeration list
        try {
            println("receive : use  " + qBrowser.toString() + " to read from Q : " + qBrowser?.getQueue().queueName)

            list = qBrowser?.getEnumeration ()
        }
        catch (JMSException jmse) {
            tidyUpBrowser()
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        if (list == null) {
            list = []
        }
        return list
    }


    def tidyUpBrowser () {
        qBrowser.set(null)
        browserQconnection.get().stop()

        browserQsession.get()?.close()
        browserQconnection.get()?.close()
        browserQsession.set (null)
        browserQconnection.set (null)
    }
}
