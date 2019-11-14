/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2010 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.LoggerProvider;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.parosproxy.paros.CommandLine;
import org.parosproxy.paros.network.SSLConnector;
import org.zaproxy.zap.eventBus.EventBus;
import org.zaproxy.zap.eventBus.SimpleEventBus;

public class ZAP {

    /** Not part of the public API. */
    public static final LoggerProvider JERICHO_LOGGER_PROVIDER = new LoggerProviderLog4j();

    /**
     * ZAP can be run in 4 different ways: cmdline: an inline process that exits when it completes
     * the tasks specified by the parameters daemon: a single process with no Swing UI, typically
     * run as a background process desktop: a Swing based desktop tool (which is how is originated,
     * as a fork of Paros Proxy) zaas: a highly scalable distributed system with a web based UI, aka
     * 'ZAP as a Service' (this is 'work in progress')
     */
    public enum ProcessType {
        cmdline,
        daemon,
        desktop,
        zaas
    }

    private static ProcessType processType;

    private static final EventBus eventBus = new SimpleEventBus();
    private static final Logger logger = Logger.getLogger(ZAP.class);

    static {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionLogger());

        // set SSLConnector as socketfactory in HttpClient.
        ProtocolSocketFactory sslFactory = null;
        try {
            final Protocol protocol = Protocol.getProtocol("https");
            sslFactory = protocol.getSocketFactory();

        } catch (final IllegalStateException e) {
            // Print the exception - log not yet initialised
            e.printStackTrace();
        }

        if (sslFactory == null || !(sslFactory instanceof SSLConnector)) {
            Protocol.registerProtocol(
                    "https",
                    new Protocol("https", (ProtocolSocketFactory) new SSLConnector(), 443));
        }

        // Initialise this earlier as possible.
        Config.LoggerProvider = JERICHO_LOGGER_PROVIDER;
    }

    /**
     * Main method
     *
     * @param args the arguments passed to the command line version
     * @throws Exception if something wrong happens
     */
    public static void main(String[] args) throws Exception {
        setCustomErrStream();

        CommandLine cmdLine = null;
        try {
            cmdLine = new CommandLine(args != null ? Arrays.copyOf(args, args.length) : null);

        } catch (final Exception e) {
            // Cant use the CommandLine help here as the
            // i18n messages wont have been loaded
            System.out.println("Failed due to invalid parameters: " + Arrays.toString(args));
            System.out.println(e.getMessage());
            System.out.println("Use '-h' for more details.");
            System.exit(1);
        }

        ZapBootstrap bootstrap = createZapBootstrap(cmdLine);
        try {
            int rc = bootstrap.start();
            if (rc != 0) {
                System.exit(rc);
            }

        } catch (final Exception e) {
            logger.fatal(e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void setCustomErrStream() {
        System.setErr(
                new DelegatorPrintStream(System.err) {

                    @Override
                    public void println(String x) {
                        // Suppress Nashorn removal warnings, too verbose (a warn each time is
                        // used).
                        if ("Warning: Nashorn engine is planned to be removed from a future JDK release"
                                .equals(x)) {
                            return;
                        }
                        super.println(x);
                    }
                });
    }

    private static ZapBootstrap createZapBootstrap(CommandLine cmdLineArgs) {
        ZapBootstrap bootstrap;
        if (cmdLineArgs.isGUI()) {
            ZAP.processType = ProcessType.desktop;
            bootstrap = new GuiBootstrap(cmdLineArgs);
        } else if (cmdLineArgs.isDaemon()) {
            ZAP.processType = ProcessType.daemon;
            bootstrap = new DaemonBootstrap(cmdLineArgs);
        } else {
            ZAP.processType = ProcessType.cmdline;
            bootstrap = new CommandLineBootstrap(cmdLineArgs);
        }
        return bootstrap;
    }

    public static ProcessType getProcessType() {
        return processType;
    }

    public static EventBus getEventBus() {
        return eventBus;
    }

    private static final class UncaughtExceptionLogger implements Thread.UncaughtExceptionHandler {

        private static final Logger logger = Logger.getLogger(UncaughtExceptionLogger.class);

        private static boolean loggerConfigured = false;

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (!(e instanceof ThreadDeath)) {
                if (loggerConfigured || isLoggerConfigured()) {
                    logger.error("Exception in thread \"" + t.getName() + "\"", e);

                } else {
                    System.err.println("Exception in thread \"" + t.getName() + "\"");
                    e.printStackTrace();
                }
            }
        }

        private static boolean isLoggerConfigured() {
            if (loggerConfigured) {
                return true;
            }

            @SuppressWarnings("unchecked")
            Enumeration<Appender> appenders = LogManager.getRootLogger().getAllAppenders();
            if (appenders.hasMoreElements()) {
                loggerConfigured = true;
            } else {

                @SuppressWarnings("unchecked")
                Enumeration<Logger> loggers = LogManager.getCurrentLoggers();
                while (loggers.hasMoreElements()) {
                    Logger c = loggers.nextElement();
                    if (c.getAllAppenders().hasMoreElements()) {
                        loggerConfigured = true;
                        break;
                    }
                }
            }

            return loggerConfigured;
        }
    }

    private static class DelegatorPrintStream extends PrintStream {

        private final PrintStream delegatee;

        public DelegatorPrintStream(PrintStream delegatee) {
            super(NullOutputStream.NULL_OUTPUT_STREAM);
            this.delegatee = delegatee;
        }

        @Override
        public void flush() {
            delegatee.flush();
        }

        @Override
        public void close() {
            delegatee.close();
        }

        @Override
        public boolean checkError() {
            return delegatee.checkError();
        }

        @Override
        protected void setError() {
            // delegatee manages its error state.
        }

        @Override
        protected void clearError() {
            // delegatee manages its error state.
        }

        @Override
        public void write(int b) {
            delegatee.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            delegatee.write(b);
        }

        @Override
        public void write(byte buf[], int off, int len) {
            delegatee.write(buf, off, len);
        }

        @Override
        public void print(boolean b) {
            delegatee.print(b);
        }

        @Override
        public void print(char c) {
            delegatee.print(c);
        }

        @Override
        public void print(int i) {
            delegatee.print(i);
        }

        @Override
        public void print(long l) {
            delegatee.print(l);
        }

        @Override
        public void print(float f) {
            delegatee.print(f);
        }

        @Override
        public void print(double d) {
            delegatee.print(d);
        }

        @Override
        public void print(char s[]) {
            delegatee.print(s);
        }

        @Override
        public void print(String s) {
            delegatee.print(s);
        }

        @Override
        public void print(Object obj) {
            delegatee.print(obj);
        }

        @Override
        public void println() {
            delegatee.println();
        }

        @Override
        public void println(boolean x) {
            delegatee.println(x);
        }

        @Override
        public void println(char x) {
            delegatee.println(x);
        }

        @Override
        public void println(int x) {
            delegatee.println(x);
        }

        @Override
        public void println(long x) {
            delegatee.println(x);
        }

        @Override
        public void println(float x) {
            delegatee.println(x);
        }

        @Override
        public void println(double x) {
            delegatee.println(x);
        }

        @Override
        public void println(char x[]) {
            delegatee.println(x);
        }

        @Override
        public void println(String x) {
            delegatee.println(x);
        }

        @Override
        public void println(Object x) {
            delegatee.println(x);
        }

        @Override
        public PrintStream printf(String format, Object... args) {
            return delegatee.printf(format, args);
        }

        @Override
        public PrintStream printf(Locale l, String format, Object... args) {
            return delegatee.printf(l, format, args);
        }

        @Override
        public PrintStream format(String format, Object... args) {
            delegatee.format(format, args);
            return this;
        }

        @Override
        public PrintStream format(Locale l, String format, Object... args) {
            delegatee.format(l, format, args);
            return this;
        }

        @Override
        public PrintStream append(CharSequence csq) {
            delegatee.append(csq);
            return this;
        }

        @Override
        public PrintStream append(CharSequence csq, int start, int end) {
            delegatee.append(csq, start, end);
            return this;
        }

        @Override
        public PrintStream append(char c) {
            delegatee.append(c);
            return this;
        }
    }

    // This class is a copy of Jericho's Log4j 2.x implementation but changed for Log4j 1.2.
    private static class LoggerProviderLog4j implements LoggerProvider {

        private static volatile net.htmlparser.jericho.Logger sourceLogger = null;

        private LoggerProviderLog4j() {}

        @Override
        public net.htmlparser.jericho.Logger getLogger(final String name) {
            return new Log4JLogger(LogManager.getLogger(name));
        }

        @Override
        public net.htmlparser.jericho.Logger getSourceLogger() {
            if (sourceLogger == null) {
                sourceLogger = getLogger("net.htmlparser.jericho");
            }
            return sourceLogger;
        }

        private static class Log4JLogger implements net.htmlparser.jericho.Logger {
            private final Logger log4JLogger;

            public Log4JLogger(final Logger log4JLogger) {
                this.log4JLogger = log4JLogger;
            }

            @Override
            public void error(final String message) {
                log4JLogger.error(message);
            }

            @Override
            public void warn(final String message) {
                log4JLogger.warn(message);
            }

            @Override
            public void info(final String message) {
                log4JLogger.info(message);
            }

            @Override
            public void debug(final String message) {
                log4JLogger.debug(message);
            }

            @Override
            public boolean isErrorEnabled() {
                return log4JLogger.isEnabledFor(Level.ERROR);
            }

            @Override
            public boolean isWarnEnabled() {
                return log4JLogger.isEnabledFor(Level.WARN);
            }

            @Override
            public boolean isInfoEnabled() {
                return log4JLogger.isEnabledFor(Level.INFO);
            }

            @Override
            public boolean isDebugEnabled() {
                return log4JLogger.isEnabledFor(Level.DEBUG);
            }
        }
    }
}
