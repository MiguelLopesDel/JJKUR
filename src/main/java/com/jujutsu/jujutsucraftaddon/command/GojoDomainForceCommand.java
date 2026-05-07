package com.jujutsu.jujutsucraftaddon.command;

import com.jujutsu.jujutsucraftaddon.procedures.GojoDomainForceProcedure;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class GojoDomainForceCommand {
    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jjkuForceGojoDomain")
            .requires(s -> s.hasPermission(2))
            .then(Commands.argument("target", EntityArgument.entity())
                .executes(arguments -> {
                    GojoDomainForceProcedure.execute(arguments);
                    return 0;
                })
            )
        );
    }
}
