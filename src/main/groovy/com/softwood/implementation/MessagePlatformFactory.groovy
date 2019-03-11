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
        String providerUrl, senderCredentials, receiverCredentials

        ClassLoader classLoader = getClass().getClassLoader()
        File configFile = new File(classLoader.getResource("ApplicationConfig.groovy").getFile())

        String configText = configFile.text

        File absPath =  configFile.getAbsoluteFile()

        def url = new URL ("file:${configFile}")

        //https://stackoverflow.com/questions/55092121/cant-get-groovy-configslurper-to-parse-a-string-and-find-result-as-property/55093357#55093357
        def config = slurper.parse(configText)


        switch (messagePlatformType?.toUpperCase()) {
            case "WLS" :
            case "WEBLOGIC" :
                def mp = config.messagePlatform
                Map wls = config.messagePlatform.weblogic
                if (wls)
                    providerUrl = "${wls?.protocol ?: ''}://${wls?.hostname ?: 'localhost'}:${wls?.port ?: '7001'}"
                else
                    providerUrl = "invalid"
                String defaultProviderUrl = wls.defaultProviderUrl
                senderCredentials = env.getProperty("MVA_SENDER_SECURITY_CREDENTIALS")
                receiverCredentials = env.getProperty("MVA_RECEIVER_SECURITY_CREDENTIALS")
                if (!senderCredentials) {
                    wls.put('mvaSenderSecurityCredentials', "testSender1")
                } else
                    wls.put ('mvaSenderSecurityCredentials', senderCredentials)
                if (!receiverCredentials) {
                    wls.put ('mvaReceiverSecurityCredentials', "testReceiver1")
                } else
                    wls.put ('mvaReceiverSecurityCredentials', receiverCredentials)
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
