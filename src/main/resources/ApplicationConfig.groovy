
messagePlatform {

    //this is saved as platform environment in the platform instance
    weblogic {
        protocol = 't3'  //change to 't3s' for secure connection
        hostname = 'localhost'
        port = '7001'   //default is 7002 for secure
        defaultProviderUrl = "t3://localhost:7001"
        senderSecurityPrincipal = 'cramer'
        receiverSecurityPrincipal = 'cortex'
        browserSecurityPrincipal = 'queueBrowser'
        orderQueue = 'jms/workOrderQueue'
        orderTopic = 'jms/workOrderTopic'
        orderResultsQueue = 'jms/workOrderResultsQueue'
        errorQueue = 'jms/errorQueue'
        DEFAULT_QUEUE = 'jms/workOrderQueue'  //fallback default queue
        DEFAULT_TOPIC = 'jms/workOrderTopic'

        defaultSubscriptionReceiveTimeout = 1L
        defaultScriptDirectory = "~${File.separatorChar}.scripts"  //set to users home directory
        INITIAL_CONTEXT_FACTORY = "weblogic.jndi.WLInitialContextFactory"
    }

    activeMQ {
       protocol = ''
       hostname = 'localhost'
       port = '7001'
       defaultProviderUrl = "xx://localhost:yyyy"
    }

    rabbitMQ {
       protocol = ''
       hostname = 'localhost'
       port = '7001'
       defaultProviderUrl = "xx://localhost:yyyy"

    }

    apacheQpid {
       protocol = ''
       hostname = 'localhost'
       port = '7001'
       defaultProviderUrl = "xx://localhost:yyyy"
    }

}