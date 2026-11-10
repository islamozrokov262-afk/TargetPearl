package com.islamozroko.pearlmirror;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PearlMirrorMod implements ClientModInitializer {
    private static PearlMirrorConfig config;
    private static KeyMapping mirrorKey;

    public static Vec3 landingPoint = null;
    private static final Set<UUID> seenPearls = new HashSet<>();

    private static boolean isThrowing = false;
    private static int throwTicks = 0;
    private static int throwCooldown = 0;
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

            if (throwCooldown > 0) throwCooldown--;

            while (mirrorKey.consumeClick()) {
                if (throwCooldown > 0) break;
                if (landingPoint == null) {
                    client.player.displayClientMessage(Component.literal("§cПерл врага не найден!"), true);
                    break;
                }

                int pearlSlot = findPearl(client);
                if (pearlSlot == -1) {
                    client.player.displayClientMessage(Component.literal("§cПерла нет в хотбаре!"), true);
                    break;
                }

                Vec3 eyes = client.player.getEyePosition();
                float[] angles = calculateThrowAngles(client, eyes, landingPoint);

                oldSlot = client.player.getInventory().getSelectedSlot();
                client.player.getInventory().setSelectedSlot(pearlSlot);
                client.player.setYRot(angles[0]);
                client.player.setXRot(angles[1]);

                isThrowing = true;
                throwTicks = config.throwTicks;
                landingPoint = null;
                throwCooldown = config.delayTicks;
            }

            if (isThrowing) {
                if (throwTicks > 0) {
                    throwTicks--;
                    client.options.keyUse.setDown(true);
                } else {
                    client.options.keyUse.setDown(false);
                    isThrowing = false;
                    if (oldSlot >= 0) {
                        client.player.getInventory().setSelectedSlot(oldSlot);
                        oldSlot = -1;
                    }
                }
            }
        });
    }

    public static void onPearlSpawned(Entity pearl) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || pearl == null || config == null) return;

        Entity owner = null;
        try {
            if (pearl instanceof ThrownEnderpearl ep) {
                owner = ep.getOwner();
            }
        } catch (Throwable ignored) {}

        if (owner == client.player) return;
        String ownerName = owner == null ? "" : owner.getName().getString();

        if (!config.allPearls && pearl.distanceTo(client.player) > config.radius) return;
        if (!config.allPearls && pearl.tickCount > 2) return;

        if (config.ignoreTeammates && !ownerName.isEmpty() && config.teammateList.contains(ownerName)) return;
        if (config.onlyFromList && !config.playerList.contains(ownerName)) return;

        if (config.onlyAuraTarget) {
            Entity auraTarget = getAuraTarget();
            if (auraTarget == null) {
                if (!config.fallbackToAny) return;
            } else if (owner != auraTarget) {
                return;
            }
        }

        if (config.minHearts > 0.0F && owner instanceof LivingEntity living) {
            if (living.getHealth() / 2.0F < config.minHearts) return;
        }

        UUID id = pearl.getUUID();
        if (!seenPearls.add(id)) return;
        if (seenPearls.size() > 256) seenPearls.clear();

        Vec3 landing = predictPearlLanding(client, pearl);
        if (landing == null) return;

        if (config.preferClosest && landingPoint != null) {
            double newDist = client.player.position().distanceTo(landing);
            double oldDist = client.player.position().distanceTo(landingPoint);
            if (newDist >= oldDist) return;
        }

        landingPoint = landing;
    }

    private static Entity getAuraTarget() {
        return null;
    }

    private static Vec3 predictPearlLanding(Minecraft client, Entity pearl) {
        Vec3 pos = pearl.position();
        Vec3 motion = pearl.getDeltaMovement();

        for (int i = 0; i < 200; i++) {
            Vec3 next = pos.add(motion);
            BlockHitResult hit = client.level.clip(new ClipContext(
                    pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, pearl
            ));

            if (hit.getType() != HitResult.Type.MISS) return hit.getLocation();

            boolean inWater = !client.level.getFluidState(BlockPos.containing(pos)).isEmpty();
            double drag = inWater ? 0.8 : 0.99;
            motion = new Vec3(motion.x * drag, motion.y * drag - 0.03, motion.z * drag);
            pos = next;

            if (pos.y < client.level.getMinY() - 64) return pos;
        }
        return pos;
    }

    private static float[] calculateThrowAngles(Minecraft client, Vec3 eyes, Vec3 landing) {
        double dx = landing.x - eyes.x;
        double dz = landing.z - eyes.z;
        double dy = landing.y - eyes.y;

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float bestPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        double bestDist = Double.MAX_VALUE;

        for (float pitch = 89.0F; pitch >= -90.0F; pitch -= 1.0F) {
            Vec3 sim = simulateThrow(client, eyes, yaw, pitch);
            double dist = sim.distanceTo(landing);
            if (dist < bestDist) {
                bestDist = dist;
                bestPitch = pitch;
            }
        }

        float refined = bestPitch;
        for (float pitch = bestPitch - 1.0F; pitch <= bestPitch + 1.0F; pitch += 0.1F) {
            Vec3 sim = simulateThrow(client, eyes, yaw, pitch);
            double dist = sim.distanceTo(landing);
            if (dist < bestDist) {
                bestDist = dist;
                refined = pitch;
            }
        }
        return new float[]{yaw, refined};
    }

    private static Vec3 simulateThrow(Minecraft client, Vec3 eyes, float yaw, float pitch) {
        double speed = 1.5D;
        double vx = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * speed;
        double vy = -Math.sin(Math.toRadians(pitch)) * speed;
        double vz = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * speed;

        Vec3 pos = eyes;
        Vec3 motion = new Vec3(vx, vy, vz);

        for (int i = 0; i < 200; i++) {
            Vec3 next = pos.add(motion);
            BlockHitResult hit = client.level.clip(new ClipContext(
                    pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, client.player
            ));

            if (hit.getType() != HitResult.Type.MISS) return hit.getLocation();

            boolean inWater = !client.level.getFluidState(BlockPos.containing(pos)).isEmpty();
            double drag = inWater ? 0.8 : 0.99;
            motion = new Vec3(motion.x * drag, motion.y * drag - 0.03, motion.z * drag);
            pos = next;

            if (pos.y < client.level.getMinY() - 64) return pos;
        }
        return pos;
    }

    private static int findPearl(Minecraft client) {
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
