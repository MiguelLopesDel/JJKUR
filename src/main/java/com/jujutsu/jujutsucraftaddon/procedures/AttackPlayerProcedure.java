package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.RainbowTextUtil;
import com.jujutsu.jujutsucraftaddon.entity.*;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.entity.EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeySimpleDomainOnKeyPressedProcedure;
import net.mcreator.jujutsucraft.procedures.StartGuardProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber
public class AttackPlayerProcedure {

    @SubscribeEvent
    public static void onEntityAttacked(LivingHurtEvent event) {
        if (event != null && event.getEntity() != null) {
            execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getSource(), event.getEntity(), event.getSource().getEntity(), event.getAmount());
        }
    }

    public static void execute(LevelAccessor world, double x, double y, double z, DamageSource damagesource, Entity entity, Entity sourceentity, double amount) {
        execute(null, world, x, y, z, damagesource, entity, sourceentity, amount);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, DamageSource damagesource, Entity entity, Entity sourceentity, double amount) {
        if (damagesource == null || entity == null || sourceentity == null) return;

        ResourceLocation startAnimDamage = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:start_animation")).location();
        if (damagesource.is(ResourceKey.create(Registries.DAMAGE_TYPE, startAnimDamage))) return;

        ResourceLocation entityTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        CompoundTag entityNBT = entity.getPersistentData();
        CompoundTag sourceNBT = sourceentity.getPersistentData();

        if (entityTypeKey != null && entityTypeKey.getNamespace().equals("jujutsucraft")) {
            if (entity instanceof LivingEntity living && !living.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && !living.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
                if (Math.random() < 0.01) {
                    StartGuardProcedure.execute(world, entity);
                }
            }
        }

        ItemStack mainHand = (sourceentity instanceof LivingEntity livingSource) ? livingSource.getMainHandItem() : ItemStack.EMPTY;
        ResourceLocation mainHandKey = ForgeRegistries.ITEMS.getKey(mainHand.getItem());

        sourceentity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(sourceAddonVars -> {
            String sourceClan = sourceAddonVars.Clans;

            if (mainHandKey != null && mainHandKey.toString().equals("jujutsucraft:knife") && sourceClan.equals("Majima")) {
                if (sourceentity instanceof LivingEntity livingSource && !livingSource.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get())) {
                    if (Math.random() < 0.1) {
                        applyEffect(entity, JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 40, 1);
                        applyEffect(entity, JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 40, 1);
                        applyEffect(entity, JujutsucraftaddonModMobEffects.ANTI_HEAL.get(), 40, 1);
                        applyEffect(entity, JujutsucraftaddonModMobEffects.ATTACKED.get(), 60, 1);
                    }
                }
            }

            if (sourceClan.equals("Kiryu")) {
                if (Math.random() < (1.0 / 30.0)) {
                    entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:damage_curse")))), (float) amount * 2);
                } else if (Math.random() < 0.05) {
                    applyEffect(entity, JujutsucraftaddonModMobEffects.RCT_CUT.get(), 60, 1);
                } else {
                    applyEffect(entity, JujutsucraftaddonModMobEffects.FATIGUE_BLACK_FLASH.get(), 120, 1);
                }

                if (Math.random() < (1.0 / 30.0)) {
                    InfComboProcedure.execute(world, sourceentity);
                }

                if (sourceentity instanceof LivingEntity livingSource && !livingSource.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get())) {
                    if (Math.random() < 0.1) {
                        applyEffect(entity, JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 40, 1);
                        applyEffect(entity, JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 40, 1);
                        applyEffect(entity, JujutsucraftaddonModMobEffects.ANTI_HEAL.get(), 120, 1);
                    }
                }
            }
        });

        entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(entityAddonVars -> {
            if (entityAddonVars.Clans.equals("Kiryu") && entity instanceof LivingEntity living && living.hasEffect(JujutsucraftaddonModMobEffects.COUNTER.get())) {
                sourceentity.hurt(damagesource, (float) amount * 2);
                CounterProcedureProcedure.execute(world, damagesource, entity, sourceentity);
                cancelEvent(event);
            }
        });

        if (entity instanceof LivingEntity livingEntity) {
            CeLevelProcedure.execute(world, sourceentity);
            if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.DAMAGE.get()) && entity instanceof Player player && player.getInventory().contains(new ItemStack(JujutsucraftaddonModItems.ARTIFACT_5.get()))) {
                entityNBT.putDouble("DamageFinal", entityNBT.getDouble("DamageFinal") + amount);
            }

            JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
            JujutsucraftaddonModVariables.PlayerVariables addonVarsSnapshot = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);

            if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.COUNTER.get())) {
                CounterProcedureProcedure.execute(world, damagesource, entity, sourceentity);
                cancelEvent(event);
            }

            if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.TRAINING.get()) && amount > 150) {
                TrainingFailedProcedure.execute(world, x, y, z, entity);
            }

            if (sourceentity instanceof LivingEntity livingSource && livingSource.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get()) && !livingSource.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                LineSukunaProcedure.execute(world, x, y, z, sourceentity);
            }

            ResourceLocation sourceEntityType = ForgeRegistries.ENTITY_TYPES.getKey(sourceentity.getType());
            if (sourceEntityType != null && sourceEntityType.toString().equals("jujutsucraft:gojo_satoru") && sourceentity instanceof LivingEntity livingSource && !livingSource.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                LineGojoProcedure.execute(world, x, y, z, sourceentity);
            }

            boolean mahoragaByBaseVars = baseVars != null && (baseVars.PlayerCurseTechnique == TechniqueIDs.MAHORAGA || baseVars.PlayerCurseTechnique2 == TechniqueIDs.MAHORAGA);
            boolean mahoragaByAddonVars = addonVarsSnapshot != null && addonVarsSnapshot.Mahoraga == 1.0;
            boolean mahoragaByEntity = entity instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity || entity instanceof IgrisEntity || entity instanceof Shadow1Entity;
            if (mahoragaByBaseVars || mahoragaByAddonVars || mahoragaByEntity) {
                handleMahoragaLogic(event, world, entity, damagesource, amount);
            }

            if (baseVars != null && baseVars.PlayerCurseTechnique == TechniqueIDs.MAHORAGA) {
                MahoragaAdaptedProcedure.execute(world, damagesource, entity);
                if (livingEntity.getItemBySlot(EquipmentSlot.HEAD).getOrCreateTag().getDouble("" + damagesource) > 50) {
                    cancelEvent(event);
                }
            }

            entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
                entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                    if (vars.PlayerCurseTechnique2 == TechniqueIDs.KASHIMO && addonVars.InfusedDomain) {
                        KashimoDefenseProcedure.execute(world, x, y, z, entity);
                    }
                    if (vars.PlayerCurseTechnique2 == TechniqueIDs.MIGUEL && addonVars.InfusedDomain) {
                        MiguelAttackedProcedure.execute(world, entity, sourceentity);
                    }
                });

                if (!livingEntity.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                    entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                        if (addonVars.InfusedDomain && vars.PlayerCurseTechnique2 == TechniqueIDs.MEGUMI) {
                            MegumiHitProcedure.execute(world, entity, sourceentity);
                        }
                    });

                    if (getEffectAmplifier(livingEntity, JujutsucraftModMobEffects.SUKUNA_EFFECT.get()) >= 19) {
                        if (entity instanceof ServerPlayer serverPlayer && !hasAdvancement(serverPlayer, "jujutsucraftaddon:world_slash_advancement") && hasAdvancement(serverPlayer, "jujutsucraftaddon:cleave_web_advancement")) {
                            WorldSlashQuestProcedure.execute(world, entity);
                        }
                    }

                    entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                        if (addonVars.BrainDamage == 5) BrainEffectTwoProcedure.execute(world, entity);
                    });

                    if (world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_SUKUNA_POSSESSION_ENABLED)) {
                        if (vars.PlayerCurseTechnique2 == TechniqueIDs.MEGUMI || vars.PlayerCurseTechnique2 == TechniqueIDs.ITADORI) {
                            if (Math.random() <= 0.05 && vars.BodyItem.getCount() >= 1.0) {
                                if (livingEntity.getMaxHealth() <= world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_SUKUNA_HEALTH_POSSESSION)) {
                                    SukunaKeybindOnKeyPressedProcedure.execute(entity);
                                }
                            }
                        }
                    }

                    if (vars.PlayerCurseTechnique2 == TechniqueIDs.YUTA) {
                        ChangeMimicryProcedure.execute(entity, sourceentity);
                    }

                    if (!livingEntity.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
                        entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                            if (addonVars.Timer1 == 1 && vars.PlayerCurseTechnique2 != -1 && !livingEntity.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get()) && !addonVars.Subrace.equals("Death Painting")) {
                                if (amount > 5) LimbssProcedure.execute(world, entity, amount);
                            }
                        });
                    }

                    if (world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_LIMB_LOSS) && !isCreative(entity)) {
                        entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                            if (addonVars.Limb > 0 && !livingEntity.hasEffect(JujutsucraftaddonModMobEffects.LIMBS_EFFECT.get())) {
                                applyEffect(entity, JujutsucraftaddonModMobEffects.LIMBS_EFFECT.get(), -1, 1);
                            }
                        });
                    }

                    if (livingEntity.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get()) && !livingEntity.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                        entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                            if (addonVars.CEShield == 3) {
                                applyEffect(entity, JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get(), 20, getEffectAmplifier(livingEntity, JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get()) + 1);
                            } else if (addonVars.CEShield == 4) {
                                KeySimpleDomainOnKeyPressedProcedure.execute(world, x, y, z, entity);
                            }
                        });
                    }

                    if (entityNBT.getDouble("brokenBrain") == 2 && !livingEntity.hasEffect(MobEffects.CONFUSION)) {
                        applyEffect(entity, MobEffects.CONFUSION, 60, 245);
                    }
                }

                entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                    if (addonVars.Subrace.equals("Disaster Curses")) modifyAmount(event, amount / 1.2);
                    if (livingEntity.hasEffect(JujutsucraftaddonModMobEffects.BERSERK.get()) && vars.PlayerCurseTechnique2 == -1)
                        modifyAmount(event, amount / 1.1);
                    if (addonVars.Timer1 == 1 && world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_BLACK_FLASH_REWORKED)) {
                        CounterBFProcedure.execute(world, sourceentity);
                    }
                });

                entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                    if (addonVars.InfusedDomain && vars.PlayerCurseTechnique2 == TechniqueIDs.TODO) {
                        if (!livingEntity.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get()) && !livingEntity.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && !livingEntity.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                            MegumiProcedure.execute(world, x, y, z, entity, sourceentity, amount);
                        }
                    }
                });

                if (vars.PlayerCurseTechnique2 == TechniqueIDs.YUTA && entity instanceof ServerPlayer serverPlayer && hasAdvancement(serverPlayer, "jujutsucraftaddon:sorcerer_strongest_of_modern")) {
                    if (!livingEntity.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get()) && sourceentity instanceof LivingEntity livingSource && !livingSource.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                        YutaCondition2Procedure.execute(world, x, y, z, entity);
                    }
                }

                if (isCurseOrCombat(damagesource)) {
                    entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                        if (addonVars.CEShield == 2 && !world.isClientSide() && entity.getServer() != null) {
                            int amp = getEffectAmplifier(livingEntity, MobEffects.DAMAGE_BOOST);
                            entity.getServer().getCommands().performPrefixedCommand(entity.createCommandSourceStack().withPermission(4).withSuppressedOutput(), "effect give @s jujutsucraft:domain_amplification 1 " + amp + " false");
                        }
                        if (addonVars.CEShield == 1 && !livingEntity.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get()) && addonVars.OutputLevel > 0 && vars.PlayerCursePower > 100) {
                            vars.PlayerCursePower -= 150.0 / addonVars.OutputLevel;
                            vars.syncPlayerVariables(entity);
                            modifyAmount(event, amount / 2);
                            spawnCEParticles(world, entity);
                        }
                    });
                }

            });

            if (sourceentity instanceof CloneEntity || sourceentity instanceof FakeClonesEntity || sourceentity instanceof FakePurpleClonesEntity) {
                livingEntity.swing(InteractionHand.MAIN_HAND, true);
            }

            if (livingEntity.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
                Vec3 center = new Vec3(x, y, z);
                world.getEntitiesOfClass(UiUiEntity.class, new AABB(center, center).inflate(50.0)).forEach(uiui -> {
                    if (uiui.getPersistentData().getString("OWNER_UUID").equals(entity.getStringUUID())) {
                        uiui.teleportTo(x, y, z);
                        applyEffect(uiui, JujutsucraftModMobEffects.SIMPLE_DOMAIN.get(), 400, getEffectAmplifier(uiui, JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) + 3);
                    }
                });
            }

            if (sourceentity instanceof LivingEntity livingSource && livingSource.hasEffect(JujutsucraftaddonModMobEffects.BERSERK.get()) && livingSource.getMainHandItem().getItem() == JujutsucraftaddonModItems.YUUN.get()) {
                modifyAmount(event, amount * 2);
            }

            if (entityNBT.getDouble("IsMahoraga") == 1 && livingEntity.getItemBySlot(EquipmentSlot.HEAD).getItem().toString().contains("mahoraga")) {
                MahoragaLogicProcedure.execute(world, entity, sourceentity);
            }

            if (!livingEntity.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && mainHand.getItem() == JujutsucraftaddonModItems.WUKONG_STAFF.get()) {
                SummonCloneProcedure.execute(world, x, y, z, sourceentity);
            }

            if (world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_WUKONG_BGM)) {
                if (sourceentity instanceof LivingEntity livingSource && getEffectAmplifier(livingSource, MobEffects.DAMAGE_BOOST) > 19 && entityNBT.getDouble("Wukong") == 0) {
                    world.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraftaddon:wukongtheme")), SoundSource.MASTER, 1, 1);
                    entityNBT.putDouble("Wukong", 1);
                }
            }

            if (world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_NPC_FISTS) && ForgeRegistries.ENTITY_TYPES.getKey(sourceentity.getType()).getNamespace().equals("jujutsucraft")) {
                NpcFistsProcedure.execute(world, entity, sourceentity);
            }

            if (entityTypeKey != null && entityTypeKey.toString().equals("jujutsucraft:eight_handled_sword_divergent_sila_divine_general_mahoraga")) {
                double sourceSkill = sourceNBT.getDouble("skill");
                String skillKey = new java.text.DecimalFormat("##.##").format(sourceSkill);
                CompoundTag headTag = livingEntity.getItemBySlot(EquipmentSlot.HEAD).getOrCreateTag();
                if (headTag.getDouble("skill" + skillKey) >= 100 && entityNBT.getDouble("adapted" + skillKey) != 1) {
                    livingEntity.getAttribute(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("jujutsucraft:size"))).setBaseValue(livingEntity.getAttribute(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("jujutsucraft:size"))).getBaseValue() + 0.5);
                    entityNBT.putDouble("adapted" + skillKey, 1);
                }
            }
        }
    }

    private static void applyEffect(Entity entity, net.minecraft.world.effect.MobEffect effect, int duration, int amplifier) {
        if (entity instanceof LivingEntity living && !entity.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
        }
    }

    private static int getEffectAmplifier(LivingEntity entity, net.minecraft.world.effect.MobEffect effect) {
        return entity.hasEffect(effect) ? entity.getEffect(effect).getAmplifier() : 0;
    }

    private static boolean hasAdvancement(ServerPlayer player, String id) {
        Advancement adv = player.server.getAdvancements().getAdvancement(new ResourceLocation(id));
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private static boolean isCurseOrCombat(DamageSource source) {
        return source.is(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:damage_combat"))) || source.is(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:damage_curse")));
    }

    private static boolean isCreative(Entity entity) {
        if (entity instanceof ServerPlayer sp) return sp.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
        return false;
    }

    private static void cancelEvent(@Nullable Event event) {
        if (event != null && event.isCancelable()) event.setCanceled(true);
    }

    private static void modifyAmount(@Nullable Event event, double amount) {
        if (event instanceof LivingHurtEvent lhe) lhe.setAmount((float) amount);
    }

    private static void spawnCEParticles(LevelAccessor world, Entity entity) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        double x = entity.getX(), y = entity.getY(), z = entity.getZ(), d = 2.0;
        SimpleParticleType p = (SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_BLUE.get();
        if (entity instanceof LivingEntity living) {
            if (living.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get()))
                p = (SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_RED.get();
            else if (living.hasEffect(JujutsucraftModMobEffects.JACKPOT.get()))
                p = (SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_GREEN.get();
        }
        serverLevel.sendParticles(p, x, y, z, (int) Math.min(Math.pow(d, 2.5) + 20.0, 100.0), d, 0.0, d, Math.min(d * 0.1, 1.5));
    }

    private static void handleMahoragaLogic(@Nullable Event event, LevelAccessor world, Entity entity, DamageSource source, double amount) {
        ItemStack head = (entity instanceof LivingEntity living) ? living.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY;
        CompoundTag tag = head.getOrCreateTag();
        String sourceId = "" + source;
        double current = tag.getDouble(sourceId);

        boolean isCombat = source.is(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:damage_combat")));
        boolean isCurse = source.is(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:damage_curse")));
        double limit = (isCombat || isCurse) ? 24 : 49;

        if (current < limit) {
            if (Math.random() < 0.01) {
                tag.putDouble(sourceId, current + 1);
                if (entity instanceof Player p && !p.level().isClientSide())
                    p.displayClientMessage(Component.literal((isCombat || isCurse ? "Adaptation: " : "Adapting: ") + (current + 1) + "%"), false);
            }
        } else if (current == limit) {
            tag.putDouble(sourceId, current + 1);
            if (entity instanceof Player p) {
                String msg = isCombat ? "Jujutsu Damage" : (isCurse ? "True Damage" : "" + source);
                String red = isCombat || isCurse ? "25% Of " + msg + " Reduction" : "50% Of Damage Reduction";
                RainbowTextUtil.sendRainbowMessage(p, "Mahoraga Adapted To: " + msg);
                RainbowTextUtil.sendRainbowMessage(p, red);
            }
        }
        modifyAmount(event, amount * (1 - (tag.getDouble(sourceId) / 100.0)));
    }
}
