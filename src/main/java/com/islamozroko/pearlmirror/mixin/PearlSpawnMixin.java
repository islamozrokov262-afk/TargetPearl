package com.islamozroko.pearlmirror.mixin;

import com.islamozroko.pearlmirror.PearlMirrorMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class PearlSpawnMixin {
    
    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void onEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        Entity entity = mc.level.getEntity(packet.getId());
        
        // Если это перл - передаем его в наш мод
        if (entity != null && entity.getType() == EntityType.ENDER_PEARL) {
            PearlMirrorMod.onPearlSpawned(entity);
        }
    }
}
