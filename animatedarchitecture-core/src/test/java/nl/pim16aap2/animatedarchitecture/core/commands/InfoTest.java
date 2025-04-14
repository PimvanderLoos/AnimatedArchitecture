package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class InfoTest
{
    @Mock
    private HighlightedBlockSpawner highlightedBlockSpawner;

    @Mock
    private Structure structure;

    @Mock
    private StructureSnapshot snapshot;

    @Mock
    private ICommandSender commandSender;

    private StructureRetriever structureRetriever;

    private AssistedFactoryMocker<Info, Info.IFactory> assistedFactoryMocker;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        structureRetriever = StructureRetrieverFactory.ofStructure(structure);

        assistedFactoryMocker = AssistedFactoryMocker.injectMocksFromTestClass(Info.IFactory.class, this);
    }

    @AfterEach
    void afterEach()
    {
        verifyNoMoreInteractions(highlightedBlockSpawner);
    }

    @Test
    void getCommand_shouldReturnInfo()
    {
        assertEquals(
            CommandDefinition.INFO,
            assistedFactoryMocker.getFactory().newInfo(commandSender, structureRetriever).getCommand()
        );
    }

    @Test
    void availableForNonPlayers_shouldReturnTrue()
    {
        assertTrue(
            assistedFactoryMocker
                .getFactory()
                .newInfo(commandSender, structureRetriever)
                .availableForNonPlayers()
        );
    }

    @Test
    void hasCreatorAccess_shouldReturnTrueForNonPlayer()
    {
        when(commandSender.getPlayer()).thenReturn(Optional.empty());

        assertTrue(Info.hasCreatorAccess(snapshot, commandSender));
    }

    @Test
    void hasCreatorAccess_shouldReturnTrueForPrimeOwner()
    {
        final UUID uuid = UUID.randomUUID();
        final IPlayer playerCommandSender = mock();
        final PlayerData playerData = new PlayerData(uuid, "", mock(), false, false);
        final StructureOwner owner = new StructureOwner(1, PermissionLevel.CREATOR, playerData);

        when(playerCommandSender.getUUID()).thenReturn(uuid);
        when(commandSender.getPlayer()).thenReturn(Optional.of(playerCommandSender));
        when(snapshot.getPrimeOwner()).thenReturn(owner);

        assertTrue(Info.hasCreatorAccess(snapshot, commandSender));
    }

    @Test
    void hasCreatorAccess_shouldReturnFalseForNonPrimeOwner()
    {
        final UUID uuidPrimeOwner = UUID.randomUUID();
        final UUID uuidCommandSender = UUID.randomUUID();
        final IPlayer playerCommandSender = mock();
        final PlayerData playerData = new PlayerData(uuidPrimeOwner, "", mock(), false, false);
        final StructureOwner owner = new StructureOwner(1, PermissionLevel.CREATOR, playerData);

        when(playerCommandSender.getUUID()).thenReturn(uuidCommandSender);
        when(commandSender.getPlayer()).thenReturn(Optional.of(playerCommandSender));
        when(snapshot.getPrimeOwner()).thenReturn(owner);

        assertFalse(Info.hasCreatorAccess(snapshot, commandSender));
    }
}
