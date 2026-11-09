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
import org.lwjgl.glfw.GLFW;

public class PearlMirrorMod implements ClientModInitializer {
    private static PearlMirrorConfig config;
    private static KeyMapping mirrorKey;
    private static boolean isThrowing = false;
    private static float targetYaw = 0;
    private static float targetPitch = 0;

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

            // Ищем перл врага в радиусе (по имени класса, чтобы не зависеть от маппингов)
            Entity enemyPearl = null;
            for (Entity e : client.level.entitiesForRendering()) {
                if (e.getClass().getSimpleName().contains("Enderpearl")) {
                    // Проверяем, что это не наш перл
                    if (e != client.player.getVehicle() && e.distanceTo(client.player) <= config.radius) {
                        // Проверяем владельца, если метод есть
                        try {
                            Object owner = e.getClass().getMethod("getOwner").invoke(e);
                            if (owner != client.player) {
                                enemyPearl = e;
                                break;
                            }
                        } catch (Exception ex) {
                            // Если метода нет, просто берем перл
                            enemyPearl = e;
                            break;
                        }
                    }
                }
            }

            if (enemyPearl != null) {
                targetYaw = enemyPearl.getYRot();
                targetPitch = enemyPearl.getXRot();
            }

            while (mirrorKey.consumeClick()) {
                if (enemyPearl != null) {
                    int pearlSlot = findPearl(client);
                    if (pearlSlot != -1) {
                        client.player.getInventory().setSelectedSlot(pearlSlot);
                        client.player.setYRot(targetYaw);
                        client.player.setXRot(targetPitch);
                        isThrowing = true;
                    } else {
                        client.player.displayClientMessage(Component.literal("§cПерла нет в хотбаре!"), true);
                    }
                } else {
                    client.player.displayClientMessage(Component.literal("§cПерл врага не найден!"), true);
                }
            }

            if (isThrowing) {
                client.options.keyUse.setDown(true);
                isThrowing = false;
            } else {
                client.options.keyUse.setDown(false);
            }
        });
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
