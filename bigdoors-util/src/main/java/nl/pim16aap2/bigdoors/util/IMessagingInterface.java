package nl.pim16aap2.bigdoors.util;

import java.util.UUID;
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
    void writeToConsole(Level level, String message);

    /**
     * Send a message to a player.
     * 
     * @param playerUUID UUID of the player that will receive the message.
     * @param message    The message.
     */
    void messagePlayer(UUID playerUUID, String message);

    /**
     * Send a message to whomever or whatever issued a command at a given level (if
     * applicable).
     *
     * @param target The recipient of this message of unspecified type (console, player, whatever).
     * @param level  The level of the message (info, warn, etc). Does not apply to
     *               players.
     * @param str    The message.
     */
    void sendMessageToTarget(final Object target, final Level level, final String str);
}
