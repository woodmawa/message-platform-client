package com.softwood.cli

import com.softwood.client.AbstractMessagePlatformFactory
import com.softwood.client.MessagePlatformFactoryProducer
import com.softwood.client.MessageSystemClient

class Launcher {

    static void main (args){

        //get factory from the factoryProducer and get messagePlatform client instance from it
        MessageSystemClient mclient = MessagePlatformFactoryProducer.getFactory().getMessagePlatformInstance("WLS")

        def action, message
        if (args.size() == 0) {
            action = 'send'
            mclient.send("'empty': test message")
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
}
