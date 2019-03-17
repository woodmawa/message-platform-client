package com.softwood.temp

import weblogic.jndi.Environment

import javax.jms.*
import javax.naming.Context
import javax.naming.NamingException

class JmsBrowser {
    private static Context ctx
    static Environment env
    private static QueueConnectionFactory qcf = null
    private static QueueConnection qc = null
    private static QueueSession qsess = null
    private static Queue q = null
    private static QueueBrowser qbrwsr = null
    private static TextMessage message = null
    // NOTE: The next two lines set the name of the Queue Connection Factory
    //       and the Queue that we want to use.
    private static final String QCF_NAME = "jms/queueConnectionFactory"
    private static final String QUEUE_NAME = "jms/workOrderQueue"

    JmsBrowser() {
        super()
    }

    public static def collectMessage() {
        // create InitialContext
        Hashtable properties = new Hashtable();
        properties.put(Context.INITIAL_CONTEXT_FACTORY,
                "weblogic.jndi.WLInitialContextFactory");

        env = new Environment()
        env.setProviderUrl("t3://localhost:7001")
        env.setSecurityPrincipal("cortex")
        env.setSecurityCredentials("Belgareth1xex49is")
        try {
            ctx =  env.getInitialContext()//new InitialContext(properties)
        } catch (NamingException ne) {
            ne.printStackTrace(System.err)
            System.exit(0)
        }

        println("Got InitialContext " + ctx.toString());
        // create QueueConnectionFactory
        try {
            qcf = (QueueConnectionFactory)ctx.lookup(QCF_NAME)
        }
        catch (NamingException ne) {
            ne.printStackTrace(System.err)
            System.exit(0)
        }

        println("Got QueueConnectionFactory " + qcf.toString());
        // create QueueConnection
        try {
            qc = qcf.createQueueConnection();
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err);
            System.exit(0);
        }

        println("Got QueueConnection " + qc.toString());
        // create QueueSession
        try {
            qsess = qc.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err);
            System.exit(0);
        }

        println("Got QueueSession " + qsess.toString());

        // lookup Queue
        try {
            q = (Queue) ctx.lookup(QUEUE_NAME);
        }
        catch (NamingException ne) {
            ne.printStackTrace(System.err);
            System.exit(0);
        }

        println("Got Queue " + q.toString())
        // create QueueReceiver
        try {
            qsess.createReceiver(q)
            qbrwsr = qsess.createBrowser(q)
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0);
        }


        // start the connection
        qc.start()

        println("Got QueueBrowser " + qbrwsr.toString())
        // create TextMessage
        def text
        int numMsgs = 0
        Enumeration e = qbrwsr.getEnumeration()
        try {

            while (e.hasMoreElements()) {
                Message message = (Message) e.nextElement()
                numMsgs++
                println "message entry [$numMsgs] : " + message?.text
            }
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }

        println("queue has [$numMsgs] unread entries ")
        // clean up
        try {
            message = null
            qbrwsr.close()
            qbrwsr = null
            q = null
            qsess.close()
            qsess = null
            qc.close()
            qc = null
            qcf = null
            ctx = null
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
        }

        println("Cleaned up and done.")
        return text
    }

}