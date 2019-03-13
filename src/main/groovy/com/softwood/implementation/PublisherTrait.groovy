package com.softwood.implementation

import javax.jms.JMSException
import javax.jms.Message
import javax.jms.Queue
import javax.jms.QueueConnection
import javax.jms.QueueConnectionFactory
import javax.jms.QueueSender
import javax.jms.QueueSession
import javax.jms.Session
import javax.jms.Topic
import javax.jms.TopicConnection
import javax.jms.TopicConnectionFactory
import javax.jms.TopicPublisher
import javax.jms.TopicSession
import javax.naming.NamingException


/**
 *
 * reusable trait for basic controlled JMS features that based on feature groups
 *
 * this is the constrained JMS publisher feature set
 *
 * relies on its parent implementation class, for environment variables, and queue/topic lookups
 *
 * @Author Will Woodman
 * @Copyright Softwood Consulting ltd, 2019
 *
 */
trait PublisherTrait {

    //make sender queue connection public in the implementing class - expect impl class to set this
    ThreadLocal<TopicConnection> publisherTconnection = new ThreadLocal<>()

    private ThreadLocal<TopicSession> publisherTsession = new ThreadLocal<>()
    private  ThreadLocal<TopicPublisher> tPublisher = new ThreadLocal<>()


    /**
     * use topic connection factory to create TopicConnection object for publisher using appropriate
     * security principal and credentials.  topic connection created is also stored in a threadlocal
     * local variable for publisher or subscriber
     *
     *
     * @param type - Enum of either Sender or Receiver determines which security principal/credential are used
     * @return new QueueConnection - used to create queue related objects
     */
    TopicConnection createPublisherTopicConnection () {

        // call dependency on implenting parent, expecting map of key message platform variables
        Map env = this.getPlatformEnvironment()

        TopicConnection tc
        //queue connection factor should have been setup by parent's implementing class constructor
        if (!tcf) {
            try {
                tcf = (TopicConnectionFactory)ctx.get().lookup(TCF_NAME)
            }
            catch (NamingException ne) {
                ne.printStackTrace(System.err)
                System.exit(0)
            }
        }

        String publisherPrinciple
        String publisherCredentials
        try {
            if (!publisherTconnection.get()) {
                //set thread local connection {

                publisherPrinciple = env.get('mvaPublisherSecurityPrincipal')
                publisherCredentials = env.get('mvaPublisherSecurityCredentials')
                tc = qtf.createTopicConnection(publisherPrinciple, publisherCredentials)
            } else
                tc = publisherTconnection.get()

        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        tc
    }

    /**
     * creates a new TopicSession from appropriate TopicConnection for  a publisher .  creates
     * auto acknowledge sessions by default.   Stores publisher session in thread local variable
     *
     * @param qc - optional queue connection passed from parent
     *
     * @return
     */

    TopicSession createPublisherSession (TopicConnection tc = null) {
        // create QueueSession
        TopicSession ts
        try {
            //if no sender queue connection - then build one
            if (!publisherTconnection.get()) {
                if (tc == null) {
                    //invoke the impl class parent service to build queueConnection for sender
                    publisherTconnection.set(createSenderQueueConnection ())
                } else
                    publisherTconnection.set (tc)
            }

            if (!publisherTsession.get()) {
                // set thread local session
                publisherTsession.set(ts = publisherTconnection.get().createQueueSession(false, Session.AUTO_ACKNOWLEDGE))
            } else
                ts = publisherTsession.get()
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        ts
    }

    /**
     * create a publisher
     * @param t - topic instance that publisher will publish to
     * @return publisher
     */
    TopicPublisher createTopicPublisher (Topic t) {
        // create TopicPublisher
        TopicPublisher publisher
        if (!publisherTsession.get())
            publisherTsession.set (createPublisherSession())

        try {
            if (!tPublisher.get()) {
                tPublisher.set(publisher = PublisherTsession.get().createPublisher(t))
            } else
                publisher = tPublisher.get()
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
        publisher
    }

    void publish(Message message) {
        // send message - use the default qSender from thread local
        // if not set create one and save in thread local
        if (!tPublisher.get()) {
            String tname = this.operatingEnv.get('orderTopic')  ?: this.DEFAULT_TOPIC
            Topic t = this.getTopic (tname)
            tPublisher.set (createTopicPublisher (t))
        }

        try {
            tPublisher.get().publish (message)
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }

    }

    void publish (TopicPublisher tPublisher, Message message) {
        // send message
        try {
            if (!tPublisher)
                throw new JMSException("missing QueueSender, received null")
            else
                tPublisher?.publish(message)
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
    }

    void publishText (String text) {

        if (!publisherTsession.get()) {
            createPublisherSession()
        }

        Message message
        try {
            message = publisherTsession.get().createTextMessage()
            message.setText (text)
            publish (message)
        }
        catch (JMSException jmse) {
            jmse.printStackTrace(System.err)
            System.exit(0)
        }
    }

    def tidyUpPublisher () {
        publisherTsession.get()?.close()
        publisherTconnection.get()?.close()
        publisherTsession.set (null)
        publisherTconnection.set (null)
    }
}
