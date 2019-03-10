package scripts

import com.softwood.client.MessagePlatformFactoryProducer
import com.softwood.client.MessageSystemClient

MessageSystemClient mclient = MessagePlatformFactoryProducer.getFactory().getMessagePlatformInstance("WLS")

mclient.send("hello world")

mclient.tidyUp()

