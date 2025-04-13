package nl.pim16aap2.animatedarchitecture.core.structures;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import nl.pim16aap2.testing.logging.WithLogCapture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

@Timeout(1)
@WithLogCapture
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StructureAnimationRequestTest
{
    @Mock
    private IConfig config;

    private IPlayer player;

    private StructureAnimationRequestBuilder builder;

    @BeforeEach
    void beforeEach()
        throws NoSuchMethodException
    {
        final var localizer = UnitTestUtil.initLocalizer();
        final var playerFactory = UnitTestUtil.createPlayerFactory();

        player = UnitTestUtil.createPlayer(UUID.randomUUID());

        Mockito.when(config.skipAnimationsByDefault()).thenReturn(true);

        final var structureAnimationRequestFactory = new AssistedFactoryMocker<>(
            StructureAnimationRequest.class,
            StructureAnimationRequest.IFactory.class)
            .injectParameter(ILocalizer.class, localizer)
            .injectParameter(ITextFactory.class, ITextFactory.getSimpleTextFactory())
            .injectParameter(IPlayerFactory.class, playerFactory)
            .getFactory();

        builder = new StructureAnimationRequestBuilder(
            structureAnimationRequestFactory,
            player,
            playerFactory,
            config
        );
    }

    @Test
    void testIsValidActionTypeNotOpenableToggle()
    {
        final Structure structure = Mockito.mock(Mockito.RETURNS_MOCKS);
        final var request = newToggleRequest(structure, player, StructureActionType.TOGGLE);

        Assertions.assertTrue(request.isValidActionType(structure));
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any());
    }

    @Test
    void testIsValidActionTypeNotOpenableOpen()
    {
        final Structure structure = Mockito.mock(Mockito.RETURNS_MOCKS);
        final var request = newToggleRequest(structure, player, StructureActionType.OPEN);

        Assertions.assertFalse(request.isValidActionType(structure));
        Mockito
            .verify(player)
            .sendMessage(UnitTestUtil.textArgumentMatcher("structure_action.open.error.type_has_no_open_status"));
    }

    @Test
    void testIsValidActionTypeNotOpenableClose()
    {
        final Structure structure = Mockito.mock(Mockito.RETURNS_MOCKS);
        final var request = newToggleRequest(structure, player, StructureActionType.CLOSE);

        Assertions.assertFalse(request.isValidActionType(structure));

        Mockito
            .verify(player)
            .sendMessage(UnitTestUtil.textArgumentMatcher("structure_action.close.error.type_has_no_open_status"));
    }

    @Test
    void testIsValidActionTypeOpenable()
    {
        final Structure structure = Mockito.mock(Mockito.RETURNS_MOCKS);
        UnitTestUtil.setPropertyContainerInMockedStructure(structure, Property.OPEN_STATUS);

        final var requestToggle = newToggleRequest(structure, player, StructureActionType.TOGGLE);
        Assertions.assertTrue(requestToggle.isValidActionType(structure));
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any());

        final var requestOpen = newToggleRequest(structure, player, StructureActionType.OPEN);
        Assertions.assertTrue(requestOpen.isValidActionType(structure));
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any());

        final var requestClose = newToggleRequest(structure, player, StructureActionType.CLOSE);
        Assertions.assertTrue(requestClose.isValidActionType(structure));
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any());
    }

    private StructureAnimationRequest newToggleRequest(
        Structure structure,
        IPlayer player,
        StructureActionType actionType)
    {
        return builder.builder()
            .structure(structure)
            .structureActionCause(StructureActionCause.SERVER)
            .structureActionType(actionType)
            .messageReceiver(player)
            .build();
    }
}
