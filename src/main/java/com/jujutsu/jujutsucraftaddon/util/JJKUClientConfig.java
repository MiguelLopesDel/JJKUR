package com.jujutsu.jujutsucraftaddon.util;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Sistema de Configuração Client-Side do JJKU:
 * O Forge atualiza esses valores automaticamente ao detectar mudanças no arquivo .toml
 */
public class JJKUClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SHOW_HUD;
    public static final ForgeConfigSpec.BooleanValue SHOW_SUKUNA_MARKS;
    public static final ForgeConfigSpec.BooleanValue SHOW_BACKSTEP_HUD;
    public static final ForgeConfigSpec.BooleanValue SHOW_ZENITH_ARMS;

    static {
        BUILDER.push("Visual Settings");

        SHOW_HUD = BUILDER
                .comment("Ativar ou desativar o HUD customizado principal do JJKU: Resonance")
                .define("show_custom_hud", true);

        SHOW_BACKSTEP_HUD = BUILDER
                .comment("Ativar ou desativar o HUD de movimentação e defesa (Backstep/Guard)")
                .define("show_backstep_hud", true);

        SHOW_SUKUNA_MARKS = BUILDER
                .comment("Ativar ou desativar a visualização das suas próprias marcas de Sukuna")
                .define("show_sukuna_marks", true);

        SHOW_ZENITH_ARMS = BUILDER
                .comment("Ativar ou desativar a visualização dos 4 braços na forma Zenith")
                .define("show_zenith_arms", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
