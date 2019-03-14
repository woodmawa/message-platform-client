package com.softwood.cli

import com.softwood.client.AbstractMessagePlatformFactory
import com.softwood.client.MessagePlatformFactoryProducer
import com.softwood.client.MessageSystemClient
import com.softwood.implementation.JmsConnectionType
import groovy.cli.Option
import groovy.cli.Unparsed
import groovy.cli.picocli.CliBuilder
import picocli.CommandLine

/**
 * simple command line driver for fat jar functions
 * cortex can call jar with params to execute single show action
 */

class Launcher {

    static class CliOptions {
        @Option (shortName = 's', longName ='send', description = 'send message to jms Queue')
        String sendText

        //@Option (paramLabel='Text', description = 'text to send to queue ')
        //String text

        @Option (shortName = 'r', longName = 'receive', description = 'read message from jms Queue')
        boolean receive

        @Option (shortName = 'e', longName = 'execute', description = 'run script file as closure, will be passed JMS session as param')
        File script

        @Unparsed (description = 'positional parameters')
        List remaining
    }

    static MessageSystemClient mclient = MessagePlatformFactoryProducer.getFactory().getMessagePlatformInstance("WLS")

    static void main (args){



        //println "howdi "
        //System.exit(-1)
        /*def cli = new CliBuilder(name: 'jmsClient',
                usage:'java -jar message-platform-client [<options>]',
                header: 'Options:',
                footer: "Softwood Consulting Ltd")
        cli.width = 80 //default is 74
        cli.with {
            s (longOpt: 'send', type: String, 'send message to jms Queue')
            r (longOpt: 'receive', type: Boolean, 'read a message from jms Queue')
            e (longOpt: 'execute', type: File, 'groovy script file to be called as a closure, and passed JMS Session ')
            //D (args:2, valueSeperator: '=', argName: 'property=value', 'Use value for givenm property')
        }

        def options = cli.parse(args)*/


        CliOptions options = new CliOptions()
        def cli = new CliBuilder (usage: 'java -jar file [<Options>]')
        cli.parseFromInstance (options, args)

        def message

        if ((message = options.sendText)) {
            send(message)

        }
        if (options.receive) {
            message = receive()
            println "read message : $message"
        }
        if (options.script) {
            //todo - read file from command line, generate a closure from it and execute with withQueue action

        }

    }


    static def send (String text) {
        mclient.sendText("hello world")
        mclient.tidyUpSender()
    }

    static def withSenderQueue (Closure work) {
        def q = mclient.getQueue (mclient.getPlatformEnvironmentProperty('orderQueue'))
        mclient.withQueue (JmsConnectionType.Sender, q, work)
     }

    static def withReceiverQueue (Closure work) {
        def q = mclient.getQueue (mclient.getPlatformEnvironmentProperty('orderQueue'))
        mclient.withQueue (JmsConnectionType.Receiver, q, work)
    }

    static def receive () {
        mclient.receiverStart()
        def result = mclient.receiveText ()

        mclient.tidyUpReceiver()
        result
    }

    static def withPublisherTopic (Closure work) {
        def t = mclient.getTopic (mclient.getPlatformEnvironmentProperty('orderTopic'))
        mclient.withQueue (JmsConnectionType.Publisher, t, work)
    }

    static def withSubscriberTopic (Closure work) {
        def t = mclient.getTopic (mclient.getPlatformEnvironmentProperty('orderTopic'))
        mclient.withQueue (JmsConnectionType.Subscriber, t, work)
    }

}
