package com.blr19c.falowp.bot.plugins.desktop.log

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.slf4j.LoggerFactory

/**
 * 日志监听器
 */
object LogListener {


    fun registerListener() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(LogEventAppender(loggerContext))
    }

    class LogEventAppender(context: LoggerContext) : AppenderBase<ILoggingEvent>() {

        init {
            this.setContext(context)
            this.start()
        }

        override fun append(eventObject: ILoggingEvent) {
            println("123")
            eventObject.formattedMessage
        }

    }
}