package com.softwood.implementation

import com.softwood.client.AbstractMessagePlatformFactory
import com.softwood.client.MessageSystemClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

    Map env = System.getenv()
    ConfigSlurper slurper

    private final Logger log = LoggerFactory.getLogger(this.getClass())

    MessageSystemClient getMessagePlatformInstance (String messagePlatformType) {
        slurper = new ConfigSlurper()
        String providerUrl, senderCredentials, receiverCredentials, browserCredentials
        String protocol, hostname, port

        ClassLoader classLoader = getClass().getClassLoader()
        // https://stackoverflow.com/questions/25362943/cant-load-resources-file-from-jar
        InputStream configStream = classLoader.getResourceAsStream("ApplicationConfig.groovy")

        if (!configStream) {
            throw new FileNotFoundException("Cant find application configuration file : ApplicationConfig.groovy")
        }
        String configText = configStream.text

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
                senderCredentials = env.get("SENDER_SECURITY_CREDENTIALS")
                receiverCredentials = env.get("RECEIVER_SECURITY_CREDENTIALS")
                browserCredentials = env.get("BROWSER_SECURITY_CREDENTIALS")
                protocol = env.get("JMS_PROTOCOL")
                hostname = env.get("JMS_HOSTNAME")
                port = env.get("JMS_PORT")

                //having read environment for security credentials - now set up
                //as lower cased variables in the environment context.
                //if null setup a hard coded default -- should probably throw an error though
                if (!senderCredentials) {
                    log.debug ("SENDER_SECURITY_CREDENTIALS not set in environment - setting a default")
                    wls.put('senderSecurityCredentials', "testSender1")
                } else
                    wls.put ('senderSecurityCredentials', senderCredentials)
                if (!receiverCredentials) {
                    log.debug ("RECEIVER_SECURITY_CREDENTIALS not set in environment - setting a default")
                    wls.put ('receiverSecurityCredentials', "testReceiver1")
                } else
                    wls.put ('receiverSecurityCredentials', receiverCredentials)
                if (!browserCredentials) {
                    log.debug ("BROWSER_SECURITY_CREDENTIALS not set in environment - setting a default")
                    wls.put ('browserSecurityCredentials', "testBrowser1")
                } else
                    wls.put ('browserSecurityCredentials', browserCredentials)
                //if environment variables present use these as default
                if (!hostname) {
                    wls.put('hostname', hostname)
                }
                if (!port) {
                    wls.put('port', hostname)
                }
                if (!protocol) {
                    wls.put('protocol', protocol)
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
