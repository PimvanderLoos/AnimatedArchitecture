package nl.pim16aap2.animatedarchitecture.core.api.debugging;

import lombok.AccessLevel;
import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A registry for {@link IDebuggable}s.
 * <p>
 * All debuggables should be registered here so that they can be accessed by the debug command.
 */
@Singleton
public final class DebuggableRegistry
{
    @Getter(AccessLevel.PACKAGE)
    private final List<IDebuggable> debuggables = new ArrayList<>();

    @Inject
    public DebuggableRegistry()
    {
    }

    public void registerDebuggable(IDebuggable debuggable)
    {
        debuggables.add(Objects.requireNonNull(debuggable, "Cannot register null debuggable!"));
    }
}
