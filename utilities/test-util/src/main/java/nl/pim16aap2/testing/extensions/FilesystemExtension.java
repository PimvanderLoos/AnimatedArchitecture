package nl.pim16aap2.testing.extensions;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import nl.pim16aap2.testing.annotations.FileSystemTest;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.nio.file.FileSystem;
import java.util.List;
import java.util.stream.Stream;

/**
 * A JUnit 5 extension to run filesystem tests across multiple platform configurations.
 * <p>
 * This extension automatically runs tests against Unix, Windows, and macOS filesystem configurations using Google's
 * Jimfs in-memory filesystem. Each test method annotated with {@link FileSystemTest} will be executed once for each
 * platform configuration.
 * <p>
 * To use this extension, annotated your test method with {@link FileSystemTest}. The extension will inject a
 * {@link FileSystem} parameter into your test methods, providing access to the configured filesystem for that test
 * invocation.
 * <p>
 * Example usage:
 * <pre> {@code
 * class MyFileSystemTest {
 *     @FileSystemTest
 *     void testCreateDirectory(FileSystem fs) {
 *         Path testDir = fs.getPath("test");
 *         Files.createDirectory(testDir);
 *         assertTrue(Files.exists(testDir));
 *     }
 * }
 * } </pre>
 * <p>
 * The filesystem is automatically created before each test invocation and closed after each test to ensure test
 * isolation. Each platform configuration provides different path separators, case sensitivity rules, and other
 * filesystem behaviors specific to that operating system.
 */
public class FilesystemExtension implements TestTemplateInvocationContextProvider, ParameterResolver
{
    private static final String FILE_SYSTEM_KEY = "filesystem";

    private static final ExtensionContext.Namespace EXTENSION_NAMESPACE =
        ExtensionContext.Namespace.create(FilesystemExtension.class);

    @Override
    public boolean supportsTestTemplate(ExtensionContext context)
    {
        return context
            .getTestMethod()
            .map(method -> method.isAnnotationPresent(FileSystemTest.class))
            .orElse(false);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
        ExtensionContext context)
    {
        return Stream
            .of(
                Configuration.unix(),
                Configuration.windows(),
                Configuration.osX())
            .map(JimfsInvocationContext::new);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
        return parameterContext.getParameter().getType().equals(FileSystem.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
        return extensionContext
            .getStore(EXTENSION_NAMESPACE)
            .get(FILE_SYSTEM_KEY, FileSystem.class);
    }

    private static class JimfsInvocationContext implements TestTemplateInvocationContext
    {
        private final Configuration config;

        JimfsInvocationContext(Configuration config)
        {
            this.config = config;
        }

        @Override
        public String getDisplayName(int invocationIndex)
        {
            return config.toString();
        }

        @Override
        public List<Extension> getAdditionalExtensions()
        {
            return List.of(new FileSystemLifecycleExtension(config));
        }
    }

    private static class FileSystemLifecycleExtension implements BeforeEachCallback, AfterEachCallback
    {
        private final Configuration config;

        FileSystemLifecycleExtension(Configuration config)
        {
            this.config = config;
        }

        @Override
        public void beforeEach(ExtensionContext context)
        {
            final FileSystem fs = Jimfs.newFileSystem(config);
            context
                .getStore(EXTENSION_NAMESPACE)
                .put(FILE_SYSTEM_KEY, fs);
        }

        @Override
        public void afterEach(ExtensionContext context)
            throws Exception
        {
            context
                .getStore(EXTENSION_NAMESPACE)
                .get(FILE_SYSTEM_KEY, FileSystem.class)
                .close();
        }
    }
}
