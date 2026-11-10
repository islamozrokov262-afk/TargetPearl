package com.islamozroko.pearlmirror.mixin;

import com.islamozroko.pearlmirror.PearlMirrorMod;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EnderPearlSpawnMixin {
    // Вместо addEntity в Level, мы цепляемся за метод baseTick (он есть у всех сущностей)
    @Inject(method = "baseTick", at = @At("HEAD"))
    private void pearlMirror$onEntityTick(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        
        // Проверяем, что это эндер-перл по имени сущности
        if (entity.getType().toString().contains("ender_pearl")) {
            // Вызываем наш метод только 1 раз при спавне
            // (проверка на tickCount == 0 гарантирует, что мы поймаем только что заспавненный перл)
            if (entity.tickCount == 0) {
                PearlMirrorMod.onPearlSpawned(entity);
            }
        }
    }
}
