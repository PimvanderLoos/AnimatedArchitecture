package nl.pim16aap2.bigdoors.api;


import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Interface for messaging players and writing to console.
 *
 * @author Pim
 */
public interface IMessagingInterface
{
    /**
     * Write a message to the console.
     *
     * @param level   Level of importance of the message.
     * @param message The message.
     */
    void writeToConsole(@NotNull Level level, @NotNull String message);

    /**
     * Send a message to a player.
     *
     * @param player  The player.
     * @param message The message.
     */
    void messagePlayer(@NotNull IPPlayer player, @NotNull String message);

    /**
     * Send a message to whomever or whatever issued a command at a given level (if applicable).
     *
     * @param target  The recipient of this message of unspecified type (console, player, whatever).
     * @param level   The level of the message (info, warn, etc). Does not apply to players.
     * @param message The message.
     */
    void sendMessageToTarget(@NotNull Object target, @NotNull Level level, @NotNull String message);

    /**
     * Broadcasts a server-wide message.
     *
     * @param message The message to broadcast.
     */
    void broadcastMessage(@NotNull String message);
}
