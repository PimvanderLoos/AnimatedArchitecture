package nl.pim16aap2.bigdoors.moveblocks;

import com.google.common.flogger.StackSize;
import com.google.errorprone.annotations.CheckReturnValue;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.events.IBigDoorsEventCaller;
import nl.pim16aap2.bigdoors.managers.MovableDeletionManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.IMovableConst;
import nl.pim16aap2.bigdoors.util.Mutable;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Keeps track of which movables are currently active.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class MovableActivityManager extends Restartable implements MovableDeletionManager.IDeletionListener
{
    private final Map<Long, RegisteredAnimatorEntry> animators = new ConcurrentHashMap<>();

    private final IBigDoorsEventFactory eventFactory;
    private final IBigDoorsEventCaller bigDoorsEventCaller;

    private volatile boolean isActive = false;

    /**
     * Constructs a new {@link MovableActivityManager}.
     *
     * @param holder
     *     The {@link RestartableHolder} that manages this object.
     */
    @Inject
    public MovableActivityManager(
        RestartableHolder holder, IBigDoorsEventFactory eventFactory, IBigDoorsEventCaller bigDoorsEventCaller,
        MovableDeletionManager movableDeletionManager)
    {
        super(holder);
        movableDeletionManager.registerDeletionListener(this);
        this.eventFactory = eventFactory;
        this.bigDoorsEventCaller = bigDoorsEventCaller;
    }

    /**
     * Aborts an animator entry if it exists.
     *
     * @param entryRef
     *     A reference to an animator entry. The references object may be null, in which case nothing happens.
     */
    private void abort(Mutable<@Nullable RegisteredAnimatorEntry> entryRef)
    {
        final @Nullable MovableActivityManager.RegisteredAnimatorEntry entry = entryRef.get();
        if (entry != null)
            entry.abort();
    }

    /**
     * Attempts to register a new animation.
     * <p>
     * The registration attempt returns an optional long. This value is the stamp of the registry. In case the
     * registered animation is registered with exclusive access, the stamp will be a unique, non-zero value.
     * <p>
     * Registrations with non-exclusive access all share a single stamp for all animators for the same movable UID. The
     * stamp is still unique between each group of animators.
     * <p>
     * Once registered, the animator for the animation can be inserted using {@link #addAnimator(long, Animator)}.
     *
     * @param uid
     *     The UID of the movable being animated.
     * @param exclusive
     *     True to give the animation exclusive access to the
     * @return The stamp of the animation entry if it was registered successfully. If the animation could not be
     * registered, {@link OptionalLong#empty()} is returned.
     */
    @CheckReturnValue
    public OptionalLong registerAnimation(long uid, boolean exclusive)
    {
        @SuppressWarnings("NullAway") // NullAway doesn't see the @Nullable here
        final Mutable<@Nullable RegisteredAnimatorEntry> abortEntryRef = new Mutable<>(null);
        @SuppressWarnings("NullAway") // Or here
        final Mutable<@Nullable RegisteredAnimatorEntry> newEntryRef = new Mutable<>(null);

        animators.compute(uid, (key, entry)
            ->
        {
            if (entry == null)
            {
                final RegisteredAnimatorEntry newEntry = RegisteredAnimatorEntry.newAnimatorEntry(key, exclusive);
                newEntryRef.set(newEntry);
                return newEntry;
            }

            // If the existing entry is exclusive, we cannot register a new animator.
            if (entry.isExclusive())
            {
                log.atFine().withStackTrace(StackSize.FULL)
                   .log("Trying to register animator with active exclusive entry: %s", entry);
                return entry;
            }

            // If the new animator is exclusive, but the old one(s) is/are not, we have to abort the
            // old one(s) to make space for the new one.
            if (exclusive)
            {
                final RegisteredAnimatorEntry newEntry = RegisteredAnimatorEntry.newAnimatorEntry(key, true);
                newEntryRef.set(newEntry);
                abortEntryRef.set(entry);
                return newEntry;
            }

            // If there's no exclusivity around, we can re-use the existing, non-exclusive registry entry.
            newEntryRef.set(entry);
            return entry;
        });

        abort(abortEntryRef);

        final @Nullable MovableActivityManager.RegisteredAnimatorEntry newEntry = newEntryRef.get();
        return newEntry == null ? OptionalLong.empty() : OptionalLong.of(newEntry.getStamp());
    }

    /**
     * Unregisters an animation.
     *
     * @param uid
     *     The UID of the movable for which the animation was registered.
     * @param stamp
     *     The stamp of the registry entry.
     * @throws IllegalArgumentException
     *     If an entry was found and its stamp does not match the provided stamp.
     */
    public void unregisterAnimation(long uid, long stamp)
    {
        animators.compute(uid, (key, entry) ->
        {
            if (entry == null)
                return null;
            if (stamp != entry.getStamp())
                throw new IllegalArgumentException(
                    "Stamp mismatch: Expected stamp " + entry.getStamp() + " but got stamp: " + stamp);
            return entry.size() > 0 ? entry : null;
        });
    }

    /**
     * Processed a finished {@link Animator}.
     * <p>
     * The {@link AbstractMovable} that was being used by the {@link Animator} will be registered as inactive and any
     * scheduling that is required will be performed.
     *
     * @param animator
     *     The {@link Animator} to post-process.
     * @throws IllegalStateException
     *     When this manager is active and no entry exists for the provided animator.
     */
    void processFinishedAnimation(Animator animator)
    {
        processFinishedAnimation0(animator);
    }

    private void processFinishedAnimation0(Animator animator)
    {
        animators.compute(animator.getMovableUID(), (key, entry) ->
        {
            if (entry != null)
                return entry.remove(animator) ? null : entry;
            // We don't care about attempts to remove non-existent entries while shut(ting) down.
            // On shutdown, the map is cleared, so it is likely to reach this point.
            if (isActive)
                throw new IllegalStateException("Trying to remove unregistered animator: " + animator);
            return null;
        });

        bigDoorsEventCaller.callBigDoorsEvent(
            eventFactory.createToggleEndEvent(
                animator.getMovable(), animator.getSnapshot(), animator.getCause(),
                animator.getActionType(), animator.getPlayer(), animator.getTime(),
                animator.isSkipAnimation()));
    }

    /**
     * Stores a {@link Animator} in the appropriate slot in {@link #animators}
     *
     * @param stamp
     *     The stamp of the entry.
     *     <p>
     *     The stamp is used to validate the correct
     * @param animator
     *     The {@link Animator}.
     * @throws IllegalStateException
     *     When no existing entry exists for the provided animator.
     */
    public void addAnimator(long stamp, Animator animator)
    {
        animators.compute(animator.getMovableUID(), (uid, entry)
            ->
        {
            if (entry == null)
                throw new IllegalStateException("Trying to add animator to non-existent entry: " + animator);
            entry.addAnimator(stamp, animator);
            return entry;
        });
    }

    /**
     * Gets all the currently active {@link Animator}s.
     *
     * @return All the currently active {@link Animator}s.
     */
    public Stream<Animator> getBlockMovers()
    {
        return animators.values().stream().map(RegisteredAnimatorEntry::getMovables)
                        .flatMap(Collection::stream);
    }

    /**
     * Stops all block movers that are currently active.
     */
    public void stopAnimators()
    {
        animators.values().forEach(RegisteredAnimatorEntry::abort);
    }

    @Override
    public void onMovableDeletion(IMovableConst movable)
    {
        final @Nullable MovableActivityManager.RegisteredAnimatorEntry removed = animators.remove(
            movable.getUid());
        if (removed == null)
            return;

        log.atFinest().log("Aborted animation:%s\nFor deleted movable: %s", removed, movable);

    }

    @Override
    public void shutDown()
    {
        isActive = false;
        stopAnimators();
    }

    @Override
    public void initialize()
    {
        isActive = true;
    }

    /**
     * Represents a registered animator.
     * <p>
     * In case of non-exclusive animators, more than one animator may be registered per movable.
     */
    private sealed abstract static class RegisteredAnimatorEntry
    {
        /**
         * The stamp counter used to assign stamp values to new entries.
         */
        private static final AtomicLong STAMP_COUNTER = new AtomicLong(1);

        /**
         * The stamp of this entry.
         * <p>
         * If the entry is exclusive, the stamp will have a unique, non-zero value.
         * <p>
         * Non-exclusive animation types will always return 0.
         */
        @Getter
        private final long stamp = STAMP_COUNTER.incrementAndGet();

        /**
         * Creates a new {@link RegisteredAnimatorEntry} instance.
         *
         * @param key
         *     The key of the instance.
         * @param exclusive
         *     True if the entry is for an exclusive animator.
         *     <p>
         *     See {@link AnimationType#isExclusive()}.
         * @return The newly created entry.
         */
        static RegisteredAnimatorEntry newAnimatorEntry(Long key, boolean exclusive)
        {
            return exclusive ?
                   new ExclusiveAnimatorEntry(key) :
                   new NonExclusiveAnimatorEntry(key);
        }

        /**
         * @param stamp
         *     The stamp to verify against the stamp of this entry.
         * @throws IllegalArgumentException
         *     When the provided stamp does not match the stamp of this entry.
         */
        protected void verifyStamp(long stamp)
        {
            if (stamp != this.stamp)
                throw new IllegalArgumentException("Expected stamp '" + this.stamp + "' but received '" + stamp + "'");
        }

        /**
         * Aborts the registered animator and prevents new animators from being registered in this registry entry. If
         * more than one is registered, all animators are aborts.
         */
        public abstract void abort();

        /**
         * Adds a new animator to the current entry.
         *
         * @param stamp
         *     The stamp to use to verify correctness of the addition.
         * @param animator
         *     The animator to add.
         * @throws IllegalArgumentException
         *     When the provided stamp does not match the expected one.
         * @throws IllegalStateException
         *     When the current entry has been aborted previously.
         */
        public abstract void addAnimator(long stamp, Animator animator)
            throws IllegalArgumentException, IllegalStateException;

        /**
         * @return All animators in this entry.
         */
        public abstract Collection<Animator> getMovables();

        /**
         * @return The key of the entry.
         */
        public abstract long getKey();

        /**
         * @return True if this entry describes an exclusive type of animation.
         * <p>
         * See {@link AnimationType#isExclusive()}.
         */
        public abstract boolean isExclusive();

        /**
         * Removes the animator from this entry.
         *
         * @return True if, after removal, the entry does <b>not</b> contain any other animators.
         */
        public abstract boolean remove(Animator animator);

        /**
         * @return The number of animators in this entry.
         */
        public abstract int size();

        static final class NonExclusiveAnimatorEntry extends RegisteredAnimatorEntry
        {
            @Getter
            private final long key;
            private final Set<Animator> animators = Collections.newSetFromMap(new IdentityHashMap<>());
            private boolean isAborted = false;

            NonExclusiveAnimatorEntry(long key)
            {
                this.key = key;
            }

            @Override
            public synchronized void abort()
            {
                isAborted = true;
                animators.forEach(Animator::abort);
                animators.clear();
            }

            @Override
            public synchronized int size()
            {
                return animators.size();
            }

            @Override
            public synchronized void addAnimator(long stamp, Animator animator)
            {
                verifyStamp(stamp);
                if (isAborted)
                    throw new IllegalStateException("Trying to register animator in aborted state: " + animator);
                animators.add(animator);
            }

            @Override
            public synchronized boolean remove(Animator animator)
            {
                animators.remove(animator);
                return animators.isEmpty();
            }

            @Override
            public synchronized Collection<Animator> getMovables()
            {
                return Collections.unmodifiableSet(animators);
            }

            @Override
            public boolean isExclusive()
            {
                return false;
            }

            @Override
            public synchronized boolean equals(final Object o)
            {
                if (o == this)
                    return true;
                if (!(o instanceof NonExclusiveAnimatorEntry other))
                    return false;
                return this.isAborted != other.isAborted ||
                    this.key != other.key ||
                    !this.animators.equals(other.animators);
            }

            @Override
            public synchronized int hashCode()
            {
                final int prime = 31;
                int hashCode = 1;
                hashCode = hashCode * prime + Boolean.hashCode(this.isAborted);
                hashCode = hashCode * prime + Long.hashCode(this.key);
                hashCode = hashCode * prime + this.animators.hashCode();

                return hashCode;
            }

            @Override
            public synchronized String toString()
            {
                return "NonExclusiveAnimatorEntry(stamp=" + this.getStamp() + ", key=" + this.key + ", animators=" +
                    this.animators + ", isAborted=" + this.isAborted + ")";
            }
        }

        static final class ExclusiveAnimatorEntry extends RegisteredAnimatorEntry
        {
            @Getter
            private final long key;

            private boolean isAborted = false;
            private @Nullable Animator animator;

            ExclusiveAnimatorEntry(long key)
            {
                this.key = key;
            }

            @Override
            public synchronized void abort()
            {
                isAborted = true;
                if (animator != null)
                    animator.abort();
            }

            @Override
            public synchronized int size()
            {
                return animator == null ? 0 : 1;
            }

            @Override
            public synchronized void addAnimator(long stamp, Animator animator)
            {
                verifyStamp(stamp);
                if (this.animator != null)
                    throw new IllegalStateException(
                        "Trying to override existing exclusive animator " + this.animator + " with animator: " +
                            animator);
                if (isAborted)
                    throw new IllegalStateException("Trying to register animator in aborted state: " + animator);
                this.animator = animator;
            }

            @Override
            public synchronized boolean remove(Animator animator)
            {
                if (animator != this.animator)
                {
                    final String logStr = "Trying to remove animator %s while the entry actually contains animator %s";
                    if (animator.getAnimationType().isExclusive())
                        throw new IllegalStateException(String.format(logStr, animator, this.animator));
                    log.atFiner().log(logStr, animator, this.animator);
                    return false;
                }
                isAborted = true;
                this.animator = null;
                return true;
            }

            @Override
            public synchronized Collection<Animator> getMovables()
            {
                return animator == null ? Collections.emptyList() : List.of(animator);
            }

            @Override
            public boolean isExclusive()
            {
                return true;
            }

            @Override
            public synchronized boolean equals(final Object o)
            {
                if (o == this)
                    return true;
                if (!(o instanceof ExclusiveAnimatorEntry other))
                    return false;
                return this.isAborted != other.isAborted ||
                    this.key != other.key ||
                    this.getStamp() != other.getStamp() ||
                    !Objects.equals(this.animator, other.animator);
            }

            @Override
            public synchronized int hashCode()
            {
                final int prime = 31;
                int hashCode = 1;
                hashCode = hashCode * prime + Boolean.hashCode(this.isAborted);
                hashCode = hashCode * prime + Long.hashCode(this.key);
                hashCode = hashCode * prime + Long.hashCode(this.getStamp());
                hashCode = hashCode * prime + Objects.hashCode(this.animator);

                return hashCode;
            }

            @Override
            public synchronized String toString()
            {
                return "ExclusiveAnimatorEntry(stamp=" + this.getStamp() + ", key=" + this.key + ", isAborted=" +
                    this.isAborted + ", animator=" + this.animator + ")";
            }
        }
    }
}
