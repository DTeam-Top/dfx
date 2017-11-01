import ch.qos.logback.classic.Level
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy

appender("Console", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d [%thread] %-5level %logger{36} - %msg%n"
    }
}

appender("R", RollingFileAppender) {
    file = "dfx.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%d [%thread] %-5level %logger{36} - %msg%n"
    }
    rollingPolicy(FixedWindowRollingPolicy) {
        fileNamePattern = "dfx.log.%i"
        minIndex = 1
        maxIndex = 10
    }
    triggeringPolicy(SizeBasedTriggeringPolicy) {
        maxFileSize = "10MB"
    }
}

logger("io.vertx", Level.WARN)
logger("io.netty", Level.WARN)
logger("ch.qos.logback", Level.WARN)

final String DFX_LOG_LEVEL = System.getProperty("DFX_LOG_LEVEL") ?:
        System.getenv("DFX_LOG_LEVEL")

root(Level.valueOf(DFX_LOG_LEVEL), ["Console", "R"])
