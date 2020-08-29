package nl.pim16aap2.bigdoors.exceptions;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CommandPlayerNotFoundException extends Exception
{
    private static final long serialVersionUID = 1L;
    
    @Getter
    @NotNull String playerArg;
}
