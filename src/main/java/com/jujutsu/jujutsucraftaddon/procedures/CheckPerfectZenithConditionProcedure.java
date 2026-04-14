package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.util.JJKUVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class CheckPerfectZenithConditionProcedure {
    public static void execute(Entity entity) {
        if (entity instanceof ServerPlayer _player) {
            _player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(baseVars -> JJKUVariables.get(_player).ifPresent(addonVars -> {
                Advancement zenithAdv = _player.server.getAdvancements().getAdvancement(ResourceLocation.parse("jujutsucraftaddon:domain_zenith"));
                Advancement finger20Adv = _player.server.getAdvancements().getAdvancement(ResourceLocation.parse("jujutsucraft:finger_20"));
                Advancement perfectBodyAdv = _player.server.getAdvancements().getAdvancement(ResourceLocation.parse("jujutsucraftaddon:perfect_zenith_body"));

                if (zenithAdv != null && finger20Adv != null && perfectBodyAdv != null) {
                    boolean hasZenith = _player.getAdvancements().getOrStartProgress(zenithAdv).isDone();
                    boolean hasFinger20 = _player.getAdvancements().getOrStartProgress(finger20Adv).isDone();
                    boolean hasPerfectBody = _player.getAdvancements().getOrStartProgress(perfectBodyAdv).isDone();

                    if (hasZenith && hasFinger20 && baseVars.PlayerFame >= 100000.0 && !hasPerfectBody) {
                        AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(perfectBodyAdv);
                        if (!_ap.isDone()) {
                            for (String criteria : _ap.getRemainingCriteria()) {
                                _player.getAdvancements().award(perfectBodyAdv, criteria);
                            }
                        }
                    }
                }

                if (perfectBodyAdv != null && _player.getAdvancements().getOrStartProgress(perfectBodyAdv).isDone()) {
                    if (!addonVars.zenith_perfect_body) {
                        addonVars.zenith_perfect_body = true;
                        addonVars.syncPlayerVariablesToAll(_player);
                    }
                }
            }));
        }
    }
}
