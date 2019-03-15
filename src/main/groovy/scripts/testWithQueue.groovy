package scripts

import com.softwood.client.MessagePlatformFactoryProducer
import com.softwood.client.MessageSystemClient
import com.softwood.implementation.JmsConnectionType


MessageSystemClient mclient = MessagePlatformFactoryProducer.getFactory().getMessagePlatformInstance("WLS")

Closure work = {it ->

    sendText ("hello")
    sendtext ("william")
    sendText ("have a great day")
    println "all done 3 messages sent "
}
def q = mclient.getQueue (mclient.getPlatformEnvironmentProperty('orderQueue'))

mclient.withQueue(JmsConnectionType.Sender, q, work)

println "exiting... "