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
     * registered animation is registered with read/write access, the stamp will be a unique, non-zero value.
     * <p>
     * Registrations with read-only access all share a single stamp for all animators for the same movable UID. The
     * stamp is still unique between each group of animators.
     * <p>
     * Once registered, the animator for the animation can be inserted using {@link #addAnimator(long, Animator)}.
     *
     * @param uid
     *     The UID of the movable being animated.
     * @param requiresWriteAccess
     *     True to register the animation with read/write access. Only one such animator can be active per movable.
     * @return The stamp of the animation entry if it was registered successfully. If the animation could not be
     * registered, {@link OptionalLong#empty()} is returned.
     */
    @CheckReturnValue
    public OptionalLong registerAnimation(long uid, boolean requiresWriteAccess)
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
                final RegisteredAnimatorEntry newEntry =
                    RegisteredAnimatorEntry.newAnimatorEntry(key, requiresWriteAccess);
                newEntryRef.set(newEntry);
                return newEntry;
            }

            // If the existing entry is requiresWriteAccess, we cannot register a new animator.
            if (entry.requiresWriteAccess())
            {
                log.atFine().withStackTrace(StackSize.FULL)
                   .log("Trying to register animator with active requiresWriteAccess entry: %s", entry);
                return entry;
            }

            // If the new animator is requiresWriteAccess, but the old one(s) is/are not, we have to abort the
            // old one(s) to make space for the new one.
            if (requiresWriteAccess)
            {
                final RegisteredAnimatorEntry newEntry = RegisteredAnimatorEntry.newAnimatorEntry(key, true);
                newEntryRef.set(newEntry);
                abortEntryRef.set(entry);
                return newEntry;
            }

            // If there is nothing related to write access, we can re-use the existing, read-only registry entry.
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
     * Stops an active animator with write access if one currently exists for the movable with the provided UID.
     * <p>
     * See {@link Animator#stopAnimation()}.
     *
     * @param uid
     *     The UID of the movable being animated.
     */
    public void stopAnimatorsWithWriteAccess(long uid)
    {
        animators.compute(uid, (key, entry) ->
        {
            if (entry == null)
                return null;
            if (!entry.requiresWriteAccess())
                return entry;
            entry.stop();
            return entry;
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
        return animators.values().stream().map(RegisteredAnimatorEntry::getAnimators)
                        .flatMap(Collection::stream);
    }

    /**
     * Aborts all block movers that are currently active.
     * <p>
     * See {@link Animator#abort()}.
     */
    public void abortAnimators()
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
        abortAnimators();
    }

    @Override
    public void initialize()
    {
        isActive = true;
    }

    /**
     * Represents a registered animator.
     * <p>
     * In case of read-only animators, more than one animator may be registered per movable.
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
         * If the entry has write access, the stamp will have a unique, non-zero value.
         * <p>
         * For read-only entries, the stamp will be shared by all animators for a given movable. Between movables, the
         * stamp will still be unique.
         */
        @Getter
        private final long stamp = STAMP_COUNTER.incrementAndGet();

        /**
         * Creates a new {@link RegisteredAnimatorEntry} instance.
         *
         * @param key
         *     The key of the instance.
         * @param isReadWrite
         *     True if the entry is for an animator that requires write access.
         *     <p>
         *     See {@link AnimationType#requiresWriteAccess()}.
         * @return The newly created entry.
         */
        static RegisteredAnimatorEntry newAnimatorEntry(Long key, boolean isReadWrite)
        {
            return isReadWrite ?
                   new ReadWriteAnimatorEntry(key) :
                   new ReadOnlyAnimatorEntry(key);
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
         * more than one is registered, all animators are aborted.
         * <p>
         * See {@link Animator#abort()}.
         */
        public abstract void abort();

        /**
         * Gracefully the registered animator and prevents new animators from being registered in this registry entry.
         * If more than one is registered, all animators are stopped.
         * <p>
         * See {@link Animator#stopAnimation()}.
         */
        public abstract void stop();

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
        public abstract Collection<Animator> getAnimators();

        /**
         * @return The key of the entry.
         */
        public abstract long getKey();

        /**
         * @return True if this entry describes an animation that requires write access.
         * <p>
         * See {@link AnimationType#requiresWriteAccess()}.
         */
        public abstract boolean requiresWriteAccess();

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

        static final class ReadOnlyAnimatorEntry extends RegisteredAnimatorEntry
        {
            @Getter
            private final long key;
            private final Set<Animator> animators = Collections.newSetFromMap(new IdentityHashMap<>());
            private boolean isAborted = false;

            ReadOnlyAnimatorEntry(long key)
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
            synchronized public void stop()
            {
                animators.forEach(Animator::stopAnimation);
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
            public synchronized Collection<Animator> getAnimators()
            {
                return Collections.unmodifiableSet(animators);
            }

            @Override
            public boolean requiresWriteAccess()
            {
                return false;
            }

            @Override
            public synchronized boolean equals(final Object o)
            {
                if (o == this)
                    return true;
                if (!(o instanceof ReadOnlyAnimatorEntry other))
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
                return "ReadOnlyAnimatorEntry(stamp=" + this.getStamp() + ", key=" + this.key + ", animators=" +
                    this.animators + ", isAborted=" + this.isAborted + ")";
            }
        }

        static final class ReadWriteAnimatorEntry extends RegisteredAnimatorEntry
        {
            @Getter
            private final long key;

            private boolean isAborted = false;
            private @Nullable Animator animator;

            ReadWriteAnimatorEntry(long key)
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
            synchronized public void stop()
            {
                isAborted = true;
                if (animator != null)
                    animator.stopAnimation();
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
                        "Trying to override existing ReadWrite animator " + this.animator + " with animator: " +
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
                    if (animator.getAnimationType().requiresWriteAccess())
                        throw new IllegalStateException(String.format(logStr, animator, this.animator));
                    log.atFiner().log(logStr, animator, this.animator);
                    return false;
                }
                isAborted = true;
                this.animator = null;
                return true;
            }

            @Override
            public synchronized Collection<Animator> getAnimators()
            {
                return animator == null ? Collections.emptyList() : List.of(animator);
            }

            @Override
            public boolean requiresWriteAccess()
            {
                return true;
            }

            @Override
            public synchronized boolean equals(final Object o)
            {
                if (o == this)
                    return true;
                if (!(o instanceof ReadWriteAnimatorEntry other))
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
                return "ReadWriteAnimatorEntry(stamp=" + this.getStamp() + ", key=" + this.key + ", isAborted=" +
                    this.isAborted + ", animator=" + this.animator + ")";
            }
        }
    }
}
