package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.IgrisEntity;
import com.jujutsu.jujutsucraftaddon.entity.Shadow1Entity;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(value = EffectCharactorProcedure.class, priority = -10000)
public abstract class EffectCharactorProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, Entity entityiterator, CallbackInfo ci) {
        ci.cancel();

        if (entity != null && entityiterator != null) {
            String STR1 = "";
            Entity entity_a = null;
            Entity entity_b = null;
            boolean logic_start = false;
            boolean logic_a = false;
            boolean cursed_technique = false;
            double x_pos = 0.0;
            double z_pos = 0.0;
            double NUM1 = 0.0;
            double old_cool = 0.0;
            double y_pos = 0.0;
            double T1 = 0.0;
            double T2 = 0.0;
            ItemStack item_A = ItemStack.EMPTY;
            ItemStack equipment_item_offhand = ItemStack.EMPTY;
            ItemStack equipment_item = ItemStack.EMPTY;
            entity_a = entityiterator;
            if (entityiterator instanceof LivingEntity) {
                cursed_technique = entity.getPersistentData().getDouble("skill") > 100.0 && !entity.getPersistentData().getBoolean("attack");
                x_pos = entityiterator.getX();
                y_pos = entityiterator.getY() + entityiterator.getBbHeight() * 0.5;
                z_pos = entityiterator.getZ();
                equipment_item = (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).copy();
                if (equipment_item.getItem() == JujutsucraftModItems.DRAGON_BONE.get() && entity.getPersistentData().getBoolean("attack")) {
                    (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY)
                            .getOrCreateTag()
                            .putDouble(
                                    "power_energy",
                                    Math.min(
                                            equipment_item.getOrCreateTag().getDouble("power_energy") + Math.min(Math.max(entity.getPersistentData().getDouble("cnt6"), 0.1), 5.0),
                                            10.0
                                    )
                            );
                }

                equipment_item = (entityiterator instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).copy();
                equipment_item_offhand = (entityiterator instanceof LivingEntity _livEntxx ? _livEntxx.getOffhandItem() : ItemStack.EMPTY).copy();
                if ((
                        equipment_item.getItem() == JujutsucraftModItems.DRAGON_BONE.get()
                                || equipment_item_offhand.getItem() == JujutsucraftModItems.DRAGON_BONE.get()
                )
                        && (
                        entityiterator.getPersistentData().getBoolean("attack")
                                || entityiterator instanceof LivingEntity _livEnt19 && _livEnt19.hasEffect((MobEffect) JujutsucraftModMobEffects.GUARD.get())
                                || entityiterator.getPersistentData().getBoolean("guard")
                )) {
                    if (equipment_item.getItem() == JujutsucraftModItems.DRAGON_BONE.get()) {
                        (entityiterator instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY)
                                .getOrCreateTag()
                                .putDouble(
                                        "power_energy",
                                        Math.min(
                                                equipment_item.getOrCreateTag().getDouble("power_energy")
                                                        + Math.min(Math.max(entity.getPersistentData().getDouble("cnt6"), 0.1), 5.0),
                                                10.0
                                        )
                                );
                    }

                    if (equipment_item_offhand.getItem() == JujutsucraftModItems.DRAGON_BONE.get()) {
                        (entityiterator instanceof LivingEntity _livEntxxx ? _livEntxxx.getOffhandItem() : ItemStack.EMPTY)
                                .getOrCreateTag()
                                .putDouble(
                                        "power_energy",
                                        Math.min(
                                                equipment_item_offhand.getOrCreateTag().getDouble("power_energy")
                                                        + Math.min(Math.max(entity.getPersistentData().getDouble("cnt6"), 0.1), 5.0),
                                                10.0
                                        )
                                );
                    }
                }

                if (entityiterator instanceof Player) {
                    JujutsucraftModVariables.PlayerVariables pVars = (JujutsucraftModVariables.PlayerVariables) entityiterator.getCapability(
                            JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null
                    )
                            .orElse(new JujutsucraftModVariables.PlayerVariables());
                    T1 = pVars.PlayerCurseTechnique;
                    T2 = pVars.PlayerCurseTechnique2;
                }

                if (entity instanceof EntityWaterEntity
                        && (entity instanceof EntityWaterEntity _datEntI ? (Integer) _datEntI.getEntityData().get(EntityWaterEntity.DATA_type) : 0) == 0
                        || entity instanceof EntityWater2Entity) {
                    if (entityiterator instanceof Player
                            ? T1 != 10.0 && T2 != 10.0
                            : !(entityiterator instanceof ChosoEntity) && !(entityiterator instanceof EsoEntity) && !(entityiterator instanceof KamoNoritoshiEntity)) {
                        if (entityiterator instanceof EntityWaterEntity
                                && (entityiterator instanceof EntityWaterEntity _datEntIx ? (Integer) _datEntIx.getEntityData().get(EntityWaterEntity.DATA_type) : 0)
                                == 2
                                || entityiterator instanceof BloodBallEntity
                                || entityiterator instanceof SlicingExorcismEntity) {
                            if (!entityiterator.level().isClientSide() && entityiterator.getServer() != null) {
                                entityiterator.getServer()
                                        .getCommands()
                                        .performPrefixedCommand(
                                                new CommandSourceStack(
                                                        CommandSource.NULL,
                                                        entityiterator.position(),
                                                        entityiterator.getRotationVector(),
                                                        entityiterator.level() instanceof ServerLevel ? (ServerLevel) entityiterator.level() : null,
                                                        4,
                                                        entityiterator.getName().getString(),
                                                        entityiterator.getDisplayName(),
                                                        entityiterator.level().getServer(),
                                                        entityiterator
                                                ),
                                                "kill @s"
                                        );
                            }

                            if (!entityiterator.level().isClientSide()) {
                                entityiterator.discard();
                            }
                        }
                    } else {
                        old_cool = entityiterator.getPersistentData().getDouble("COOLDOWN_TICKS");
                        if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                            _entity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 60, 0, false, false));
                        }

                        entityiterator.getPersistentData().putDouble("COOLDOWN_TICKS", old_cool);
                    }
                }

                if (entity.getPersistentData().getBoolean("attack")) {
                    EffectAttackProcedure.execute(world, entityiterator, entity);
                }

                if (cursed_technique
                        && !entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:no_cursed_technique")))
                        && entity.getPersistentData().getDouble("skill") != 705.0
                        && (
                        entityiterator instanceof Player
                                ? T1 == 5.0 || T2 == 5.0
                                : entityiterator instanceof OkkotsuYutaEntity || entityiterator instanceof OkkotsuYutaCullingGameEntity
                )
                        && LocateRikaProcedure.execute(world, entityiterator)) {
                    if (entity.getPersistentData().getDouble("skill") >= 305.0 && entity.getPersistentData().getDouble("skill") < 320.0) {
                        if (entityiterator instanceof ServerPlayer _player) {
                            Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:skill_copy_cursed_speech"));
                            AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
                            if (!_ap.isDone()) {
                                for (String criteria : _ap.getRemainingCriteria()) {
                                    _player.getAdvancements().award(_adv, criteria);
                                }
                            }
                        }
                    } else if (entity.getPersistentData().getDouble("skill") == 3810.0
                            && (entityiterator instanceof Player || entityiterator instanceof OkkotsuYutaCullingGameEntity)) {
                        if (entityiterator instanceof ServerPlayer _playerx) {
                            Advancement _adv = _playerx.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:skill_copy_takako_uro"));
                            AdvancementProgress _ap = _playerx.getAdvancements().getOrStartProgress(_adv);
                            if (!_ap.isDone()) {
                                for (String criteria : _ap.getRemainingCriteria()) {
                                    _playerx.getAdvancements().award(_adv, criteria);
                                }
                            }
                        }
                    } else {
                        if (entityiterator instanceof Player) {
                            logic_a = true;
                            AtomicReference<IItemHandler> _iitemhandlerref = new AtomicReference<>();
                            entityiterator.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(_iitemhandlerref::set);
                            if (_iitemhandlerref.get() != null) {
                                for (int _idx = 0; _idx < _iitemhandlerref.get().getSlots(); _idx++) {
                                    ItemStack itemstackiterator = _iitemhandlerref.get().getStackInSlot(_idx).copy();
                                    if (itemstackiterator.getItem() == JujutsucraftModItems.COPIED_CURSED_TECHNIQUE.get()
                                            && itemstackiterator.getOrCreateTag().getDouble("skill") == entity.getPersistentData().getDouble("skill")) {
                                        if (itemstackiterator.getCount() < 10 && entityiterator instanceof Player _playerxx) {
                                            ItemStack _setstack = itemstackiterator.copy();
                                            _setstack.setCount(1);
                                            ItemHandlerHelper.giveItemToPlayer(_playerxx, _setstack);
                                        }

                                        logic_a = false;
                                        break;
                                    }
                                }
                            }

                            item_A = new ItemStack((ItemLike) JujutsucraftModItems.COPIED_CURSED_TECHNIQUE.get()).copy();
                            item_A.setDamageValue(item_A.getMaxDamage() - 1);
                        } else {
                            logic_a = false;
                            NUM1 = 0.0;

                            for (int index0 = 0; index0 < 4; index0++) {
                                item_A = entity_a instanceof LivingEntity _entGetArmor
                                        ? _entGetArmor.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, (int) NUM1))
                                        : ItemStack.EMPTY;
                                if (item_A.getOrCreateTag().getDouble("skill") == 0.0
                                        || item_A.getOrCreateTag().getDouble("skill") == entity.getPersistentData().getDouble("skill")) {
                                    logic_a = true;
                                    break;
                                }

                                if (++NUM1 > 3.0) {
                                    NUM1 = 0.0;
                                }
                            }

                            if (!logic_a) {
                                logic_a = true;
                                NUM1 = Math.floor(Math.random() * 4.0);
                                item_A = entity_a instanceof LivingEntity _entGetArmorx
                                        ? _entGetArmorx.getItemBySlot(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, (int) NUM1))
                                        : ItemStack.EMPTY;
                            }
                        }

                        if (logic_a) {
                            entity_b = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))
                                    ? GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("OWNER_UUID"))
                                    : entity;
                            entity_b = entity_b instanceof LivingEntity ? entity_b : entity;
                            item_A.getOrCreateTag().putDouble("skill", entity.getPersistentData().getDouble("skill"));
                            item_A.getOrCreateTag().putDouble("effect", entity.getPersistentData().getDouble("effect"));
                            item_A.getOrCreateTag()
                                    .putDouble(
                                            "COOLDOWN_TICKS",
                                            Math.round(
                                                    Math.max(
                                                            Math.max(
                                                                    entity instanceof LivingEntity _livEntxxx
                                                                            && _livEntxxx.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get())
                                                                            ? _livEntxxx.getEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get()).getDuration()
                                                                            : 0.0,
                                                                    entity.getPersistentData().getDouble("COOLDOWN_TICKS")
                                                            )
                                                                    * 2.0,
                                                            50.0
                                                    )
                                            )
                                    );
                            if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))) {
                                item_A.getOrCreateTag().putString("SHIKIGAMI_NAME", ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString());
                                item_A.getOrCreateTag().putDouble("SHIKIGAMI_HP", entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMaxHealth() : -1.0);
                            }

                            if (entity_a instanceof Player) {
                                item_A.setHoverName(
                                        Component.literal(
                                                entity_b.getDisplayName().getString()
                                                        + Component.translatable("jujutsu.message.cursed_technique").getString()
                                                        + " ("
                                                        + Component.translatable("jujutsu.overlay.cost").getString()
                                                        + ": "
                                                        + Math.round(item_A.getOrCreateTag().getDouble("COOLDOWN_TICKS"))
                                                        + ")"
                                        )
                                );
                                if (entity_a instanceof Player _playerxx) {
                                    ItemStack _setstack = item_A.copy();
                                    _setstack.setCount(1);
                                    ItemHandlerHelper.giveItemToPlayer(_playerxx, _setstack);
                                }
                            }
                        }
                    }
                }

                if (entity.getPersistentData().getDouble("skill") == 2815.0
                        && entity_a.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))
                        && !entity_a.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:no_cursed_technique")))
                        && !entity_a.isAlive()
                        && !entity_a.level().isClientSide()) {
                    entity_a.discard();
                }

                equipment_item = (entity_a instanceof LivingEntity _entGetArmorx ? _entGetArmorx.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY).copy();
                if ((
                        equipment_item.getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()
                                || equipment_item.getItem() == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get()
                )
                        && !(entity_a instanceof Player _plrCldCheck108 && _plrCldCheck108.getCooldowns().isOnCooldown(equipment_item.getItem()))) {
                    logic_start = false;
                    if (cursed_technique) {
                        STR1 = "skill" + Math.round(entity.getPersistentData().getDouble("skill"));
                        logic_start = true;
                    } else if (entity.getPersistentData().getDouble("skill_domain") != 0.0) {
                        STR1 = "domain" + Math.round(entity.getPersistentData().getDouble("skill_domain"));
                        logic_start = true;
                    }

                    if (logic_start && equipment_item.getOrCreateTag().getDouble(STR1) == 0.0) {
                        NUM1 = 1.0;

                        for (int index1 = 0; index1 < 800; index1++) {
                            if (equipment_item.getOrCreateTag().getString("DATA" + Math.round(NUM1)).equals("")
                                    || equipment_item.getOrCreateTag().getString("DATA" + Math.round(NUM1)).equals(STR1)) {
                                (entity_a instanceof LivingEntity _entGetArmorxx ? _entGetArmorxx.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY)
                                        .getOrCreateTag()
                                        .putString("DATA" + Math.round(NUM1), STR1);
                                (entity_a instanceof LivingEntity _entGetArmorxxx ? _entGetArmorxxx.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY)
                                        .getOrCreateTag()
                                        .putDouble(STR1, 1.0);
                                if (entity_a instanceof Player _playerxx && !_playerxx.level().isClientSide()) {
                                    _playerxx.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.adaptation_start").getString()), false);
                                }
                                break;
                            }

                            NUM1++;
                        }
                    }
                }

                equipment_item = (entity instanceof LivingEntity _entGetArmorxx ? _entGetArmorxx.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY).copy();
                if ((
                        equipment_item.getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()
                                || equipment_item.getItem() == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get()
                )
                        && !(entity instanceof Player _plrCldCheck124 && _plrCldCheck124.getCooldowns().isOnCooldown(equipment_item.getItem()))
                        && entity_a.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))) {
                    if (entity instanceof Player) {
                        JujutsucraftModVariables.PlayerVariables pVars_target = (JujutsucraftModVariables.PlayerVariables) entity.getCapability(
                                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null
                        )
                                .orElse(new JujutsucraftModVariables.PlayerVariables());
                        T1 = pVars_target.PlayerCurseTechnique;
                        T2 = pVars_target.PlayerCurseTechnique2;
                    }

                    boolean canAdapt = false;
                    if (entity instanceof Player _player) {
                        if (T1 == 16.0 || T2 == 16.0) canAdapt = true;
                        if (_player.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables()).Mahoraga == 1.0)
                            canAdapt = true;
                    } else if (entity instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity || entity instanceof CursedSpiritGrade010Entity || entity instanceof IgrisEntity || entity instanceof Shadow1Entity) {
                        canAdapt = true;
                    }

                    if (canAdapt
                            && equipment_item.getOrCreateTag().getDouble("skill" + Math.round(entity_a.getPersistentData().getDouble("skill"))) >= 100.0
                            && !entity_a.level().isClientSide()
                            && entity_a.getServer() != null) {
                        entity_a.getServer()
                                .getCommands()
                                .performPrefixedCommand(
                                        new CommandSourceStack(
                                                CommandSource.NULL,
                                                entity_a.position(),
                                                entity_a.getRotationVector(),
                                                entity_a.level() instanceof ServerLevel ? (ServerLevel) entity_a.level() : null,
                                                4,
                                                entity_a.getName().getString(),
                                                entity_a.getDisplayName(),
                                                entity_a.level().getServer(),
                                                entity_a
                                        ),
                                        "kill @s"
                                );
                    }
                }

                if (!entity_a.isAlive() && entity_a instanceof Player && !(entity instanceof Player)) {
                    entity_b = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))
                            ? GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("OWNER_UUID"))
                            : entity;
                    entity_b = entity_b instanceof LivingEntity ? entity_b : entity;
                    if (!(entity_b instanceof Player)) {
                        NUM1 = 1.0;

                        for (int index2 = 0; index2 < 128; index2++) {
                            STR1 = "pName" + Math.round(NUM1);
                            if (entity_b.getPersistentData().getString(STR1).equals("")) {
                                entity_b.getPersistentData().putString(STR1, entity_a.getDisplayName().getString());
                                break;
                            }

                            NUM1++;
                        }
                    }
                }
            }
        }
    }
}
