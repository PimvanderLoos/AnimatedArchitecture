/**
 * This package contains a set of classes and utilities to capture and test logs in JUnit tests.
 * <p>
 * To use this package, you should use the {@link nl.pim16aap2.testing.logging.WithLogCapture} annotation on your test
 * class. This will capture all logs during the test and make them available for assertions.
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
 */
@NonNullByDefault
package nl.pim16aap2.testing.logging;

import org.eclipse.jdt.annotation.NonNullByDefault;
