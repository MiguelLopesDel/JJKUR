package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModEnchantments;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.AnimationDodgeProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.SukunaAttackAnimationsProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.SwapTodoTarget;
import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.*;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.Objects;

@Mixin(value = WhenEntityTakesDamageProcedure.class, priority = -10000)
public abstract class WhenEntityAttackedProcedureMixin {

    /**
     * @author Satushi / RIGOROUS RESTORATION
     * @reason 100% Fusion of JJKUR Original Logic and Jujutsu Craft v43 improvements.
     * RESTORED: Soul Perception, Cursed Spirit Immunity, Trig Dodges, Mahoraga Adaptation, Falling Blossom Counter.
     */
    @Inject(at = @At("HEAD"), method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;D)V", remap = false, cancellable = true)
    private static void execute(Event event, LevelAccessor world, DamageSource damagesource, Entity entity, Entity immediatesourceentity, Entity sourceentity, double amount, CallbackInfo ci) {
        ci.cancel();
        if (damagesource == null || entity == null || sourceentity == null || immediatesourceentity == null) return;

        // Skip animation damage
        if (damagesource.is(TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("forge:animation")))) return;

        JujutsucraftModVariables.PlayerVariables pVars = entity instanceof Player ? entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null) : null;
        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());
        JujutsucraftaddonModVariables.PlayerVariables sourceAddonVars = sourceentity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());

        boolean cancel = false;
        double damageAmount = amount;
        double oldCooldown = entity.getPersistentData().getDouble("COOLDOWN_TICKS");

        // --- SECTION 1: ADDON ANIMATIONS & DODGES ---
        SukunaAttackAnimationsProcedure.execute(sourceentity, entity, world);

        if (sourceentity instanceof FushiguroTojiBugEntity _tojiBug && amount >= 100) {
            _tojiBug.setAnimation("playful" + Mth.nextInt(RandomSource.create(), 1, 4));
        } else if (sourceentity instanceof FushiguroTojiEntity _toji && amount >= 100) {
            ItemStack sourceHand = (sourceentity instanceof LivingEntity _liv) ? _liv.getMainHandItem() : ItemStack.EMPTY;
            if (sourceHand.getItem() == JujutsucraftModItems.PLAYFUL_CLOUD.get()) {
                _toji.setAnimation("playful" + Mth.nextInt(RandomSource.create(), 1, 4));
            }
        }

        // Run == 1 Animation effect
        if (sourceAddonVars.Run == 1 && sourceentity instanceof LivingEntity _livS) {
            if (!_livS.level().isClientSide() && !_livS.hasEffect(JujutsucraftaddonModMobEffects.ANIMATION.get())) {
                _livS.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.ANIMATION.get(), 10, 1, false, false));
            }
        }

        // Todo Aoi / Boogie Woogie
        if (pVars != null && pVars.PlayerCurseTechnique == 20.0 && pVars.PlayerCursePower >= 1000.0 && Math.random() < 1.0 / 60.0) {
            SwapTodoTarget.execute(entity, world, sourceentity);
            AnimationDodgeProcedure.execute(world, entity);
            cancelEvent(event); return;
        } else if (entity instanceof TodoAoiEntity && Math.random() < 1.0 / 60.0) {
            SwapTodoTarget.execute(entity, world, sourceentity);
            cancelEvent(event); return;
        }

        // Ultra Instinct
        ItemStack chestArmor = (entity instanceof LivingEntity _liv) ? _liv.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY;
        int uiLevel = EnchantmentHelper.getItemEnchantmentLevel(JujutsucraftaddonModEnchantments.ULTRA_INSTINCT.get(), chestArmor);
        if (uiLevel > 0 && Math.random() < uiLevel / 100.0) {
            AnimationDodgeProcedure.execute(world, entity);
            cancelEvent(event); return;
        }

        // Itadori Awakening (RESTORED TRIG POSITIONING)
        if (sourceentity instanceof LivingEntity _livSource && _livSource.hasEffect(JujutsucraftaddonModMobEffects.ITADORI_AWAKENING.get()) && Math.random() < 1.0 / 60.0) {
            sourceentity.getPersistentData().putDouble("skill", 3810.0);
            sourceentity.getPersistentData().putDouble("cnt6", 4.0);
            double yaw = Math.toRadians(sourceentity.getYRot() + 90.0F);
            double pitch = Math.toRadians(sourceentity.getXRot());
            sourceentity.getPersistentData().putDouble("x_pos", entity.getX() + Math.cos(yaw) * Math.cos(pitch) * (double) (2.0F + entity.getBbWidth()));
            sourceentity.getPersistentData().putDouble("y_pos", entity.getY() + (double) entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * (double) (2.0F + entity.getBbWidth()));
            sourceentity.getPersistentData().putDouble("z_pos", entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * (double) (2.0F + entity.getBbWidth()));
            if (!_livSource.level().isClientSide()) {
                _livSource.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), 60, 1, false, false));
                if (!_livSource.hasEffect(JujutsucraftaddonModMobEffects.KOKUSEN_N.get())) {
                    _livSource.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.KOKUSEN_N.get(), 20, 1, false, false));
                }
            }
        }

        // Uro Fujiwara / Zenin Restricted (RESTORED TRIG POSITIONING)
        if (pVars != null && pVars.PlayerCurseTechnique == 38.0) {
            double chance = addonVars.Clans.equals("Fujiwara") ? 1.0 / 50.0 : 1.0 / 120.0;
            if (Math.random() < chance) {
                applyDodgeRigor(world, entity, 3810.0);
                cancelEvent(event); return;
            }
        } else if (pVars != null && pVars.PlayerCurseTechnique == -1.0 && pVars.PlayerCursePower == 0.0) {
            double chance = addonVars.Clans.equals("Rejected Zenin") ? 1.0 / 40.0 : 1.0 / 80.0;
            if (Math.random() < chance) {
                AnimationDodgeProcedure.execute(world, entity);
                cancelEvent(event); return;
            }
        }

        // Wukong Set
        ItemStack feetArmor = (entity instanceof LivingEntity _liv) ? _liv.getItemBySlot(EquipmentSlot.FEET) : ItemStack.EMPTY;
        if (feetArmor.getItem() == JujutsucraftaddonModItems.WUKONG_SET_BOOTS.get().asItem() && Math.random() < 1.0 / 100.0) {
            if (!(entity instanceof GeoEntity)) { AnimationDodgeProcedure.execute(world, entity); cancelEvent(event); return; }
        }
        if (chestArmor.getItem() == JujutsucraftaddonModItems.WUKONG_SET_CHESTPLATE.get().asItem() && Math.random() < 1.0 / 20.0) {
            sourceentity.hurt(damagesource, (float) amount / 10.0F);
        }

        // --- SECTION 2: BASE CANCEL LOGIC ---
        if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo"))) 
            && !sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo_no_move"))) 
            && LogicStartPassiveProcedure.execute(entity)) {
            
            if (pVars != null && (pVars.PlayerCurseTechnique == 38.0 || pVars.PlayerCurseTechnique2 == 38.0 || entity instanceof UroTakakoEntity)) {
                if (entity instanceof LivingEntity _liv && !_liv.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get()) 
                    && sourceentity.getBbWidth() + sourceentity.getBbHeight() <= (entity.getBbWidth() + entity.getBbHeight()) * 4.0F
                    && _liv.hasEffect(JujutsucraftModMobEffects.GUARD.get())) cancel = true;
            }
            if (entity instanceof LivingEntity _liv && !_liv.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get()) 
                && _liv.hasEffect(JujutsucraftModMobEffects.PRAYER_SONG.get())) {
                if (_liv.hasEffect(JujutsucraftModMobEffects.GUARD.get()) && _liv.getEffect(JujutsucraftModMobEffects.GUARD.get()).getAmplifier() > 0) cancel = true;
                else if (Math.random() < 1.0 / 120.0) cancel = true; // RESTORED 1/120 chance
            }
        }

        // RESTORED: Specific Entity Protection
        if (immediatesourceentity instanceof BulletBallProjectileEntity || entity instanceof DomainExpansionEntityEntity) cancel = true;

        if (!cancel && !damagesource.is(TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("forge:curse"))) 
            && !damagesource.is(TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:damage_combat")))) {
            
            LivingEntity sourceTarget = (sourceentity instanceof Mob _mob) ? _mob.getTarget() : null;
            LivingEntity entityTarget = (entity instanceof Mob _mob) ? _mob.getTarget() : null;
            if (sourceTarget != null && sourceTarget == entityTarget) cancel = true;

            if (!cancel && (entity instanceof TamableAnimal _t1 && _t1.isTame() || sourceentity instanceof TamableAnimal _t2 && _t2.isTame())) {
                Entity owner1 = (entity instanceof TamableAnimal _t) ? _t.getOwner() : null;
                Entity owner2 = (sourceentity instanceof TamableAnimal _t) ? _t.getOwner() : null;
                if (owner1 == sourceentity || owner2 == entity || (owner1 != null && owner1 == owner2)) cancel = true;
            }

            if (!world.getLevelData().getGameRules().getBoolean(JujutsucraftModGameRules.JUJUTSUPVP)
                && (sourceentity instanceof Player || sourceentity.getPersistentData().getBoolean("Player")) 
                && (entity instanceof Player || entity.getPersistentData().getBoolean("Player"))) {
                cancel = true;
                if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get()) 
                    || sourceentity instanceof LivingEntity _livS && _livS.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) cancel = false;
            }

            if ((entity instanceof Mob _m && _m.getTarget() == sourceentity && entity.getPersistentData().getDouble("cnt_target") > 6.0) 
                || (sourceentity instanceof Mob _m2 && _m2.getTarget() == entity && sourceentity.getPersistentData().getDouble("cnt_target") > 6.0)) cancel = false;

            if (!cancel) {
                if (sourceentity.getPersistentData().getDouble("friend_num") != 0.0 && sourceentity.getPersistentData().getDouble("friend_num") == entity.getPersistentData().getDouble("friend_num")) cancel = true;
                if ((damagesource.is(DamageTypes.MOB_ATTACK) || damagesource.is(DamageTypes.PLAYER_ATTACK))) {
                    int combatAmp = (sourceentity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get())) ? _liv.getEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get()).getAmplifier() : 0;
                    if (combatAmp > 0 || sourceentity.getPersistentData().getDouble("skill") <= -999.0) cancel = true;
                }
            }
        }

        if (cancel) { cancelEvent(event); return; }

        // --- SECTION 3: DAMAGE CALCULATION & ADAPTATION ---
        ItemStack mainHandItem = (sourceentity instanceof LivingEntity _liv) ? _liv.getMainHandItem().copy() : ItemStack.EMPTY;
        Entity ownerEntity = sourceentity;
        if (!sourceentity.getPersistentData().getString("OWNER_UUID").isEmpty() && (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo"))) || sourceentity.getPersistentData().getDouble("NameRanged_ranged") != 0.0)) {
            ownerEntity = GetEntityFromUUIDProcedure.execute(world, sourceentity.getPersistentData().getString("OWNER_UUID"));
            if (!(ownerEntity instanceof LivingEntity)) ownerEntity = sourceentity;
        }

        EntityAutoGuardProcedure.execute(world, entity, ownerEntity);
        CounterProcedure.execute(entity);
        SetAttributeMainhandProcedure.execute((sourceentity instanceof LivingEntity _liv) ? _liv.getMainHandItem() : ItemStack.EMPTY);

        boolean useCurse = false;
        boolean changeDamage = false;
        boolean guard = false;

        if (damagesource.is(TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("forge:curse"))) 
            || damagesource.is(TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("forge:combat")))) {
            if (!sourceentity.getPersistentData().getBoolean("attack") && mainHandItem.getOrCreateTag().getDouble("CursePower") != 0.0) useCurse = true;
        } else if (!(immediatesourceentity instanceof Projectile)) {
            CursedToolsAbilityProcedure.execute(sourceentity, entity);
            EffectAttackProcedure.execute(world, entity, sourceentity);

            if (mainHandItem.is(ItemTags.create(new ResourceLocation("forge:cursed_tool")))) {
                if (mainHandItem.getItem() == JujutsucraftModItems.EXECUTIONERS_SWORD.get()) { damageAmount = 1.0; changeDamage = true; }
                if (mainHandItem.getItem() == JujutsucraftModItems.FESTER_LIFE_BLADE.get()) changeDamage = true;
                if (mainHandItem.getItem() == JujutsucraftModItems.PLAYFUL_CLOUD.get()) { damageAmount *= 1.25; changeDamage = true; }
            }

            // v43 RESTORED: Mahoraga Adaptation Bonus (NUM1 calculation)
            ItemStack headItem = (sourceentity instanceof LivingEntity _liv) ? _liv.getItemBySlot(EquipmentSlot.HEAD).copy() : ItemStack.EMPTY;
            double adaptationBonus = 0.0;
            if ((!(sourceentity instanceof Player _p) || !_p.getCooldowns().isOnCooldown(headItem.getItem())) && (headItem.getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get() || headItem.getItem() == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get())) {
                JujutsucraftModVariables.PlayerVariables sourceVars = sourceentity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
                if (sourceentity instanceof Player ? (sourceVars != null && (sourceVars.PlayerCurseTechnique == 16.0 || sourceVars.PlayerCurseTechnique2 == 16.0)) : (sourceentity instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity || sourceentity instanceof CursedSpiritGrade010Entity)) {
                    if (headItem.getOrCreateTag().getDouble("toLiving") >= 100.0) {
                        adaptationBonus = Math.round(Math.floor(headItem.getOrCreateTag().getDouble("toLiving") / 100.0) * 2.5);
                        adaptationBonus = entity.getPersistentData().getBoolean("CursedSpirit") ? adaptationBonus * -1.0 : adaptationBonus;
                    }
                }
            }

            // RESTORED: Generic CursePower Scaling (Fusion)
            double cp = mainHandItem.getOrCreateTag().getDouble("CursePower") + adaptationBonus;
            if (cp > 0.0) { damageAmount *= 1.0 + cp * 0.02; changeDamage = true; useCurse = true; }
            else if (cp < 0.0 && entity.getPersistentData().getBoolean("CursedSpirit")) {
                // Sword of Extermination Cooldown Check (RESTORED)
                if (!(mainHandItem.getItem() == JujutsucraftModItems.SWORD_OF_EXTERMINATION.get() && sourceentity instanceof Player _p && _p.getCooldowns().isOnCooldown(mainHandItem.getItem()))) {
                    damageAmount *= 1.0 + Math.abs(cp) * 0.2; changeDamage = true; useCurse = true;
                }
            }

            // v43 Criticals
            if (sourceentity instanceof LivingEntity _livS && _livSourceHasEffect(_livS, JujutsucraftModMobEffects.SPECIAL.get()) && entity instanceof LivingEntity _livE && _livSourceHasEffect(_livE, JujutsucraftModMobEffects.SPECIAL.get())) {
                _livE.removeEffect(JujutsucraftModMobEffects.SPECIAL.get());
                damageAmount *= 1.5; changeDamage = true;
                playCriticalEffect(world, entity, "jujutsucraft:crush", (SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_NANAMI_2.get());
            }
            if (entity instanceof LivingEntity _livE && _livE.getPercentFrozen() * 100.0F >= 5.0F) {
                _livE.setTicksFrozen(0);
                damageAmount *= 1.5; changeDamage = true;
                playCriticalEffect(world, entity, "jujutsucraft:glass_crash", null);
                if (world instanceof ServerLevel _level) _level.levelEvent(2001, BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ()), Block.getId(Blocks.ICE.defaultBlockState()));
            }
        }

        // --- SECTION 4: DEFENSE & FINAL APPLICATION ---
        // RESTORED: NBT "Damage" Defense
        if ((entity.getPersistentData().getDouble("skill") != 0.0 || (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftModMobEffects.GUARD.get())) || entity.getPersistentData().getBoolean("guard"))) {
            if (entity.getPersistentData().getDouble("Damage") > 0.0 && !entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo"))) && entity != sourceentity) {
                damageAmount = Math.max(damageAmount - entity.getPersistentData().getDouble("Damage"), 0.0);
                changeDamage = true;
                guard = true;
            }
        }

        // v43 RESTORED: Falling Blossom Emotion Defense
        if (ownerEntity instanceof LivingEntity _livO && _livO.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && entity instanceof LivingEntity _livE && _livE.hasEffect(JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get())) {
            if (entity instanceof LivingEntity _livN && _livN.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get()) && _livN.getEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get()).getAmplifier() > 0) {
                double red = 15.0 * (1.0 + (_livE.hasEffect(MobEffects.DAMAGE_BOOST) ? _livE.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0) * 0.33);
                if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo"))) || sourceentity.getPersistentData().getBoolean("DomainAttack")) {
                    damageAmount = Math.max(damageAmount - red, 0.0);
                    changeDamage = true;
                }
                // Counter projectiles
                if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))) {
                    sourceentity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:damage_curse"))), entity), (float) red);
                }
            }
        }

        // RESTORED: Cursed Spirit Immunity
        if (entity.getPersistentData().getBoolean("CursedSpirit") && !useCurse) {
            boolean immune = false;
            if (sourceentity instanceof Player _p) {
                if (((JujutsucraftModVariables.PlayerVariables) _p.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables())).PlayerCursePowerFormer < 100.0 && !LogicUseMinePieceProcedure.execute(_p)) immune = true;
            } else if (!sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:no_curse_power")))) immune = true;
            if (immune) { damageAmount = 0.0; changeDamage = true; }
        }

        // RESTORED: Soul Perception / Mahito
        boolean mahitoType = (entity instanceof Player ? (((JujutsucraftModVariables.PlayerVariables) entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables())).PlayerCurseTechnique == 15.0 || ((JujutsucraftModVariables.PlayerVariables) entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables())).PlayerCurseTechnique2 == 15.0) : entity instanceof MahitoEntity);
        if (mahitoType && entity instanceof LivingEntity _livE && !_livE.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get()) && !_livE.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())) {
            boolean seesSoul = false;
            if (ownerEntity instanceof ServerPlayer _sp && _sp.server != null) seesSoul = _sp.getAdvancements().getOrStartProgress(Objects.requireNonNull(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:observation_of_the_soul")))).isDone();
            else if (ownerEntity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:soul_perception")))) seesSoul = true;
            if (!seesSoul && !sourceentity.getPersistentData().getBoolean("ignore")) { damageAmount *= 0.5; changeDamage = true; }
        }

        // RESTORED: Difficulty Multiplier
        if (entity instanceof Player && (ownerEntity.getPersistentData().getBoolean("jjkChara") || ownerEntity instanceof Player)) {
            damageAmount *= GetDifficultyLevelProcedure.execute(world);
            changeDamage = true;
        }

        if (changeDamage) {
            if (sourceentity.getPersistentData().getBoolean("ignore") && entity instanceof LivingEntity _livE && !_livE.level().isClientSide()) _livE.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.IGNORE_GUARD.get(), 1, 0, false, false));
            double oldHealth = (entity instanceof LivingEntity _liv) ? _liv.getHealth() : -1.0;
            if (damageAmount > 0.0) {
                ResourceLocation dType = ModList.get().isLoaded("minepiece") ? new ResourceLocation("jujutsucraft:damage_curse") : new ResourceLocation("jujutsucraft:damage_combat");
                entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, dType)), ownerEntity), (float) damageAmount);
            }

            // v43 Stun & Domain Break
            if (entity instanceof LivingEntity _liv && _liv.getHealth() != oldHealth) {
                double ratio = (oldHealth - _liv.getHealth()) / Math.max(_liv.getMaxHealth(), 0.1) * (entity instanceof Player ? 0.5 : 1.0);
                if (ratio >= 0.05 && !_liv.level().isClientSide()) {
                    _liv.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.STUN.get(), 10, 0, false, false));
                    if (ratio >= 0.1) {
                        _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) Math.round(Math.min(ratio * 75.0, 60.0)), (int) Math.round(ratio * 5.0), false, false));
                        _liv.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME.get(), (int) Math.round(Math.min(ratio * 75.0, 60.0)), 0, false, false));
                        if (ratio >= 0.2) {
                            _liv.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.STUN.get(), (int) Math.round(Math.min(ratio * 75.0, 60.0)), 1, false, false));
                            double domDur = _liv.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) ? _liv.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getDuration() : 0;
                            if (domDur > 0 && domDur <= 600) _liv.removeEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                        }
                    }
                }
            }

            // Visual Effects & Procedures (RESTORED)
            if (!damagesource.is(TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("forge:curse")))) {
                if (entity instanceof LivingEntity _livE && !_livE.hasEffect(JujutsucraftModMobEffects.DAMAGE_EFFECT.get())) {
                    if (!_livE.level().isClientSide()) {
                        _livE.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.DAMAGE_EFFECT.get(), 3, 0, false, false));
                        _livE.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME_GUARD.get(), 10, 0, false, false));
                    }
                    // Re-application for parity
                    if (entity instanceof LivingEntity _livRedundant) {
                        _livRedundant.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.DAMAGE_EFFECT.get(), 3, 0, false, false));
                        _livRedundant.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME_GUARD.get(), 10, 0, false, false));
                    }
                }
                if (guard) GuardEffectProcedureProcedure.execute(world, entity.getX() + entity.getBbWidth() * (Math.random() - 0.5), entity.getY() + entity.getBbHeight() * Math.random(), entity.getZ() + entity.getBbWidth() * (Math.random() - 0.5), sourceentity, entity);
                DamageEffectProcedureProcedure.execute(world, sourceentity, entity, amount);
            }

            // Projectile Discard (RESTORED v43)
            if (immediatesourceentity instanceof Projectile && !immediatesourceentity.level().isClientSide()) immediatesourceentity.discard();

            // COOLDOWN_TICKS Reset (RESTORED v43)
            entity.getPersistentData().putDouble("COOLDOWN_TICKS", oldCooldown);
            cancelEvent(event);
        }
    }

    private static void applyDodgeRigor(LevelAccessor world, Entity entity, double skill) {
        entity.getPersistentData().putDouble("skill", skill);
        entity.getPersistentData().putDouble("cnt6", 4.0);
        double yaw = Math.toRadians(entity.getYRot() + 90.0F);
        double pitch = Math.toRadians(entity.getXRot());
        entity.getPersistentData().putDouble("x_pos", entity.getX() + Math.cos(yaw) * Math.cos(pitch) * (2.0 + entity.getBbWidth()));
        entity.getPersistentData().putDouble("y_pos", entity.getY() + (double) entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * (double) (2.0 + entity.getBbWidth()));
        entity.getPersistentData().putDouble("z_pos", entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * (2.0 + entity.getBbWidth()));
        if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) _liv.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), 60, 1, false, false));
        AnimationDodgeProcedure.execute(world, entity);
    }

    private static void cancelEvent(Event event) {
        if (event != null && event.isCancelable()) {
            event.setCanceled(true);
        } else if (event != null && event.hasResult()) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static boolean _livSourceHasEffect(LivingEntity liv, MobEffect effect) {
        return liv.hasEffect(effect) && liv.getEffect(effect).getAmplifier() >= 0;
    }

    private static void playCriticalEffect(LevelAccessor world, Entity entity, String sound, SimpleParticleType particle) {
        if (world instanceof Level _level) {
            SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(sound));
            if (!_level.isClientSide()) _level.playSound(null, BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ()), s, SoundSource.NEUTRAL, 1.0F, 1.0F);
            else _level.playLocalSound(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), s, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
        }
        if (world instanceof ServerLevel _level && particle != null) {
            _level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 1, 0.5, 0.5, 0.5, 0.25);
            _level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 25, 0.25, 0.25, 0.25, 0.5);
        }
    }
}
