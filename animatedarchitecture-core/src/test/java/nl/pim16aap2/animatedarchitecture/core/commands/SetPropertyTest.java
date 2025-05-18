package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CannotAddPropertyException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidCommandInputException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.PropertyCannotBeEditedByUserException;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyAccessLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import nl.pim16aap2.testing.assertions.AssertionBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.assertThatMessageable;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class SetPropertyTest
{
    private StructureRetriever structureRetriever;

    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private Structure structure;

    @Mock
    private ICommandSender commandSender;

    private AssistedFactoryMocker<SetProperty, SetProperty.IFactory> assistedFactoryMocker;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        assistedFactoryMocker =
            new AssistedFactoryMocker<>(SetProperty.class, SetProperty.IFactory.class).injectParameter(databaseManager);

        structureRetriever = StructureRetrieverFactory.ofStructure(structure);
    }

    @AfterEach
    void afterEach()
    {
        verifyNoMoreInteractions(databaseManager, commandSender);
    }

    @Test
    void validateInput_shouldSucceedForValidInputForUserEditableProperty()
    {
        // Setup
        final Property<String> property = mock();
        when(property.getType()).thenReturn(String.class);
        when(property.getPropertyAccessLevel()).thenReturn(PropertyAccessLevel.USER_EDITABLE);

        final SetProperty setProperty = setPropertyWithDefaults(property, "newValue");

        // Execute & Verify
        assertDoesNotThrow(setProperty::validateInput);
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void validateInput_shouldAcceptNullValue()
    {
        // Setup
        final Property<String> property = mock();
        when(property.getPropertyAccessLevel()).thenReturn(PropertyAccessLevel.USER_EDITABLE);

        final SetProperty setProperty = setPropertyWithDefaults(property, null);

        // Execute & Verify
        assertDoesNotThrow(setProperty::validateInput);
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void validateInput_shouldThrowExceptionForInvalidValueType()
    {
        // Setup
        final Property<String> property = mock();
        when(property.getType()).thenReturn(String.class);

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, 12);

        // Execute & Verify
        assertThatExceptionOfType(InvalidCommandInputException.class)
            .isThrownBy(setProperty::validateInput)
            .withMessage("Value '%d' cannot be assigned to property 'null'.", 12)
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender).sentErrorMessage("commands.set_property.error.invalid_value_type");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @ParameterizedTest
    @EnumSource(value = PropertyAccessLevel.class, names = {"USER_EDITABLE"}, mode = EnumSource.Mode.EXCLUDE)
    void validateInput_shouldThrowExceptionForNonEditableProperty(PropertyAccessLevel propertyAccessLevel)
    {
        // Setup
        final Property<String> property = mockPropertyWithNamespacedKey();

        when(property.getType()).thenReturn(String.class);
        when(property.getPropertyAccessLevel()).thenReturn(propertyAccessLevel);

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, "newValue");

        // Execute & Verify
        assertThatExceptionOfType(PropertyCannotBeEditedByUserException.class)
            .isThrownBy(setProperty::validateInput)
            .withMessage(property.getNamespacedKey().toString())
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.set_property.error.property_not_editable")
            .withArgs(property.getNamespacedKey().getKey());
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void performAction0_shouldThrowExceptionWhenPropertyCannotBeAdded()
    {
        // Setup
        final Property<?> property = mockPropertyWithNamespacedKey();

        when(structure.hasProperty(property)).thenReturn(false);
        when(property.canBeAddedByUser()).thenReturn(false);
        UnitTestUtil.setStructureLocalization(structure);

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, "newValue");

        // Execute & Verify
        assertThatExceptionOfType(CannotAddPropertyException.class)
            .isThrownBy(() -> setProperty.performAction0(structure))
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.set_property.error.property_cannot_be_added")
            .withArgs(property.getNamespacedKey().getKey(), "StructureType");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void performAction0_shouldSucceedWhenPropertyCanBeAddedByUser()
    {
        // Setup
        final Property<String> property = mock();
        final SetProperty setProperty = setPropertyWithDefaults(property, "newValue");

        when(structure.hasProperty(property)).thenReturn(false);
        when(property.canBeAddedByUser()).thenReturn(true);
        when(property.cast("newValue")).thenAnswer(invocation -> invocation.getArguments()[0]);

        // Execute
        assertDoesNotThrow(() -> setProperty.performAction0(structure));

        // Verify
        verify(structure).setPropertyValue(property, "newValue");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void performAction0_shouldSucceedWhenPropertyExists()
    {
        // Setup
        final Property<String> property = mock();
        final SetProperty setProperty = setPropertyWithDefaults(property, "newValue");

        when(structure.hasProperty(property)).thenReturn(true);
        when(property.cast("newValue")).thenAnswer(invocation -> invocation.getArguments()[0]);

        // Execute
        assertDoesNotThrow(() -> setProperty.performAction0(structure));

        // Verify
        verify(structure).setPropertyValue(property, "newValue");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void performAction0_shouldThrowNewExceptionWhenExceptionOccurs()
    {
        // Setup
        final String newValue = "newValue";
        final Property<String> property = mockPropertyWithNamespacedKey();
        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, newValue);

        when(structure.hasProperty(property)).thenReturn(true);
        when(property.cast("newValue")).thenAnswer(invocation -> invocation.getArguments()[0]);
        doThrow(new RuntimeException("Test exception")).when(structure).setPropertyValue(property, newValue);

        // Execute & Verify
        assertThatExceptionOfType(CommandExecutionException.class)
            .isThrownBy(() -> setProperty.performAction0(structure))
            .withMessageStartingWith("Failed to set value 'newValue' for property 'property' for structure ")
            .withRootCauseExactlyInstanceOf(RuntimeException.class)
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender).sentErrorMessage("commands.base.error.generic");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void performAction_shouldSyncStructureDataWhenSuccessIsTrue()
    {
        // Setup
        final Property<?> property = mockPropertyWithNamespacedKey();
        final IExecutor executor = mock();
        final StructureSnapshot snapshot = mock(StructureSnapshot.class);

        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
        when(structure.getSnapshot()).thenReturn(snapshot);
        when(databaseManager.syncStructureData(snapshot)).thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final SetProperty setProperty =
            spy(setPropertyWithDefaults(assistedFactoryMocker.injectParameter(executor), property, "newValue"));

        doNothing().when(setProperty).performAction0(structure);

        // Execute
        setProperty.performAction(structure).join();

        // Verify
        verify(databaseManager).syncStructureData(snapshot);
        assertThatMessageable(commandSender)
            .sentSuccessMessage("commands.set_property.success")
            .withArgs(property.getNamespacedKey().getKey());
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void performAction_shouldAppendExceptionContext()
    {
        // Setup
        final Property<Integer> property = mockPropertyWithNamespacedKey();
        final IExecutor executor = mock();

        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final SetProperty setProperty = spy(setPropertyWithDefaults(
            assistedFactoryMocker.injectParameter(executor),
            property,
            12
        ));

        doThrow(new CommandExecutionException(true, "TEST EXCEPTION")).when(setProperty).performAction0(structure);

        // Execute & Verify
        final var thrown = AssertionBuilder
            .assertHasExceptionContext(setProperty.performAction(structure))
            .withMessage("Set value '12' for property 'property' for structure 'null'.")
            .thenAssert();

        assertThat(thrown)
            .rootCause()
            .isExactlyInstanceOf(CommandExecutionException.class)
            .hasMessage("TEST EXCEPTION")
            .extracting("userInformed", InstanceOfAssertFactories.BOOLEAN)
            .isTrue();
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void handleDatabaseActionSuccess_shouldSendSuccessMessage()
    {
        // Setup
        final Property<?> property = mockPropertyWithNamespacedKey();

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, "newValue");

        // Execute
        setProperty.handleDatabaseActionSuccess(structure);

        assertThatMessageable(commandSender)
            .sentSuccessMessage("commands.set_property.success")
            .withArgs(property.getNamespacedKey().getKey());
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void getCommand_shouldReturnCorrectCommandDefinition()
    {
        // Setup
        final Property<?> property = mock();
        final SetProperty setProperty = setPropertyWithDefaults(property, "newValue");

        // Execute & Verify
        assertEquals(CommandDefinition.SET_PROPERTY, setProperty.getCommand());
        verify(commandSender).getPersonalizedLocalizer();
    }

    private SetProperty setPropertyWithDefaults(Property<?> property, @Nullable Object newValue)
    {
        return setPropertyWithDefaults(assistedFactoryMocker, property, newValue);
    }

    /**
     * Creates a new SetProperty instance with the default mocks and the given new value.
     *
     * @param assistedFactoryMocker
     *     The mocker to get the factory from that will create the SetProperty instance.
     * @param newValue
     *     The new value to set for the property.
     * @return The new SetProperty instance.
     */
    private SetProperty setPropertyWithDefaults(
        AssistedFactoryMocker<SetProperty, SetProperty.IFactory> assistedFactoryMocker,
        Property<?> property,
        @Nullable Object newValue)
    {
        final var factory = assistedFactoryMocker.getFactory();
        return factory.newSetProperty(commandSender, structureRetriever, property, newValue);
    }

    /**
     * Creates a new mock of {@link Property} with a {@link NamespacedKey} of "test:property".
     *
     * @param reified
     *     The reified type of the property. Do not provide any arguments.
     * @param <T>
     *     The type of the property.
     * @return The new mock of {@link Property}.
     */
    @SafeVarargs
    private <T> Property<T> mockPropertyWithNamespacedKey(@SuppressWarnings("unused") T... reified)
    {
        assertEquals(0, reified.length, "Do not provide any arguments to this method.");
        final Property<T> property = mock();
        final NamespacedKey namespacedKey = NamespacedKey.of("test:property");
        when(property.getNamespacedKey()).thenReturn(namespacedKey);
        return property;
    }
}
