package nl.pim16aap2.testing.logging;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * A JUnit 5 extension to capture logs during tests.
 * <p>
 * This extension will capture all logs during a test and make them available for assertions.
 * <p>
 * When using this extension, you should annotate your test class with either {@link WithLogCapture} or
 * <code>@ExtendWith(LogCaptorExtension.class)</code>.
 * <p>
 * When using this extension, you can inject a {@link LogCaptor} into your test methods. This will contain all logs
 * captured during the test. The log level and log events are reset after each test.
 * <p>
 * Example code:
 * <pre> {@code
 * @WithLogCapture
 * class MyTest {
 *     // LogCaptor is automatically injected by the extension
 *     @Test
 *     void test(LogCaptor logCaptor) {
 *         // Your test code here
 *     }
 * }
 * } </pre>
 * <p>
 * By default, captured logs are not written to the console. If you want to see the logs in the console, you can use
 * {@link LogCaptor#enableConsoleOutput()}. Note that this will need to be done in each test.
 */
public class LogCaptorExtension
    implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback, ParameterResolver
{
    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(LogCaptorExtension.class);

    private static final String LOG_CAPTOR = "log_captor";

    @Override
    public void beforeAll(ExtensionContext context)
    {
        final LogCaptor logCaptor = LogCaptor.forRoot();
        context.getStore(NAMESPACE).put(LOG_CAPTOR, logCaptor);
    }

    @Override
    public void beforeEach(ExtensionContext context)
    {
        final LogCaptor logCaptor = context.getStore(NAMESPACE).get(LOG_CAPTOR, LogCaptor.class);
        logCaptor.disableConsoleOutput();
        logCaptor.setLogLevelToTrace();
    }

    @Override
    public void afterEach(ExtensionContext context)
    {
        final LogCaptor logCaptor = context.getStore(NAMESPACE).get(LOG_CAPTOR, LogCaptor.class);
        logCaptor.clearLogs();
    }

    @Override
    public void afterAll(ExtensionContext context)
    {
        final LogCaptor logCaptor = context.getStore(NAMESPACE).get(LOG_CAPTOR, LogCaptor.class);
        logCaptor.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
        return parameterContext.getParameter().getType() == LogCaptor.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
        return extensionContext.getStore(NAMESPACE).get(LOG_CAPTOR, LogCaptor.class);
    }
}
