package com.softwood.implementation

import com.softwood.client.AbstractMessagePlatformFactory
import com.softwood.client.MessageSystemClient

class NoMessagePlatformImplementation extends Exception {
    String message

    NoMessagePlatformImplementation (String message) {
        message  = "No such message platform factory exists for : $message"
    }
}

/**
 * Factory class to generate the appropriate messaging platform client wrapper
 */
class MessagePlatformFactory implements AbstractMessagePlatformFactory {

    Properties env = System.getProperties()
    ConfigSlurper slurper

    MessageSystemClient getMessagePlatformInstance (String messagePlatformType) {
        slurper = new ConfigSlurper()

        ClassLoader classLoader = getClass().getClassLoader()
        File configFile = new File(classLoader.getResource("ApplicationConfig.groovy").getFile())

        String configText = configFile.text

        File absPath =  configFile.getAbsoluteFile()

        def url = new URL ("file:${configFile}")

        //slurper.parse(configText)

        slurper.parse """host = 'localhost'"""

        def val = slurper.getProperty("host")

        switch (messagePlatformType?.toUpperCase()) {
            case "WLS" :
            case "WEBLOGIC" :
                def mp = slurper.messagePlatform
                Map wls = slurper.messagePlatform.weglogic
                String providerUrl = "${wls.protocol}://${wls.hostname}:${wls.port}"
                String defaultProviderUrl = wls.defaultProviderUrl
                if (!env.getProperty("senderSecurityCredentials")) {
                    wls.put('senderSecurityCredentials', "testSender")
                }
                if (!env.getProperty("receiverSecurityCredentials")) {
                    wls.put ('receiverSecurityCredentials', "testReceiver")
                }
                return new WlsJmsMessagePlatform(wls)
            case "MQ" :
            case "ACTIVEMQ" :
                return new ActiveMqJmsMessagePlatform()
            case "AMQP" : return new AmqpMessagePlatform()
            default: throw new NoMessagePlatformImplementation (messagePlatformType)
        }
        return null

    }
}
