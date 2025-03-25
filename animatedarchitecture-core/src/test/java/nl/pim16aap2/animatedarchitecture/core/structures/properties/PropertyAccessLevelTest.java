package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyAccessLevel.*;
import static org.assertj.core.api.Assertions.*;

class PropertyAccessLevelTest
{
    @Test
    void values_shouldContainAllEnumValues()
    {
        assertThat(VALUES)
            .containsExactly(PropertyAccessLevel.values());
    }

    @Test
    void hasOneFlagOf_shouldReturnTrueWhenFlagHasOneFlag()
    {
        final int flag = PropertyAccessLevel.READ.getFlag();
        final List<PropertyAccessLevel> Flags = List.of(READ, EDIT, ADD);

        assertThat(PropertyAccessLevel.hasOneFlagOf(flag, Flags)).isTrue();
    }

    @Test
    void hasOneFlagOf_shouldReturnTrueWhenFlagHasMultipleFlags()
    {
        final int flag = READ.getFlag() | EDIT.getFlag();
        final List<PropertyAccessLevel> Flags = List.of(READ, ADD, REMOVE);

        assertThat(PropertyAccessLevel.hasOneFlagOf(flag, Flags)).isTrue();
    }

    @Test
    void hasOneFlagOf_shouldReturnFalseWhenFlagHasNoFlags()
    {
        final int flag = READ.getFlag() | EDIT.getFlag();
        final List<PropertyAccessLevel> Flags = List.of(ADD, REMOVE);

        assertThat(PropertyAccessLevel.hasOneFlagOf(flag, Flags)).isFalse();
    }

    @Test
    void hasOneFlagOf_shouldReturnFalseForEmptyCollection()
    {
        final int flag = READ.getFlag() | EDIT.getFlag();

        assertThat(PropertyAccessLevel.hasOneFlagOf(flag, List.of())).isFalse();
    }

    @Test
    void hasOneFlagOf_shouldReturnFalseForZeroFlag()
    {
        final int flag = PropertyAccessLevel.READ.getFlag();

        assertThat(PropertyAccessLevel.hasOneFlagOf(flag, READ, EDIT, ADD)).isTrue();
    }

    @Test
    void hasOneFlagOf_arrayShouldDelegateToCollection()
    {
        final int flag = PropertyAccessLevel.READ.getFlag();
        final List<PropertyAccessLevel> Flags = List.of(READ, EDIT, ADD);

        assertThat(PropertyAccessLevel.hasOneFlagOf(flag, Flags)).isTrue();
    }

    @Test
    void getFlag_shouldBeUniqueAndMatchBitPositions()
    {
        assertThat(NONE.getFlag()).isEqualTo(0);
        assertThat(READ.getFlag()).isEqualTo(1);
        assertThat(EDIT.getFlag()).isEqualTo(2);
        assertThat(ADD.getFlag()).isEqualTo(4);
        assertThat(REMOVE.getFlag()).isEqualTo(8);
    }

    @Test
    void hasFlag_shouldReturnTrueWhenFlagIsPresent()
    {
        int flag = READ.getFlag() | EDIT.getFlag();

        assertThat(PropertyAccessLevel.hasFlag(flag, READ)).isTrue();
        assertThat(PropertyAccessLevel.hasFlag(flag, EDIT)).isTrue();
    }

    @Test
    void hasFlag_shouldReturnFalseWhenFlagIsNotPresent()
    {
        final int flag = READ.getFlag() | EDIT.getFlag();

        assertThat(PropertyAccessLevel.hasFlag(flag, ADD)).isFalse();
        assertThat(PropertyAccessLevel.hasFlag(flag, PropertyAccessLevel.REMOVE)).isFalse();
    }

    @Test
    void hasFlags_arrayShouldDelegateToCollection()
    {
        final int flag = READ.getFlag() | EDIT.getFlag() | ADD.getFlag();

        assertThat(PropertyAccessLevel.hasFlags(flag, READ, EDIT, ADD)).isTrue();
    }

    @Test
    void hasFlags_shouldReturnTrueWhenAllFlagsArePresent()
    {
        final int flag = READ.getFlag() | EDIT.getFlag() | ADD.getFlag();
        final List<PropertyAccessLevel> Flags = List.of(READ, EDIT, ADD);

        assertThat(PropertyAccessLevel.hasFlags(flag, Flags)).isTrue();
    }

    @Test
    void hasFlags_shouldReturnFalseWhenAnyFlagIsMissing()
    {
        final int flag = READ.getFlag() | EDIT.getFlag();
        final List<PropertyAccessLevel> Flags = List.of(READ, EDIT, ADD);

        assertThat(PropertyAccessLevel.hasFlags(flag, Flags)).isFalse();
    }

    @Test
    void hasFlags_shouldReturnTrueForEmptyCollection()
    {
        final int flag = 0;

        assertThat(PropertyAccessLevel.hasFlags(flag, List.of())).isTrue();
    }

    @Test
    void getFlagOf_shouldReturnCorrectFlagForSingleFlag()
    {
        final int flag = PropertyAccessLevel.getFlagOf(List.of(READ));

        assertThat(flag).isEqualTo(READ.getFlag());
    }

    @Test
    void getFlagOf_shouldReturnCorrectFlagForMultipleFlags()
    {
        final int flag = PropertyAccessLevel.getFlagOf(List.of(READ, EDIT, ADD));
        final int expected = READ.getFlag() | EDIT.getFlag() | ADD.getFlag();

        assertThat(flag).isEqualTo(expected);
    }

    @Test
    void getFlagOf_shouldReturnZeroForEmptyCollection()
    {
        final int flag = PropertyAccessLevel.getFlagOf(List.of());

        assertThat(flag).isZero();
    }

    @Test
    void getFlagOf_shouldHandleDuplicateFlags()
    {
        final Set<PropertyAccessLevel> Flags = new HashSet<>(List.of(READ, READ, EDIT));
        final int flag = PropertyAccessLevel.getFlagOf(Flags);
        final int expected = READ.getFlag() | EDIT.getFlag();

        assertThat(flag).isEqualTo(expected);
    }

    @Test
    void getFlagOf_shouldReturnAllFlagsWhenAllLevelsProvided()
    {
        final int flag = PropertyAccessLevel.getFlagOf(VALUES);
        final int expected = READ.getFlag() | EDIT.getFlag() | ADD.getFlag() | REMOVE.getFlag();

        assertThat(flag).isEqualTo(expected);
    }

    @Test
    void getFlagOf_arrayShouldDelegateToCollection()
    {
        final int flag = PropertyAccessLevel.getFlagOf(READ, EDIT, ADD);
        final int expected = READ.getFlag() | EDIT.getFlag() | ADD.getFlag();

        assertThat(flag).isEqualTo(expected);
    }
}
