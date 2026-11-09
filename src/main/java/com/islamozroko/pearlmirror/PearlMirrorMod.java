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

import java.util.ArrayList;
import java.util.List;

public class PearlMirrorMod implements ClientModInitializer {
    private static PearlMirrorConfig config;
    private static KeyMapping mirrorKey;
    private static boolean isThrowing = false;
    private static int throwTicks = 0;
    private static float targetYaw = 0;
    private static float targetPitch = 0;
    
    // Список активных перлов
    public static final List<Entity> activePearls = new ArrayList<>();

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

            // Чистим список от мертвых перлов и далеких
            activePearls.removeIf(p -> !p.isAlive() || p.distanceTo(client.player) > config.radius);

            // Ищем ближайший перл врага
            Entity enemyPearl = null;
            double closestDist = config.radius;

            for (Entity pearl : activePearls) {
                // Пытаемся проверить владельца (чтобы не брать свой перл)
                boolean isMine = false;
                try {
                    Object owner = pearl.getClass().getMethod("getOwner").invoke(pearl);
                    if (owner == client.player) {
                        isMine = true;
                    }
                } catch (Exception ignored) {}

                if (!isMine) {
                    double dist = pearl.distanceTo(client.player);
                    if (dist < closestDist) {
                        closestDist = dist;
                        enemyPearl = pearl;
                    }
                }
            }

            // Если нашли, запоминаем углы
            if (enemyPearl != null) {
                Entity owner = null;
                try {
                    owner = (Entity) enemyPearl.getClass().getMethod("getOwner").invoke(enemyPearl);
                } catch (Exception ignored) {}

                if (owner != null) {
                    // Берем углы у игрока, который кинул перл
                    targetYaw = owner.getYRot();
                    targetPitch = owner.getXRot();
                } else {
                    // Если владельца нет, смотрим строго горизонтально
                    targetYaw = enemyPearl.getYRot();
                    targetPitch = 0;
                }
            }

            // Нажатие кнопки
            while (mirrorKey.consumeClick()) {
                if (enemyPearl != null) {
                    int pearlSlot = findPearl(client);
                    if (pearlSlot != -1) {
                        client.player.getInventory().setSelectedSlot(pearlSlot);
                        client.player.setYRot(targetYaw);
                        client.player.setXRot(targetPitch);
                        isThrowing = true;
                        throwTicks = 2; // Держим ПКМ 2 тика
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
