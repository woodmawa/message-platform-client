package com.softwood.cli


import com.softwood.client.MessagePlatformFactoryProducer
import com.softwood.client.MessageSystemClient
import com.softwood.implementation.JmsConnectionType
import groovy.cli.Option
import groovy.cli.Unparsed
import groovy.cli.picocli.CliBuilder

import javax.jms.Message
import javax.jms.Queue
import java.nio.file.Paths
import java.util.regex.Matcher

/**
 * simple command line driver for fat jar functions
 * cortex can call jar with params to execute single show action
 */

class Launcher {

    static class CliOptions {

        @Option (shortName = 'h', longName ='help', description = 'executable jar,  help ')
        boolean help

        @Option (shortName = 's', longName ='send', description = 'send text message (enclose param in "")  to DEFAULT_QUEUE jms Queue')
        String sendText

        //@Option (paramLabel='Text', description = 'text to send to queue ')
        //String text

        //to get optionality of arg val or not indicate it can be optional and make it attribut be a List.
        // if arg not present - list be null, if arg is present, it will be empty list or have the values
        @Option (shortName = 'r', longName = 'receive',  numberOfArguments = 1, optionalArg = true,  description = 'read message from jms Queue')
        List<String> receiveQ

        @Option (shortName = 'es', longName = 'execute-sender', description = "run script file as closure, will be passed JMS Sender session as 'it' param.  Default scripts backup is at ~/.scripts if script cant be found ")
        File sscript

        @Option (shortName = 'er', longName = 'execute-receiver', description = "run script file as closure, will be passed JMS Receiver session as 'it' param.  Default scripts backup is at ~/.scripts if script cant be found ")
        File rscript

        @Option (shortName = 'q', longName = 'queue', description = "override the default Queue - 'jms/workOrderQueue' setup in config.  provide the name, and software  will do a lookup and use this Queue instead ")
        String queueName

        @Option (shortName = 't', longName = 'topic', description = "override the default Topic - 'jms/workOrderTopic' setup in config.  provide the name sofwaree  will do a lookup and use this Queue instead ")
        String topicName

        @Option (shortName = 'c', longName = 'credentials', description = "TODO : credentials required to validate script caller can execute actions on the command line  ")
        String credentials

        @Option (shortName = 'br', longName = 'browse', numberOfArguments = 1, optionalArg = true, description = "browse queue will return enumeration of entries on the queue - if no queue named then DEFAULT_QUEUE is browsed ")
        List<String> browseQueue

        @Option (shortName = 'qs', longName = 'queue-size', numberOfArguments = 1, optionalArg = true, description = "browse named queue and return the size of the messages on the queue - if no queueName provided will default to queue size for DEFAULT_QUEUE")
        List<String> browseQueueName

        @Unparsed (description = 'positional parameters')
        List remaining
    }

    static MessageSystemClient mclient

    static void main (args) {

        /*def cli = new CliBuilder(name: 'jmsClient',
                usage:'java -jar message-platform-client [<options>]',
                header: 'Options:',
                footer: "Softwood Consulting Ltd")
        cli.width = 80 //default is 74
        cli.with {
            s (longOpt: 'send', type: String, 'send message to jms Queue')
            r (longOpt: 'receive', type: Boolean, 'read a message from jms Queue')
            e (longOpt: 'execute', type: File, 'groovy script file to be called as a closure, and passed JMS Session ')
            //D (args:2, valueSeperator: '=', argName: 'property=value', 'Use value for givenm property')
        }

        def options = cli.parse(args)*/

        CliOptions options = new CliOptions()
        def cli = new CliBuilder(usage: 'java -jar file [<Options>]')
        cli.width = 80 //default is 74
        cli.parseFromInstance(options, args)

        //now get message platform from Factory
        mclient = MessagePlatformFactoryProducer.getFactory().getMessagePlatformInstance("WLS")


        def message

        if (options.help) {
            cli.usage()
            return
        }

        if (options.credentials){
            //todo validate something
        } else {
            //exit the cmdline process with error message if
            //credentials are required to run cmdline process
        }

        //check for queue or topic name default ovveride.  If present update environment before first create
        if (options.queueName) {
            //override the default queue from ApplicationConfig
            def lookupQname = options.queueName
            mclient.getPlatformEnvironmentProperty().put ("orderQueue", lookupQname)

        }
        if (options.topicName) {
            //override the default Topic from ApplicationConfig
            def lookupTname = options.receiveQ[0]
            mclient.getPlatformEnvironmentProperty().put ("orderTopic", lookupTname)

        }

        if (options.browseQueueName != null) {
            Queue q = null
            String lookupQname
            if (options.browseQueueName.size() > 0)
                lookupQname = options.browseQueueName[0]
            else {
                Map envMap = mclient.getPlatformEnvironment()
                lookupQname = envMap.get("DEFAULT_QUEUE")
            }
            q = mclient.getQueue(lookupQname)
            int size  = browseQueueSize(q)

            println "read message queue size  : $size, passsed Q: ${lookupQname ?: mclient.getPlatformEnvironmentProperty('orderQueue') }"
        }

        if (options.browseQueue != null) {
            Queue q = null
            String lookupQname
            lookupQname = options.browseQueue[0]
            q = mclient.getQueue(lookupQname)
            message = browse(q)

            println "read message : $message, passsed Q: ${lookupQname ?: mclient.getPlatformEnvironmentProperty('orderQueue') }"
        }

        if ((message = options.sendText)) {
            println "send message : $message"
            send(message)
        }
        //because of groovy truth - need to be explicit between null or [] size 0
        if (options.receiveQ != null) {
            Queue q = null
            String lookupQname
            if (options.receiveQ.size() == 1) {
                lookupQname = options.receiveQ[0]
                q = mclient.getQueue(lookupQname)
                message = receive(q)
            } else
                message = receive()

            println "read message : $message, passsed Q: ${lookupQname ?: mclient.getPlatformEnvironmentProperty('orderQueue') }"
        }

        if (options.sscript) {

            //try and find script relative to fatjar directory.  In fatjar this  shows the directory the fatjar was called from - else try backup script source below
            String base = Paths.get("").toAbsolutePath().toString()

            String userdir = System.getProperty("user.dir")

            String script = options.sscript
            String sourceName = "$base${File.separatorChar}$script"
            String backupScriptDir = mclient.getPlatformEnvironmentProperty('defaultScriptDirectory') ?: "~"
            // substitute the ~ for the users actual home path
            backupScriptDir = backupScriptDir.replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home")))
            String defaultSourceName = "$backupScriptDir${File.separatorChar}$script"
            File source = new File (sourceName.toString())
            File backupSource = new File (defaultSourceName.toString())
            def text = ""
            if (source.exists()) {
                text = "{it-> ${source.text}}"
            } else if (backupSource.exists()) {
                println "using backup script source ${backupSource.canonicalPath}"
                text = "{it-> ${backupSource.text}}"
            } else {
                throw new FileNotFoundException("cant find script file ${source.canonicalPath} passed as argument to Execute action")
            }

            //create a closure from users script and pass to withQueue platform resource handler
            GroovyShell shell = new GroovyShell()
            def clos = shell.evaluate("$text")
            /*def result = clos("hi")
            println result*/

            def result = withSenderQueue (clos)
            println "withSenderQueue : user closure returned : $result"
        }

        if (options.rscript) {

            //try and find script relative to fatjar directory.  In fatjar this  shows the directory the fatjar was called from - else try backup script source below
            String base = Paths.get("").toAbsolutePath().toString();

            String userdir = System.getProperty("user.dir")

            String script = options.rscript
            String sourceName = "$base${File.separatorChar}$script"
            String backupScriptDir = mclient.getPlatformEnvironmentProperty('defaultScriptDirectory') ?: "~"
            // substitute the ~ for the users actual home path
            backupScriptDir = backupScriptDir.replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home")))
            String defaultSourceName = "$backupScriptDir${File.separatorChar}$script"
            File source = new File (sourceName.toString())
            File backupSource = new File (defaultSourceName.toString())
            def text = ""
            if (source.exists()) {
                text = "{it-> ${source.text}}"
            } else if (backupSource.exists()) {
                println "using backup script source ${backupSource.canonicalPath}"
                text = "{it-> ${backupSource.text}}"
            } else {
                throw new FileNotFoundException("cant find script file ${source.canonicalPath} passed as argument to Execute action")
            }

            //create a closure from users script and pass to withQueue platform resource handler
            GroovyShell shell = new GroovyShell()
            def clos = shell.evaluate("$text")

            def result = withReceiverQueue (clos)
            println "withReceiverQueue :user closure returned : $result"
        }

    }

    /**
     * support routines for above
     */
    static def send (String text) {
        mclient.sendText(text)
        mclient.tidyUpSender()
    }

    static def withSenderQueue (Closure work) {
        def q = mclient.getQueue (mclient.getPlatformEnvironmentProperty('orderQueue'))
        mclient.withQueue (JmsConnectionType.Sender, q, work)
    }

    static def withReceiverQueue (Closure work) {
        def q = mclient.getQueue (mclient.getPlatformEnvironmentProperty('orderQueue'))
        mclient.withQueue (JmsConnectionType.Receiver, q, work)
    }

    static def receive (queue=null) {
        mclient.receiverStart()
        def result = mclient.receiveText ()

        mclient.tidyUpReceiver()
        result
    }

    static Enumeration<Message> browse (queue=null) {
        mclient.browserStart()
        def result = mclient.browse ()

        mclient.tidyUpBrowser()
        result
    }

    static int browseQueueSize (queue=null) {
        mclient.browserStart()
        int size = mclient.browseQueueSize (queue)

        mclient.tidyUpBrowser()
        size
    }

    static def withPublisherTopic (Closure work) {
        def t = mclient.getTopic (mclient.getPlatformEnvironmentProperty('orderTopic'))
        mclient.withQueue (JmsConnectionType.Publisher, t, work)
    }

    static def withSubscriberTopic (Closure work) {
        def t = mclient.getTopic (mclient.getPlatformEnvironmentProperty('orderTopic'))
        mclient.withQueue (JmsConnectionType.Subscriber, t, work)
    }


}
