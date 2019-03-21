package com.softwood.implementation

import org.slf4j.Logger

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

    ThreadLocal<QueueConnection> browserQconnection = new ThreadLocal<>()
    ThreadLocal<QueueSession> browserQsession = new ThreadLocal<>()
    private ThreadLocal<QueueBrowser> qBrowser = new ThreadLocal<>()

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
    QueueConnection createBrowserQueueConnection() {

        // call dependency on implenting parent, expecting map of key message platform variables
        Map env = this.getPlatformEnvironment()

        QueueConnection bqc
        //queue connection factor should have been setup by parent's implementing class constructor
        if (!qcf) {
            try {
                qcf = (QueueConnectionFactory) ctx.lookup(QCF_NAME)
                log.debug ("createBrowserQueueConnection: Got QueueConnectionFactory " + qcf.toString())

            }
            catch (NamingException ne) {
                ne.printStackTrace(System.err)
                System.exit(0)
            }
        }

        String browserPrinciple
        String browserCredentials
        try {
            //set thread local connection {

            if (!browserQconnection.get()) {
                log.debug "createBrowserQueueConnection:createBrowserQueueConnection: no Q connection - so create one "

                browserPrinciple = env.get('browserSecurityPrincipal')
                browserCredentials = env.get('browserSecurityCredentials')
                bqc = qcf.createQueueConnection(browserPrinciple, browserCredentials)
                browserQconnection.set(bqc)
                log.debug ("createBrowserQueueConnection:Got browser QueueConnection " + bqc.toString())
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
                log.debug "createBrowserQueueSession:createBrowserQueueSession: no existing  Q connection for thread - create one "
                if (qc == null) {
                    //invoke the impl class parent service to build queueConnection for sender
                    browserQconnection.set(createReceiverQueueConnection())
                } else
                    browserQconnection.set(qc)
            }
            if (!browserQsession.get()) {
                // set thread local session
                log.debug ("createBrowserQueueSession:createBrowserQueueSession: no QueueSession created one for thread - " + qs.toString())
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

    QueueBrowser createQueueBrowser(Queue q) {

        QueueBrowser browser
        if (!browserQsession.get())
            browserQsession.set (createBrowserQueueSession())

        // create QueueBrowser from the receiver session
        try {
            def qbsession = browserQsession.get()

            if (!qBrowser.get()) {
                qBrowser.set(browser = qbsession.createBrowser(q))
                log.debug ("createQueueBrowser:Got QueueBrowser " + browser.toString())
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
    int browseQueueSize (Queue queue) {
        def browser
        if (!qBrowser.get()) {

            log.debug ("browseQueueSize:browseQueue, no browser defined - create one  " )

            Queue q = queue

            browser = createQueueBrowser (q)
            qBrowser.set (browser)

        }

        browser = qBrowser.get()

        log.debug ("browseQueueSize:browsing Queue " + browser.getQueue().queueName)
        Enumeration list =  browse(browser)
        //not sure what order asc/desc
        list?.toList().size() ?: 0
    }


    /**
     * delegates to browse to get list and then calls size()
     * @return
     */
    int browseQueueSize (String queueName = null) {
        def browser
        if (!qBrowser.get()) {

            log.debug ("browseQueueSize: browseQueueSize, no browser defined - create one  " )

            Queue q
            String qname
            if (queueName == null) {
                qname = this.operatingEnv.get('orderQueue') ?: this.DEFAULT_QUEUE
            } else
                qname = queueName

            q = this.getQueue(qname)

            browser = createQueueBrowser (q)
            qBrowser.set (browser)

        }

        browser = qBrowser.get()

        log.debug ("browseQueueSize:browsing Queue " + browser.getQueue().queueName)
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

            log.debug ("browseTopOfQueue:browseQueueSize, no browser defined - create one  " )

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

        log.debug ("browseTopOfQueue: browsing Queue " + browser.getQueue().queueName)
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

        log.debug ("browseTopOfQueue: browsing Queue " + queueBrowser.getQueue().queueName)
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
    List<Message> browse(QueueBrowser qBrowser) {
        def text
        Enumeration list
        try {
            log.debug ("browse: using  " + qBrowser.toString() + " to read from Q : " + qBrowser?.getQueue().queueName)

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
        return list?.toList()
    }

    /**
     * get list of messages from Queue using the referencing queue
     * @param queue to browse
     * @return Enumeration<Message>
     */
    List<Message> browse(Queue queue) {

        if (!qBrowser.get()){
            QueueSession qs = createBrowserQueueSession(browserQconnection.get())
            QueueBrowser qb = qs.createBrowser (queue)
            qBrowser.set(qb)
        } else if (queue != qBrowser.get()?.getQueue()) {
            //override the cached thread local
            QueueSession qs = createBrowserQueueSession(browserQconnection.get())
            QueueBrowser qb = qs.createBrowser (queue)
            qBrowser.set(qb)
        }

        def text
        Enumeration elist
        try {
            log.debug("browse : use  " + qBrowser.toString() + " to read from Q : " + qBrowser?.get()?.getQueue().queueName)

            elist = qBrowser.get()?.getEnumeration ()
        }
        catch (JMSException jmse) {
            tidyUpBrowser()
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        if (elist == null) {
            elist = []
        }
        return elist?.toList()
    }

    /**
     * get list of messages from Queue using the queueName
     * @param queue to browse
     * @return Enumeration<Message>
     */
    List<Message> browse(String queueName = null) {

        Queue queue
        Queue qbrowser
        if (queueName) {
            queue = this.getQueue(queueName)
            qbrowser = createQueueBrowser(queue)
        } else {
            //assume default
            if (qBrowser)
                qbrowser = qBrowser.get()
            else {
                //if default qBrowser not yet set - define one

                queueName = getPlatformEnvironmentProperty.get ('DEFAULT_QUEUE')
                queue = this.getQueue(queueName)
                qBrowser.set (createQueueBrowser(queue))
            }
        }


        def text
        List elist
        try {
            log.debug ("browse : ${qbrowser.getQueueName()} " )

            elist = browse(qbrowser)
        }
        catch (JMSException jmse) {
            tidyUpBrowser()
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        if (elist == null) {
            elist = []
        }
        return elist
    }

    def tidyUpBrowser () {
        qBrowser.set(null)

        browserQsession.get()?.close()
        browserQconnection.get()?.close()
        browserQsession.set (null)
        browserQconnection.set (null)
    }
}
