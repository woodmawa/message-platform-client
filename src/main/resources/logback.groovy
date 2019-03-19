
import ch.qos.logback.core.*
import ch.qos.logback.core.encoder.*
import ch.qos.logback.core.read.*
import ch.qos.logback.core.rolling.*
import ch.qos.logback.core.status.*
import ch.qos.logback.classic.net.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.DEBUG

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender


//improve the pattern !
appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%-4relative [%thread] - %msg%n"
    }
}
root(DEBUG, ["CONSOLE"])

def setupLoggers() {
    logger 'com.softwood.implementation.WlsJmsMessagePlatform', DEBUG
}

def getLogLevel() {
    (isDevelopmentEnv() ? DEBUG : INFO)
}

def isDevelopmentEnv() {
    def env =  System.properties['app.env'] ?: 'DEV'
    env == 'DEV'
}
