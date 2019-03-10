package com.softwood.client

import com.softwood.implementation.MessagePlatformFactory

/**
 * factory producer to return a factory to a client
 */
final class MessagePlatformFactoryProducer {
    static AbstractMessagePlatformFactory getFactory () {
        return new MessagePlatformFactory ()
    }
}
