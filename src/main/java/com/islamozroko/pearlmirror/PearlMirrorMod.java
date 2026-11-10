package com.islamozroko.pearlmirror;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class PearlMirrorMod implements ClientModInitializer {
    private static PearlMirrorConfig config;
    private static KeyMapping mirrorKey;
    
    // Точка падения
    public static Vec3 landingPoint = null;
    
    private static boolean isThrowing = false;
    private static int throwTicks = 0;

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("pearlmirror", "main")
    );

    @Override
    public void onInitializeClient() {
        config = PearlMirrorConfig.load();

        mirrorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pearlmirror.mirror",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!config.enabled || client.player == null || client.level == null) return;

            // Нажатие кнопки
            while (mirrorKey.consumeClick()) {
                if (landingPoint != null) {
                    int pearlSlot = findPearl(client);
                    if (pearlSlot != -1) {
                        client.player.getInventory().setSelectedSlot(pearlSlot);
                        
                        // Считаем углы до точки падения
                        Vec3 eyes = client.player.getEyePosition();
                        double dx = landingPoint.x - eyes.x;
                        double dy = landingPoint.y - eyes.y;
                        double dz = landingPoint.z - eyes.z;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        
                        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
                        float pitch = (float) -(Math.atan2(dy, dist) * 180.0 / Math.PI);
                        
                        // Поворачиваем камеру на эту точку
                        client.player.setYRot(yaw);
                        client.player.setXRot(pitch);
                        
                        isThrowing = true;
                        throwTicks = 2;
                        landingPoint = null; // Сбрасываем точку после броска
                    } else {
                        client.player.displayClientMessage(Component.literal("§cПерла нет в хотбаре!"), true);
                    }
                } else {
                    client.player.displayClientMessage(Component.literal("§cПерл врага не найден!"), true);
                }
            }

            // Логика броска
            if (isThrowing) {
                if (throwTicks > 0) {
                    throwTicks--;
                    client.options.keyUse.setDown(true);
                } else {
                    client.options.keyUse.setDown(false);
                    isThrowing = false;
                }
            }
        });
    }

    // Метод для миксина (симуляция полета)
    public static void onPearlSpawned(Entity pearl) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || pearl == null) return;
        
        boolean isMine = false;
        try {
            Object owner = pearl.getClass().getMethod("getOwner").invoke(pearl);
            if (owner == client.player) isMine = true;
        } catch (Exception ignored) {}

        if (!isMine && pearl.distanceTo(client.player) <= config.radius) {
            // Симулируем полет перла, чтобы найти точку падения
            Vec3 pos = pearl.position();
            Vec3 motion = pearl.getDeltaMovement();
            
            // Гравитация (примерно 0.03 в тик)
            double gravity = 0.03;
            
            for (int i = 0; i < 100; i++) { // Максимум 100 тиков полета
                // Проверяем, не упал ли перл на землю или в воду
                if (client.level.getBlockState(net.minecraft.core.BlockPos.containing(pos)).isSolid()) {
                    break;
                }
                
                // Обновляем позицию
                pos = pos.add(motion);
                // Применяем гравитацию
                motion = motion.add(0, -gravity, 0);
                // Трение (примерно 0.99)
                motion = motion.multiply(0.99, 0.99, 0.99);
            }
            
            landingPoint = pos;
        }
    }

    private int findPearl(Minecraft client) {
        if (client.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        return -1;
    }

    public static PearlMirrorConfig getConfig() {
        return config;
    }
}
