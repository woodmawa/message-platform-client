package scripts

import com.softwood.client.MessagePlatformFactoryProducer
import com.softwood.client.MessageSystemClient

MessageSystemClient mclient = MessagePlatformFactoryProducer.getFactory().getMessagePlatformInstance("WLS")

mclient.receiverStart()
def result = mclient.receiveText ()

println "read [$result] from queue"

mclient.tidyUpSender()
