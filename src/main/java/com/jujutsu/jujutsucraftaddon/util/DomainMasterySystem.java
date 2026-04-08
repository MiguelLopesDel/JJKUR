package com.jujutsu.jujutsucraftaddon.util;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Sistema de Maestria de Domínio (JJKU: Zenith)
 * Controla os multiplicadores de embate baseados em conquistas.
 */
public class DomainMasterySystem {
    public static final ResourceLocation BEGINNER = ResourceLocation.fromNamespaceAndPath("jujutsucraftaddon", "domain_beginner");
    public static final ResourceLocation INTERMEDIATE = ResourceLocation.fromNamespaceAndPath("jujutsucraftaddon", "domain_intermediate");
    public static final ResourceLocation ADVANCED = ResourceLocation.fromNamespaceAndPath("jujutsucraftaddon", "domain_advanced");
    public static final ResourceLocation WORLD_BALANCE = ResourceLocation.fromNamespaceAndPath("jujutsucraftaddon", "domain_world_balance");
    public static final ResourceLocation THOUSAND_YEAR_KING = ResourceLocation.fromNamespaceAndPath("jujutsucraftaddon", "domain_thousand_year_king");
    public static final ResourceLocation ZENITH = ResourceLocation.fromNamespaceAndPath("jujutsucraftaddon", "domain_zenith");

    public static double getFinalMultiplier(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return 1.0;

        if (hasAdvancement(player, ZENITH)) return 3.0;
        if (hasAdvancement(player, THOUSAND_YEAR_KING)) return 2.0;
        if (hasAdvancement(player, WORLD_BALANCE)) return 1.5;
        if (hasAdvancement(player, ADVANCED)) return 1.25;
        if (hasAdvancement(player, INTERMEDIATE)) return 1.15;
        if (hasAdvancement(player, BEGINNER)) return 1.10;

        return 1.0;
    }

    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation id) {
        if (player.server == null) return false;
        Advancement adv = player.server.getAdvancements().getAdvancement(id);
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    public static double apply(double currentScore, Entity entity) {
        return currentScore * getFinalMultiplier(entity);
    }
}
