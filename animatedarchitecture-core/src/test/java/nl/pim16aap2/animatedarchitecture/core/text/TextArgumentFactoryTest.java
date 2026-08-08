package nl.pim16aap2.animatedarchitecture.core.text;

import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextArgumentFactoryTest
{
    private static final String LOCALIZATION_KEY = "structure.type.portcullis";
    private static final String LOCALIZED_VALUE = "Portcullis";

    @Mock
    private ILocalizer localizer;

    @Mock
    private ITextComponentFactory textComponentFactory;

    @Mock
    private StructureType structureType;

    @Test
    void localizedHighlight_shouldLocalizeStructureTypeKey()
    {
        // setup
        final TextComponent component = new TextComponent();
        when(structureType.getLocalizationKey()).thenReturn(LOCALIZATION_KEY);
        when(localizer.getMessage(LOCALIZATION_KEY, (Locale) null)).thenReturn(LOCALIZED_VALUE);
        when(textComponentFactory.newComponent(TextType.HIGHLIGHT)).thenReturn(component);
        final TextArgumentFactory factory =
            new TextArgumentFactory(textComponentFactory, new PersonalizedLocalizer(localizer, null));

        // execute
        final TextArgument result = factory.localizedHighlight(structureType);

        // verify
        assertThat(result.argument()).isEqualTo(LOCALIZED_VALUE);
        assertThat(result.component()).isSameAs(component);
    }

    @Test
    void localizedInfo_shouldLocalizeStructureTypeKey()
    {
        // setup
        final TextComponent component = new TextComponent();
        when(structureType.getLocalizationKey()).thenReturn(LOCALIZATION_KEY);
        when(localizer.getMessage(LOCALIZATION_KEY, (Locale) null)).thenReturn(LOCALIZED_VALUE);
        when(textComponentFactory.newComponent(TextType.INFO)).thenReturn(component);
        final TextArgumentFactory factory =
            new TextArgumentFactory(textComponentFactory, new PersonalizedLocalizer(localizer, null));

        // execute
        final TextArgument result = factory.localizedInfo(structureType);

        // verify
        assertThat(result.argument()).isEqualTo(LOCALIZED_VALUE);
        assertThat(result.component()).isSameAs(component);
    }

    @Test
    void localized_shouldReturnKeyWhenLocalizerIsNull()
    {
        // setup
        final TextArgumentFactory factory = new TextArgumentFactory(textComponentFactory, null);

        // execute
        final String result = factory.localized(LOCALIZATION_KEY);

        // verify
        assertThat(result).isEqualTo(LOCALIZATION_KEY);
    }
}
