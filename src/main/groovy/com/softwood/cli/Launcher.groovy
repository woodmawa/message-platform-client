package com.softwood.cli

import com.softwood.client.AbstractMessagePlatformFactory
import com.softwood.client.MessagePlatformFactoryProducer
import com.softwood.client.MessageSystemClient
import com.softwood.implementation.JmsConnectionType

class Launcher {

    static MessageSystemClient mclient = MessagePlatformFactoryProducer.getFactory().getMessagePlatformInstance("WLS")

    static void main (args){

        //get factory from the factoryProducer and get messagePlatform client instance from it

        def action, message
        if (args.size() == 0) {
            action = 'send'

            System.exit(0)
        }
        else {
            action = args[0]
            if (args.size() >= 2)
                message = args[1] ?: "default"
        }

        switch (action[0].toLowerCase()) {
            case 's':
                mclient.send(message)
                break
            case 'r':
            case 'c':
                message = mclient.receive()
                println "read message from queue : [$message]"
                break
            default :
                println "args usage: 'send [message]' or 'receive'"
                break
        }
    }

    static def send (String text) {
        mclient.sendText("hello world")
        mclient.tidyUpSender()
    }

    static def withQueue (Closure work) {
        def q = mclient.getQueue ('jsm/workOrderQueue')
        mclient.withQueue (JmsConnectionType.Sender, q, work)
     }

    static def receive () {
        mclient.receiverStart()
        def result = mclient.receiveText ()

        println "read [$result] from queue"
    }

    static def withTopic (Closure script) {
        def q = mclient.getTopic ('jms/workOrderTopic')
        mclient.withQueue (JmsConnectionType.Sender, q, work)
    }



}
