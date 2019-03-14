package com.softwood.cli

import com.softwood.client.AbstractMessagePlatformFactory
import com.softwood.client.MessagePlatformFactoryProducer
import com.softwood.client.MessageSystemClient
import com.softwood.implementation.JmsConnectionType
import com.softwood.implementation.MessagePlatformFactory
import com.softwood.implementation.WlsJmsMessagePlatform
import groovy.cli.Option
import groovy.cli.Unparsed
import groovy.cli.picocli.CliBuilder
import picocli.CommandLine

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

/**
 * simple command line driver for fat jar functions
 * cortex can call jar with params to execute single show action
 */

class Launcher {

    static class CliOptions {

        @Option (shortName = 'h', longName ='help', description = 'executable jar,  help ')
        boolean help

        @Option (shortName = 's', longName ='send', description = 'send message to jms Queue')
        String sendText

        //@Option (paramLabel='Text', description = 'text to send to queue ')
        //String text

        @Option (shortName = 'r', longName = 'receive',  description = 'read message from jms Queue')
        boolean receive

        @Option (shortName = 'e', longName = 'execute', description = 'run script file as closure, will be passed JMS session as param')
        File script

        @Unparsed (description = 'positional parameters')
        List remaining
    }

    static MessageSystemClient mclient

    static void main (args) {

        mclient = MessagePlatformFactoryProducer.getFactory().getMessagePlatformInstance("WLS")
        //mclient = getWlsPlatform()

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
        def cli = new CliBuilder(usage: 'java -jar file [<Options>]')
        cli.width = 80 //default is 74
        cli.parseFromInstance(options, args)

        def message

        if (options.help ) {
            cli.usage()
        } else {
            if ((message = options.sendText)) {
                println "send message : $message"
                send(message)
            }
            if (options.receive) {
                message = receive()
                println "read message : $message, passsed Q: $options.receive"
            }
            if (options.script) {

                File f = new File (".")
                println f.canonicalPath
                println options.script.canonicalPath
                Path path = FileSystems.getDefault().getPath(".").toAbsolutePath()
                Path crpath = Paths.get("")
                String s = crpath.toAbsolutePath().toString();
                println "user dir  : " + System.getProperty("user.dir")
                //Path path2 = FileSystems.getPath("").toAbsolutePath()

                println "def path " + path.toString() + " cur rel path: $s"

                //todo - read file from command line, generate a closure from it and execute with withQueue action
                if (!options.script.exists()) {
                    throw new FileNotFoundException("script file ${options.script} doesnt exist")
                    System.exit(-1)
                }

                String text = options.script.text


            }
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
