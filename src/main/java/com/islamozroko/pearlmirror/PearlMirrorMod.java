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
    
    public static float savedYaw = 0;
    public static float savedPitch = 0;
    public static boolean hasSavedPearl = false;
    
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
                if (hasSavedPearl) {
                    int pearlSlot = findPearl(client);
                    if (pearlSlot != -1) {
                        // Выключаем килку (эмулируем нажатие R)
                        setKeyPressed(config.auraKeyCode, true);
                        setKeyPressed(config.auraKeyCode, false);
                        
                        client.player.getInventory().setSelectedSlot(pearlSlot);
                        client.player.setYRot(savedYaw);
                        client.player.setXRot(savedPitch);
                        
                        isThrowing = true;
                        throwTicks = 2;
                        hasSavedPearl = false;
                    } else {
                        client.player.displayClientMessage(Component.literal("§cПерла нет в хотбаре!"), true);
                    }
                } else {
                    client.player.displayClientMessage(Component.literal("§cПерл врага не найден!"), true);
                }
            }

            // Бросок
            if (isThrowing) {
                if (throwTicks > 0) {
                    throwTicks--;
                    client.player.setYRot(savedYaw);
                    client.player.setXRot(savedPitch);
                    client.options.keyUse.setDown(true);
                } else {
                    client.options.keyUse.setDown(false);
                    isThrowing = false;
                }
            }
        });
    }

    // Метод для миксина
    public static void onPearlSpawned(Entity pearl) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || pearl == null) return;
        
        boolean isMine = false;
        try {
            Object owner = pearl.getClass().getMethod("getOwner").invoke(pearl);
            if (owner == client.player) isMine = true;
        } catch (Exception ignored) {}

        if (!isMine && pearl.distanceTo(client.player) <= config.radius) {
            Entity owner = null;
            try {
                owner = (Entity) pearl.getClass().getMethod("getOwner").invoke(pearl);
            } catch (Exception ignored) {}

            if (owner != null) {
                savedYaw = owner.getYRot();
                savedPitch = owner.getXRot() + (float) config.pitchBoost;
            } else {
                savedYaw = pearl.getYRot();
                savedPitch = (float) config.pitchBoost;
            }
            hasSavedPearl = true;
        }
    }

    private void setKeyPressed(int keyCode, boolean pressed) {
        KeyMapping.set(InputConstants.Type.KEYSYM.getOrCreate(keyCode), pressed);
        KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
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
