package nl.pim16aap2.animatedarchitecture.lightkeeper.e2e;

import nl.pim16aap2.lightkeeper.framework.ILightkeeperFramework;
import nl.pim16aap2.lightkeeper.framework.LightkeeperExtension;
import nl.pim16aap2.lightkeeper.framework.MenuHandle;
import nl.pim16aap2.lightkeeper.framework.PlayerHandle;
import nl.pim16aap2.lightkeeper.framework.Vector3Di;
import nl.pim16aap2.lightkeeper.framework.WorldHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LightkeeperExtension.class)
class AnimatedArchitectureLightkeeperIT
{
    private static final int BLOCKS_TO_MOVE = 2;

    @AfterEach
    void assertServerOutput_shouldContainNoUnexpectedErrors(ILightkeeperFramework framework)
    {
        AnimatedArchitectureE2eSupport.assertServerHealthy(framework);
    }

    @Test
    void portcullisCommandFlow_shouldCreateToggleAndMoveBlocks(ILightkeeperFramework framework)
    {
        // setup
        final WorldHandle world = framework.mainWorld();
        final Vector3Di lowerBlock = new Vector3Di(10, 100, 10);
        final String structureName = AnimatedArchitectureE2eSupport.uniqueName("cmd_portcullis");
        final PlayerHandle player = AnimatedArchitectureE2eSupport.createAnimatedArchitecturePlayer(
            framework,
            world,
            AnimatedArchitectureE2eSupport.uniquePlayerName("cmd"),
            new Vector3Di(10, 100, 8)
        );
        AnimatedArchitectureE2eSupport.placePortcullisFixture(world, lowerBlock);

        // execute
        AnimatedArchitectureE2eSupport.createClosedUpwardPortcullis(
            player,
            structureName,
            lowerBlock,
            BLOCKS_TO_MOVE
        );
        AnimatedArchitectureE2eSupport.toggleAndWaitForOpen(
            framework,
            player,
            world,
            structureName,
            lowerBlock,
            BLOCKS_TO_MOVE
        );

        // verify
        nl.pim16aap2.lightkeeper.framework.assertions.LightkeeperAssertions.assertThat(world)
            .hasBlockAt(lowerBlock)
            .ofType("minecraft:air");
        nl.pim16aap2.lightkeeper.framework.assertions.LightkeeperAssertions.assertThat(world)
            .hasBlockAt(AnimatedArchitectureE2eSupport.offset(lowerBlock, 0, BLOCKS_TO_MOVE, 0))
            .ofType("minecraft:stone");
        assertThat(player.receivedMessagesText()).contains("Portcullis creation successful");
    }

    @Test
    void createStructureMenu_shouldStartCreatorAndCreatePortcullis(ILightkeeperFramework framework)
    {
        // setup
        final WorldHandle world = framework.mainWorld();
        final Vector3Di lowerBlock = new Vector3Di(20, 100, 10);
        final String structureName = AnimatedArchitectureE2eSupport.uniqueName("gui_portcullis");
        final PlayerHandle player = AnimatedArchitectureE2eSupport.createAnimatedArchitecturePlayer(
            framework,
            world,
            AnimatedArchitectureE2eSupport.uniquePlayerName("gui"),
            new Vector3Di(20, 100, 8)
        );
        AnimatedArchitectureE2eSupport.placePortcullisFixture(world, lowerBlock);

        // execute
        final MenuHandle mainMenu = player.executeCommand("aa menu")
            .andWaitForMenuOpen(10)
            .verifyMenuName("AnimatedArchitecture Menu");
        AnimatedArchitectureE2eSupport.clickMenuItemContaining(mainMenu, "Create a new structure")
            .andWaitTicks(5);
        final MenuHandle createMenu = player.andWaitForMenuOpen(10)
            .verifyMenuName("New Structure");
        AnimatedArchitectureE2eSupport.clickMenuItemContaining(createMenu, "Create a new Portcullis")
            .andWaitForMenuClose()
            .verifyMenuClosed();
        AnimatedArchitectureE2eSupport.setCreatorName(player, structureName);
        AnimatedArchitectureE2eSupport.completeActivePortcullisCreator(player, lowerBlock, BLOCKS_TO_MOVE);
        AnimatedArchitectureE2eSupport.toggleAndWaitForOpen(
            framework,
            player,
            world,
            structureName,
            lowerBlock,
            BLOCKS_TO_MOVE
        );

        // verify
        nl.pim16aap2.lightkeeper.framework.assertions.LightkeeperAssertions.assertThat(world)
            .hasBlockAt(AnimatedArchitectureE2eSupport.offset(lowerBlock, 0, BLOCKS_TO_MOVE + 1, 0))
            .ofType("minecraft:stone");
        assertThat(player.receivedMessagesText()).contains("Portcullis creation successful");
    }

    @Test
    void worldGuardProtection_shouldDenyCreationInsideProtectedRegionAndAllowCreationOutside(
        ILightkeeperFramework framework)
    {
        // setup
        final WorldHandle world = framework.mainWorld();
        final Vector3Di deniedLowerBlock = new Vector3Di(42, 100, 42);
        final Vector3Di allowedLowerBlock = new Vector3Di(60, 100, 42);
        final String deniedStructureName = AnimatedArchitectureE2eSupport.uniqueName("wg_denied");
        final String allowedStructureName = AnimatedArchitectureE2eSupport.uniqueName("wg_allowed");
        final PlayerHandle player = AnimatedArchitectureE2eSupport.createAnimatedArchitecturePlayer(
            framework,
            world,
            AnimatedArchitectureE2eSupport.uniquePlayerName("wg"),
            new Vector3Di(40, 100, 39)
        );
        AnimatedArchitectureE2eSupport.placePortcullisFixture(world, deniedLowerBlock);
        AnimatedArchitectureE2eSupport.placePortcullisFixture(world, allowedLowerBlock);

        // execute
        player.executeCommand("aa newstructure portcullis " + deniedStructureName)
            .andWaitTicks(5)
            .leftClickBlock(deniedLowerBlock)
            .andWaitTicks(10);
        player.executeCommand("aa cancel")
            .andWaitTicks(5);
        AnimatedArchitectureE2eSupport.createClosedUpwardPortcullis(
            player,
            allowedStructureName,
            allowedLowerBlock,
            BLOCKS_TO_MOVE
        );
        AnimatedArchitectureE2eSupport.toggleAndWaitForOpen(
            framework,
            player,
            world,
            allowedStructureName,
            allowedLowerBlock,
            BLOCKS_TO_MOVE
        );

        // verify
        assertThat(player.receivedMessagesText()).contains("not allowed to create structures in this region");
        nl.pim16aap2.lightkeeper.framework.assertions.LightkeeperAssertions.assertThat(world)
            .hasBlockAt(AnimatedArchitectureE2eSupport.offset(allowedLowerBlock, 0, BLOCKS_TO_MOVE, 0))
            .ofType("minecraft:stone");
    }
}
