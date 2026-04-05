package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class SpawnJogoatProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null || !(world instanceof ServerLevel serverLevel)) return;

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(baseVars -> {
            entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                
                if (baseVars.PlayerCurseTechnique == TechniqueIDs.GOJO) {
                    handleGojoQuests(serverLevel, x, y, z, entity, addonVars.GH);
                } else if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.SUKUNA) {
                    handleSukunaQuests(serverLevel, x, y, z, entity, addonVars.SH);
                }
            });
        });
    }

    private static void handleGojoQuests(ServerLevel world, double x, double y, double z, Entity player, double gh) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (gh == 0) {
            setupBoss(world, JujutsucraftModEntities.JOGO, pos, player, 3.0);
            sendQuestMessage(player, "Beat Jogoat!");
        } else if (gh == 1) {
            setupBoss(world, JujutsucraftModEntities.JOGO, pos, player, 3.0);
            setupBoss(world, JujutsucraftModEntities.HANAMI, pos, player, 3.0);
            setupBoss(world, JujutsucraftModEntities.MAHITO, pos, player, 3.0);
            setupBoss(world, JujutsucraftModEntities.CHOSO, pos, player, 3.0);
            sendQuestMessage(player, "Smash Hanami In The Wall");
        } else if (gh == 2) {
            setupBoss(world, JujutsucraftModEntities.KENJAKU, pos, player, 3.0);
            sendQuestMessage(player, "Beat That Jin-Itadori Wife");
        } else if (gh == 3) {
            setupBoss(world, JujutsucraftModEntities.ITADORI_YUJI_SHINJUKU, pos, player, 3.0);
            setupBoss(world, JujutsucraftModEntities.OKKOTSU_YUTA, pos, player, 3.0);
            sendQuestMessage(player, "Train Your Pupils");
        } else if (gh == 4) {
            setupBoss(world, JujutsucraftModEntities.SUKUNA_FUSHIGURO, pos, player, 3.0);
            sendQuestMessage(player, "12/24");
        }
    }

    private static void handleSukunaQuests(ServerLevel world, double x, double y, double z, Entity player, double sh) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (sh == 0) {
            Entity boss = setupBoss(world, JujutsucraftModEntities.CURSED_SPIRIT_GRADE_01, pos, player, 3.0);
            if (boss instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 3, false, false));
            }
            sendQuestMessage(player, "Give This Weak Curse a \"Showtime\"");
        } else if (sh == 1) {
            setupBoss(world, JujutsucraftModEntities.JOGO, pos, player, 3.0);
            sendQuestMessage(player, "Who Is That Curse?");
        } else if (sh == 2) {
            setupBoss(world, JujutsucraftModEntities.EIGHT_HANDLED_SWORD_DIVERGENT_SILA_DIVINE_GENERAL_MAHORAGA, pos, player, 2.0);
            sendQuestMessage(player, "Take Care Of Mahoraga And Give Him a Beat");
        } else if (sh == 3) {
            setupBoss(world, JujutsucraftModEntities.ITADORI_YUJI_SHINJUKU, pos, player, 3.0);
            setupBoss(world, JujutsucraftModEntities.ZENIN_MAKI_CULLING_GAME, pos, player, 3.0);
            sendQuestMessage(player, "Beat Those Two Brats");
        } else if (sh == 4) {
            setupBoss(world, JujutsucraftModEntities.YOROZU, pos, player, 3.0);
            sendQuestMessage(player, "Time For A Test-Drive");
        } else if (sh == 5) {
            setupBoss(world, JujutsucraftModEntities.GOJO_SATORU, pos, player, 3.0);
            player.getPersistentData().putDouble("IsMahoraga", 1.0);
            executeCommand(player, "mahoraga");
            equipMahoragaHelmet(player);
            sendQuestMessage(player, "Arrogant, Gojo Satoru");
        } else if (sh == 6) {
            setupBoss(world, JujutsucraftModEntities.KASHIMO_HAJIME, pos, player, 3.0);
            sendQuestMessage(player, "Don't Disappoint Me");
        }
    }

    private static Entity setupBoss(ServerLevel world, Supplier<? extends EntityType<?>> type, BlockPos pos, Entity player, double hpMult) {
        Entity spawned = type.get().spawn(world, pos, MobSpawnType.MOB_SUMMONED);
        if (spawned instanceof LivingEntity boss) {
            boss.setYRot(world.getRandom().nextFloat() * 360.0F);
            boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 28, false, false));
            
            var maxHealth = boss.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(maxHealth.getBaseValue() * hpMult);
            }
            boss.setHealth(boss.getMaxHealth());

            CompoundTag nbt = boss.getPersistentData();
            nbt.putDouble("GH", 1.0);

            if (boss instanceof Mob mob && player instanceof LivingEntity livingPlayer) {
                mob.setTarget(livingPlayer);
            }
            world.addFreshEntity(boss);
        }
        return spawned;
    }

    private static void sendQuestMessage(Entity entity, String text) {
        if (entity instanceof Player player && !player.level().isClientSide()) {
            player.displayClientMessage(Component.literal(text), false);
            player.closeContainer();
        }
    }

    private static void executeCommand(Entity entity, String command) {
        if (!entity.level().isClientSide() && entity.getServer() != null) {
            entity.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), 
                (ServerLevel) entity.level(), 4, entity.getName().getString(), entity.getDisplayName(), entity.getServer(), entity), command);
        }
    }

    private static void equipMahoragaHelmet(Entity entity) {
        ItemStack helmet = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("jujutsucraft:mahoraga_wheel_helmet")));
        if (entity instanceof Player player) {
            player.getInventory().armor.set(3, helmet);
            player.getInventory().setChanged();
        } else if (entity instanceof LivingEntity living) {
            living.setItemSlot(EquipmentSlot.HEAD, helmet);
        }
    }
}
