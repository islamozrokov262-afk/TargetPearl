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
    
    private static int state = 0; // 0 - ничего, 1 - свапнули, 2 - кидаем
    private static int oldSlot = -1;

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
                if (hasSavedPearl && state == 0) {
                    int pearlSlot = findPearl(client);
                    if (pearlSlot != -1) {
                        // Выключаем килку
                        setKeyPressed(config.auraKeyCode, true);
                        setKeyPressed(config.auraKeyCode, false);
                        
                        // Запоминаем старый слот и свапаем на перл
                        oldSlot = client.player.getInventory().getSelectedSlot();
                        client.player.getInventory().setSelectedSlot(pearlSlot);
                        
                        // Поворачиваем камеру
                        client.player.setYRot(savedYaw);
                        client.player.setXRot(savedPitch);
                        
                        state = 1; // Переходим в состояние "кидаем"
                    } else {
                        client.player.displayClientMessage(Component.literal("§cПерла нет в хотбаре!"), true);
                    }
                } else if (!hasSavedPearl) {
                    client.player.displayClientMessage(Component.literal("§cПерл врага не найден!"), true);
                }
            }

            // Логика броска (в следующем тике после свапа)
            if (state == 1) {
                // Держим камеру, чтобы килка не сбила
                client.player.setYRot(savedYaw);
                client.player.setXRot(savedPitch);
                
                // Нажимаем ПКМ (кидаем)
                client.options.keyUse.setDown(true);
                state = 2;
            } else if (state == 2) {
                // Отпускаем ПКМ
                client.options.keyUse.setDown(false);
                
                // Возвращаем старый слот (меч)
                if (oldSlot != -1) {
                    client.player.getInventory().setSelectedSlot(oldSlot);
                    oldSlot = -1;
                }
                
                // Сбрасываем состояние
                state = 0;
                hasSavedPearl = false;
            }
        });
    }

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
            
            if (savedPitch < 0) {
                savedPitch = (float) config.pitchBoost;
            }
            
            hasSavedPearl = true;
        }
    }

    // ВОТ ЭТОТ МЕТОД НУЖЕН ДЛЯ ЭМУЛЯЦИИ КНОПКИ R
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
