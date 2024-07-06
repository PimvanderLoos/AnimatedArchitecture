package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import lombok.AccessLevel;
import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * A simple class that contains all default {@link BlockStateHandler}s.
 * <p>
 * This class is used to inject all default {@link BlockStateHandler}s into the {@link BlockStateManipulator}.
 */
@Singleton
public final class DefaultBlockStateHandlers
{
    @Getter(AccessLevel.PACKAGE)
    private final Set<BlockStateHandler<?>> defaultBlockStateTypeHandlers;

    @Inject
    DefaultBlockStateHandlers(
        BlockStateHandlerBanner blockStateHandlerBanner,
        BlockStateHandlerColorable blockStateHandlerColorable,
        BlockStateHandlerContainer blockStateHandlerContainer,
        BlockStateHandlerLockable blockStateHandlerLockable,
        BlockStateHandlerNameable blockStateHandlerNameable
    )
    {
        defaultBlockStateTypeHandlers = Set.of(
            blockStateHandlerBanner,
            blockStateHandlerColorable,
            blockStateHandlerContainer,
            blockStateHandlerLockable,
            blockStateHandlerNameable
        );
    }
}
