
messagePlatform {
    weblogic {
        protocol = 't3'
        hostname = 'localhost'
        port = '7001'
        defaultProviderUrl = "t3://localhost:7001"
        mvaSenderSecurityPrincipal = 'cramer'
        mvaReceiverSecurityPrincipal = 'cortex'
        orderQueue = 'jms/workOrderQueue'
        orderResultsQueue = 'jms/workOrderResultsQueue'
        errorQueue = 'jms/errorQueue'
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