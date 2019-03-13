package com.softwood.cli

import com.softwood.client.AbstractMessagePlatformFactory
import com.softwood.client.MessagePlatformFactoryProducer
import com.softwood.client.MessageSystemClient
import com.softwood.implementation.JmsConnectionType

/**
 * simple command line driver for fat jar functions
 * cortex can call jar with params to execute single show action
 */
class Launcher {

    static MessageSystemClient mclient = MessagePlatformFactoryProducer.getFactory().getMessagePlatformInstance("WLS")

    static void main (args){

        //get factory from the factoryProducer and get messagePlatform client instance from it

        String action, message
        if (args.size() == 0) {
            action = 'send'

            System.exit(0)
        }
        else {
            action = args[0]
            if (args.size() >= 2)
                message = args[1] ?: "default"
        }

        String selector
        if (action.substring(0,1) == '--') {  //long form
            def arg = action.substring(2)
            if (arg.contains ('sen')) {
                selector = 's'
            } else if (arg.contains ('rec')) {
                selector = 'r'
            } else if (arg.contains ('ex')) {
                selector = 'e'
            }
        } else if (action[0] == '-') {  //short form
            selector = action[1]  //take next letter

        } else

            switch (selector.toLowerCase()) {
            case 's':
                //for send action
                send(message)
                break
            case 'r':
                //for receive or collect
                message = receive()
                println "read message from queue : [$message]"
                break
            case 'e':
                //for execute a script closure
                //todo - read file from command line, generate a closure from it and execute with withQueue action

            default :
                println "args usage: '-s --send [message]' or '-r --receive' or -e --execute fileScript"
                break
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
