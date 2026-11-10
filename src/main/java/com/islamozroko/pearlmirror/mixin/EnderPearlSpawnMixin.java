package com.islamozroko.pearlmirror.mixin;

import com.islamozroko.pearlmirror.PearlMirrorMod;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EnderPearlSpawnMixin {
    @Inject(method = "baseTick", at = @At("HEAD"))
    private void pearlMirror$onEntityTick(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        
        // Отладка: проверяем, видим ли мы перлы вообще
        if (entity.getType().toString().contains("ender_pearl")) {
            if (entity.tickCount <= 1) { // <= 1, чтобы точно не пропустить спавн
                // Отладка в чат (чтобы ты увидел, что миксин работает)
                net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§a[PearlMirror] Миксин увидел перл!"), true
                );
                PearlMirrorMod.onPearlSpawned(entity);
            }
        }
    }
}
