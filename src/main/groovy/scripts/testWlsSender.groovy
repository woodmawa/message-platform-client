package scripts

import com.softwood.client.MessagePlatformFactoryProducer
import com.softwood.client.MessageSystemClient
import com.softwood.implementation.JmsConnectionType

MessageSystemClient mclient = MessagePlatformFactoryProducer.getFactory().getMessagePlatformInstance("WLS")

mclient.sendText("hello world")

mclient.tidyUpSender()

