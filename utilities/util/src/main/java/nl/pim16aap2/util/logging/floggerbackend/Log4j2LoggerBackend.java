/*
 * Copyright (C) 2019 The Flogger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.pim16aap2.util.logging.floggerbackend;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LoggerBackend;
import org.apache.logging.log4j.core.Logger;

import static nl.pim16aap2.util.logging.floggerbackend.Log4j2LogEventUtil.toLog4jLevel;
import static nl.pim16aap2.util.logging.floggerbackend.Log4j2LogEventUtil.toLog4jLogEvent;

/**
 * A customized version of Flogger's Log4j2 backend.
 *
 * @see <a
 * href="https://github.com/google/flogger/blob/master/log4j2/src/main/java/com/google/common/flogger/backend/log4j2/Log4j2LoggerBackend.java">Flogger's
 * Log4j2 backend</a>
 */
@SuppressWarnings("PMD") // Ignore PMD warnings because this is a copy of the original code.
public class Log4j2LoggerBackend extends LoggerBackend
{
    private final Logger logger;

    // VisibleForTesting?
    Log4j2LoggerBackend(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public String getLoggerName()
    {
        // Logger#getName() returns exactly the name that we used to create the Logger in
        // Log4jBackendFactory.
        return logger.getName();
    }

    @Override
    public boolean isLoggable(java.util.logging.Level level)
    {
        return logger.isEnabled(toLog4jLevel(level));
    }

    @Override
    public void log(LogData logData)
    {
        // The caller is responsible to call isLoggable() before calling this method to ensure that only
        // messages above the given threshold are logged.
        logger.get().log(toLog4jLogEvent(logger.getName(), logData));
    }

    @Override
    public void handleError(RuntimeException error, LogData badData)
    {
        logger.get().log(toLog4jLogEvent(logger.getName(), error, badData));
    }
}
