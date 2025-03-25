package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CannotAddPropertyException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CannotEditPropertyException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CannotRemovePropertyException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidCommandInputException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.PropertyIsHiddenException;
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
    void setProperty_shouldSucceedForValidInput()
    {
        // setup
        final Property<String> property = mock();
        final SetProperty setProperty = setPropertyWithDefaults(property, "newValue");

        // execute & verify
        assertDoesNotThrow(() -> setProperty.setProperty(structure, property, "newValue"));
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    @SuppressWarnings("DirectInvocationOnMock")
    void setProperty_shouldThrowExceptionWhenTryingToRemoveNonRemovableProperty()
    {
        // setup
        final Property<String> property = mock();
        final NamespacedKey namespacedKey = new NamespacedKey("test", "property");
        final SetProperty setProperty = setPropertyWithDefaults(property, "newValue");

        UnitTestUtil.initMessageable(commandSender);
        UnitTestUtil.setStructureLocalization(structure);

        when(property.getNamespacedKey()).thenReturn(namespacedKey);
        when(structure.canRemoveProperty(property)).thenReturn(false);

        // execute & verify
        verify(commandSender).getPersonalizedLocalizer();
        assertThatExceptionOfType(InvalidCommandInputException.class)
            .isThrownBy(() -> setProperty.setProperty(structure, property, null))
            .withMessage("Cannot remove property '%s' for structure type %s", namespacedKey, structure.getType())
            .extracting(InvalidCommandInputException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();
        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.set_property.error.property_is_unremovable")
            .withArgs(namespacedKey.getKey(), "StructureType");
    }

    @Test
    void validateInput_shouldSucceedForValidInputForUserEditableProperty()
    {
        // setup
        final Property<String> property = mock();
        when(property.getType()).thenReturn(String.class);
        when(property.getAdminAccessLevel()).thenReturn(PropertyAccessLevel.EDIT.getFlag());

        final SetProperty setProperty = setPropertyWithDefaults(property, "newValue");

        // execute & verify
        assertDoesNotThrow(setProperty::validateInput);
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void validateInput_shouldAcceptNullValue()
    {
        // setup
        final Property<String> property = mock();
        when(property.adminHasAccessLevel(PropertyAccessLevel.REMOVE))
            .thenReturn(true);
        when(property.getAdminAccessLevel()).thenReturn(PropertyAccessLevel.REMOVE.getFlag());

        final SetProperty setProperty = setPropertyWithDefaults(property, null);

        // execute & verify
        assertDoesNotThrow(setProperty::validateInput);
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void validateInput_shouldThrowExceptionForInvalidValueType()
    {
        // setup
        final Property<String> property = mock();
        when(property.getType()).thenReturn(String.class);
        when(property.getAdminAccessLevel()).thenReturn(PropertyAccessLevel.EDIT.getFlag());

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, 12);

        // execute & verify
        assertThatExceptionOfType(InvalidCommandInputException.class)
            .isThrownBy(setProperty::validateInput)
            .withMessage("Value '%d' cannot be assigned to property 'null'.", 12)
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender).sentErrorMessage("commands.set_property.error.invalid_value_type");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void validateInput_shouldThrowExceptionWhenEvenAdminsCannotEditProperty()
    {
        // setup
        final Property<String> property = mockPropertyWithNamespacedKey();

        when(property.getAdminAccessLevel()).thenReturn(PropertyAccessLevel.READ.getFlag());

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, "newValue");

        // execute & verify
        assertThatExceptionOfType(CannotEditPropertyException.class)
            .isThrownBy(setProperty::validateInput)
            .withMessage(property.getNamespacedKey().toString())
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.set_property.error.property_is_read_only")
            .withArgs(property.getNamespacedKey().getKey());
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void validateInput_shouldThrowExceptionWhenEvenAdminsCannotRemoveProperty()
    {
        // setup
        final Property<String> property = mockPropertyWithNamespacedKey();

        when(property.getAdminAccessLevel()).thenReturn(PropertyAccessLevel.EDIT.getFlag());

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, null);

        // execute & verify
        assertThatExceptionOfType(CannotRemovePropertyException.class)
            .isThrownBy(setProperty::validateInput)
            .withMessage(property.getNamespacedKey().toString())
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.set_property.error.property_is_unremovable")
            .withArgs(property.getNamespacedKey().getKey());
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void validateInput_shouldThrowExceptionWhenPropertyIsHidden()
    {
        // setup
        final Property<String> property = mockPropertyWithNamespacedKey();

        when(property.getAdminAccessLevel()).thenReturn(PropertyAccessLevel.NONE.getFlag());

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, null);

        // execute & verify
        assertThatExceptionOfType(PropertyIsHiddenException.class)
            .isThrownBy(setProperty::validateInput)
            .withMessage(property.getNamespacedKey().toString())
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.set_property.error.property_is_hidden")
            .withArgs(property.getNamespacedKey().getKey());
        verify(commandSender).getPersonalizedLocalizer();
    }


    @ParameterizedTest
    @EnumSource(value = PropertyAccessLevel.class, names = {"ADD"}, mode = EnumSource.Mode.EXCLUDE)
    void performAction0_shouldThrowExceptionWhenPropertyCannotBeAdded(PropertyAccessLevel propertyAccessLevel)
    {
        // setup
        final Property<?> property = mockPropertyWithNamespacedKey();

        when(structure.hasProperty(property)).thenReturn(false);
        when(property.getAccessLevel(any())).thenReturn(propertyAccessLevel.getFlag());
        UnitTestUtil.setStructureLocalization(structure);

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, "newValue");

        // execute & verify
        assertThatExceptionOfType(CannotAddPropertyException.class)
            .isThrownBy(() -> setProperty.performAction0(structure))
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.set_property.error.not_allowed_to_add_property")
            .withArgs(property.getNamespacedKey().getKey(), "StructureType");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @ParameterizedTest
    @EnumSource(value = PropertyAccessLevel.class, names = {"REMOVE"}, mode = EnumSource.Mode.EXCLUDE)
    void performAction0_shouldThrowExceptionWhenPropertyCannotBeRemoved(PropertyAccessLevel propertyAccessLevel)
    {
        // setup
        final Property<?> property = mockPropertyWithNamespacedKey();

        when(property.getAccessLevel(any())).thenReturn(propertyAccessLevel.getFlag());
        UnitTestUtil.setStructureLocalization(structure);

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, null);

        // execute & verify
        assertThatExceptionOfType(CannotRemovePropertyException.class)
            .isThrownBy(() -> setProperty.performAction0(structure))
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.set_property.error.not_allowed_to_remove_property")
            .withArgs(property.getNamespacedKey().getKey(), "StructureType");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @ParameterizedTest
    @EnumSource(value = PropertyAccessLevel.class, names = {"EDIT"}, mode = EnumSource.Mode.EXCLUDE)
    void performAction0_shouldThrowExceptionWhenPropertyCannotBeEdited(PropertyAccessLevel propertyAccessLevel)
    {
        // setup
        final Property<?> property = mockPropertyWithNamespacedKey();

        when(structure.hasProperty(property)).thenReturn(true);
        when(property.getAccessLevel(any())).thenReturn(propertyAccessLevel.getFlag());
        UnitTestUtil.setStructureLocalization(structure);

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, "newValue");

        // execute & verify
        assertThatExceptionOfType(CannotEditPropertyException.class)
            .isThrownBy(() -> setProperty.performAction0(structure))
            .extracting(CommandExecutionException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.set_property.error.not_allowed_to_edit_property")
            .withArgs(property.getNamespacedKey().getKey(), "StructureType");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void performAction0_shouldSucceedWhenPropertyCanBeAdded()
    {
        // setup
        final Property<String> property = mock();
        final SetProperty setProperty = setPropertyWithDefaults(property, "newValue");

        when(structure.hasProperty(property)).thenReturn(false);
        when(property.getAccessLevel(any())).thenReturn(PropertyAccessLevel.ADD.getFlag());
        when(property.cast("newValue")).thenAnswer(invocation -> invocation.getArguments()[0]);

        // execute
        assertDoesNotThrow(() -> setProperty.performAction0(structure));

        // verify
        verify(structure).setPropertyValue(property, "newValue");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void performAction0_shouldSucceedWhenPropertyExists()
    {
        // setup
        final Property<String> property = mock();
        final SetProperty setProperty = setPropertyWithDefaults(property, "newValue");

        when(structure.hasProperty(property)).thenReturn(true);
        when(property.getAccessLevel(any())).thenReturn(PropertyAccessLevel.EDIT.getFlag());
        when(property.cast("newValue")).thenAnswer(invocation -> invocation.getArguments()[0]);

        // execute
        assertDoesNotThrow(() -> setProperty.performAction0(structure));

        // verify
        verify(structure).setPropertyValue(property, "newValue");
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void performAction0_shouldThrowNewExceptionWhenExceptionOccurs()
    {
        // setup
        final String newValue = "newValue";
        final Property<String> property = mockPropertyWithNamespacedKey();
        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, newValue);

        when(structure.hasProperty(property)).thenReturn(true);
        when(property.getAccessLevel(any())).thenReturn(PropertyAccessLevel.EDIT.getFlag());
        when(property.cast("newValue")).thenAnswer(invocation -> invocation.getArguments()[0]);
        doThrow(new RuntimeException("Test exception")).when(structure).setPropertyValue(property, newValue);

        // execute & verify
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
        // setup
        final Property<?> property = mockPropertyWithNamespacedKey();
        final IExecutor executor = mock();
        final StructureSnapshot snapshot = mock(StructureSnapshot.class);

        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
        when(structure.getSnapshot()).thenReturn(snapshot);
        when(databaseManager.syncStructureData(snapshot)).thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final SetProperty setProperty =
            spy(setPropertyWithDefaults(assistedFactoryMocker.injectParameter(executor), property, "newValue"));

        doNothing().when(setProperty).performAction0(structure);

        // execute
        setProperty.performAction(structure).join();

        // verify
        verify(databaseManager).syncStructureData(snapshot);
        assertThatMessageable(commandSender)
            .sentSuccessMessage("commands.set_property.success")
            .withArgs(property.getNamespacedKey().getKey());
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void performAction_shouldAppendExceptionContext()
    {
        // setup
        final Property<Integer> property = mockPropertyWithNamespacedKey();
        final IExecutor executor = mock();

        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final SetProperty setProperty = spy(setPropertyWithDefaults(
            assistedFactoryMocker.injectParameter(executor),
            property,
            12
        ));

        doThrow(new CommandExecutionException(true, "TEST EXCEPTION")).when(setProperty).performAction0(structure);

        // execute & verify
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
        // setup
        final Property<?> property = mockPropertyWithNamespacedKey();

        final SetProperty setProperty = setPropertyWithDefaults(assistedFactoryMocker, property, "newValue");

        // execute
        setProperty.handleDatabaseActionSuccess(structure);

        assertThatMessageable(commandSender)
            .sentSuccessMessage("commands.set_property.success")
            .withArgs(property.getNamespacedKey().getKey());
        verify(commandSender).getPersonalizedLocalizer();
    }

    @Test
    void getCommand_shouldReturnCorrectCommandDefinition()
    {
        // setup
        final Property<?> property = mock();
        final SetProperty setProperty = setPropertyWithDefaults(property, "newValue");

        // execute & verify
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
