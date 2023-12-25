package com.badatusernames.collector;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectorClient implements ClientModInitializer {
    private int tickCounter = 0;
    private Map<String, Integer> blockstateMap = null;
    @Override
    public void onInitializeClient() {
        blockstateMap = createBlockStateMapping();

        // End of tick actions
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return; // Don't run if the world isn't loaded

            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                // Actions that are performed every second
                gatherBlockRecognitionData();
            }
        });
    }

    public void gatherBlockRecognitionData() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Get the player's eye position, which is the starting point for ray tracing
        Vec3d eyePosition = client.player.getCameraPosVec(1.0F);

        // Get the player's looking direction
        Vec3d lookVector = client.player.getRotationVec(1.0F);

        // Define a range for the ray tracing, for example, 10 blocks
        Vec3d targetPosition = eyePosition.add(lookVector.multiply(100));

        // Perform the ray tracing to get the targeted block
        BlockHitResult hitResult = client.world.raycast(new RaycastContext(
                eyePosition,
                targetPosition,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));

        // Check if a block was hit
        if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
            BlockPos blockPos = hitResult.getBlockPos();
            BlockState blockState = client.world.getBlockState(blockPos);

            // Calculate the distance from the player to the block
            double distance = eyePosition.distanceTo(new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));

            // TODO convert BlockStates into ids, and store the BlockState distance pair
        }
    }

    public Map<String, Integer> createBlockStateMapping() {
        Map<String, Integer> stateMap = new HashMap<>();
        int newID = 0;
        for (Block block : Registries.BLOCK) {
            for (BlockState state : block.getStateManager().getStates()) {
                String stateID = state.toString();
                stateMap.put(stateID, newID);
                newID++;
            }
        }
        return stateMap;
    }
}
