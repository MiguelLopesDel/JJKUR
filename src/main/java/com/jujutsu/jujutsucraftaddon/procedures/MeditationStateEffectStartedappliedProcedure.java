package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

import java.util.function.Supplier;

public class MeditationStateEffectStartedappliedProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        if (entity instanceof Player player && !world.isClientSide()) {
            player.displayClientMessage(Component.literal("Your Training With Your Inner-Spirit Starts Now... Good Luck!"), false);
        }

        if (!(world instanceof ServerLevel serverLevel)) return;

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            double tech2 = vars.PlayerCurseTechnique2;
            EntityType<?> entityType = getEntityTypeForTechnique(tech2);

            if (entityType != null) {
                spawnSpirit(serverLevel, x, y, z, entityType, tech2);
            }
        });
    }

    private static EntityType<?> getEntityTypeForTechnique(double tech) {
        if (tech == TechniqueIDs.MAKI) return JujutsucraftModEntities.FUSHIGURO_TOJI.get();
        if (tech == TechniqueIDs.SUKUNA) return JujutsucraftModEntities.SUKUNA.get();
        if (tech == TechniqueIDs.GOJO) return JujutsucraftModEntities.GOJO_SATORU.get();
        if (tech == TechniqueIDs.INUMAKI) return JujutsucraftModEntities.INUMAKI_TOGE.get();
        if (tech == TechniqueIDs.JOGO) return JujutsucraftModEntities.JOGO.get();
        if (tech == TechniqueIDs.YUTA) return JujutsucraftModEntities.OKKOTSU_YUTA_CULLING_GAME.get();
        if (tech == TechniqueIDs.MEGUMI) return JujutsucraftModEntities.FUSHIGURO_MEGUMI_SHIBUYA.get();
        if (tech == TechniqueIDs.KASHIMO) return JujutsucraftModEntities.KASHIMO_HAJIME.get();
        if (tech == TechniqueIDs.DAGON) return JujutsucraftModEntities.DAGON.get();
        if (tech == TechniqueIDs.YUKI) return JujutsucraftModEntities.TSUKUMO_YUKI.get();
        if (tech == TechniqueIDs.CHOSO) return JujutsucraftModEntities.CHOSO.get();
        if (tech == TechniqueIDs.MEI_MEI) return JujutsucraftModEntities.MEI_MEI.get();
        if (tech == TechniqueIDs.ISHIGORI) return JujutsucraftModEntities.ISHIGORI_RYU.get();
        if (tech == TechniqueIDs.NANAMI) return JujutsucraftModEntities.NANAMI_KENTO.get();
        if (tech == TechniqueIDs.HANAMI) return JujutsucraftModEntities.HANAMI.get();
        if (tech == TechniqueIDs.MAHITO) return JujutsucraftModEntities.MAHITO.get();
        if (tech == TechniqueIDs.MAHORAGA) return JujutsucraftModEntities.EIGHT_HANDLED_SWORD_DIVERGENT_SILA_DIVINE_GENERAL_MAHORAGA.get();
        if (tech == TechniqueIDs.TAKABA) return JujutsucraftModEntities.TAKABA_FUMIHIKO.get();
        if (tech == TechniqueIDs.GETO) return JujutsucraftModEntities.GETO_SUGURU_CURSE_USER.get();
        if (tech == TechniqueIDs.NAOYA) return JujutsucraftModEntities.ZENIN_NAOYA.get();
        if (tech == TechniqueIDs.TODO) return JujutsucraftModEntities.TODO_AOI.get();
        if (tech == TechniqueIDs.ITADORI) return JujutsucraftModEntities.ITADORI_YUJI_SHINJUKU.get();
        if (tech == TechniqueIDs.JINICHI) return JujutsucraftModEntities.ZENIN_JINICHI.get();
        if (tech == TechniqueIDs.URAUME) return JujutsucraftModEntities.URAUME.get();
        if (tech == TechniqueIDs.SMALLPOX_DEITY) return JujutsucraftModEntities.CURSED_SPIRIT_GRADE_01.get();
        if (tech == TechniqueIDs.OGI) return JujutsucraftModEntities.ZENIN_OGI.get();
        if (tech == TechniqueIDs.HIGURUMA) return JujutsucraftModEntities.HIGURUMA_HIROMI.get();
        if (tech == TechniqueIDs.HANA) return JujutsucraftModEntities.KURUSU_HANA.get();
        if (tech == TechniqueIDs.HAKARI) return JujutsucraftModEntities.HAKARI_KINJI.get();
        if (tech == TechniqueIDs.MIGUEL) return JujutsucraftModEntities.MIGUEL_DANCER.get();
        if (tech == TechniqueIDs.KUSAKABE) return JujutsucraftModEntities.KUSAKABE_ATSUYA.get();
        if (tech == TechniqueIDs.CHOJURO) return JujutsucraftModEntities.ZENIN_CHOJURO.get();
        if (tech == TechniqueIDs.YAGA) return JujutsucraftModEntities.YAGA_MASAMICHI.get();
        if (tech == TechniqueIDs.NOBARA) return JujutsucraftModEntities.NOBARA_KUGISAKI.get();
        if (tech == TechniqueIDs.JUNPEI) return JujutsucraftModEntities.YOSHINO_JUNPEI.get();
        if (tech == TechniqueIDs.NISHIMIYA) return JujutsucraftModEntities.NISHIMIYA_MOMO.get();
        if (tech == TechniqueIDs.DHRUV) return JujutsucraftModEntities.DHRUV_LAKDAWALLA.get();
        if (tech == TechniqueIDs.URO) return JujutsucraftModEntities.URO_TAKAKO.get();
        if (tech == TechniqueIDs.YOROZU) return JujutsucraftModEntities.YOROZU.get();
        return null;
    }

    private static void spawnSpirit(ServerLevel level, double x, double y, double z, EntityType<?> type, double tech) {
        Entity spawned = type.spawn(level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
        if (spawned instanceof LivingEntity spirit) {
            spawned.setYRot(level.getRandom().nextFloat() * 360.0F);
            
            CompoundTag nbt = spirit.getPersistentData();
            nbt.putDouble("Spirit", 1.0);
            
            if (tech == TechniqueIDs.MEGUMI) {
                nbt.putDouble("TenShadowsTechnique14", -2.0);
                nbt.putDouble("TenShadowsTechnique13", -2.0);
            }

            spirit.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 253, false, false));
            
            var maxHealth = spirit.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(900.0);
            }
            spirit.setHealth(900.0F);
        }
    }
}
