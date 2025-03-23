package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.updater.UpdateCheckResult;
import nl.pim16aap2.animatedarchitecture.core.util.updater.UpdateChecker;
import nl.pim16aap2.animatedarchitecture.core.util.updater.UpdateInformation;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.util.text.TextRendererSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * Represents a listener that keeps track of {@link Player}s logging in to send them any messages if needed.
 */
@Singleton
public final class LoginMessageListener extends AbstractListener
{
    private final AnimatedArchitecturePlugin spigotPlugin;
    private final ITextFactory textFactory;
    private final @Nullable UpdateChecker updateChecker;

    @Inject
    public LoginMessageListener(
        AnimatedArchitecturePlugin javaPlugin,
        ITextFactory textFactory,
        @Nullable UpdateChecker updateChecker,
        @Nullable RestartableHolder restartableHolder)
    {
        super(restartableHolder, javaPlugin);

        this.spigotPlugin = javaPlugin;
        this.textFactory = textFactory;
        this.updateChecker = updateChecker;

        if (restartableHolder == null)
            register();
    }

    /**
     * Listens to {@link Player}s logging in and sends them the login message.
     *
     * @param event
     *     The {@link PlayerJoinEvent}.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        if (player.hasPermission(Constants.PERMISSION_PREFIX_ADMIN + "restart"))
            // Slight delay so the player actually receives the message;
            Bukkit.getScheduler().runTaskLater(plugin, () -> sendLoginMessage(player), 60L);
    }

    private void sendLoginMessage(Player player)
    {
        final Text text = textFactory.newText();

        addErrorMessage(text);
        addUpdateMessage(text);

        if (text.isEmpty())
            return;

        final Text header = textFactory.newText().append("[AnimatedArchitecture]", TextType.SUCCESS);
        player.spigot().sendMessage(header.append(text).render(new TextRendererSpigot()));
    }

    private void addErrorMessage(Text text)
    {
        final @Nullable String msg = spigotPlugin.getInitErrorMessage();
        if (msg == null)
            return;
        text.append("\nERROR: ", TextType.ERROR)
            .append(msg, TextType.INFO);
    }

    private void addUpdateMessage(Text text)
    {
        if (updateChecker == null)
            return;

        final @Nullable UpdateInformation info = updateChecker.getUpdateInformation();
        if (info == null)
            return;

        if (info.updateCheckResult().isError())
            text.append("\nERROR: ", TextType.ERROR)
                .append("Failed to check for updates!", TextType.INFO);
        if (info.updateCheckResult() == UpdateCheckResult.UPDATE_AVAILABLE)
            text.append("\nUpdate available: '", TextType.SUCCESS)
                .append(Objects.toString(info.updateName()), TextType.HIGHLIGHT)
                .append("'!", TextType.SUCCESS);
    }
}
