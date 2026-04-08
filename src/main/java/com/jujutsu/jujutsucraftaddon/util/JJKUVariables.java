package com.jujutsu.jujutsucraftaddon.util;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

public class JJKUVariables {

    public static Optional<JujutsucraftaddonModVariables.PlayerVariables> get(Entity entity) {
        if (entity == null) return Optional.empty();

        return entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY).resolve();
    }

    @Nullable
    public static JujutsucraftaddonModVariables.PlayerVariables getNullable(Entity entity) {
        if (entity == null) return null;
        return entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY).orElseGet(null);
    }

    public static boolean getBoolean(Entity entity, Function<JujutsucraftaddonModVariables.PlayerVariables, Boolean> variableReader) {
        return get(entity).map(variableReader).orElse(false);
    }
}