package com.badatusernames.collector;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import com.badatusernames.collector.CollectorClient.BlockLabel;

import java.io.File;
import java.io.IOException;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class CollectorClient implements ClientModInitializer {
    private int tickCounter = 0;
    private Map<String, Integer> blockstateMap = null;
    private long startTime = 0;
    private SocketClient sockClient = null;
    @Override
    public void onInitializeClient() {
        startTime = System.currentTimeMillis();
        blockstateMap = createBlockStateMapping();

        // Connect to socket server
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String ipAddress = inetAddress.getHostAddress();
            sockClient = new SocketClient(ipAddress, 5050);
            sockClient.startConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // End of tick actions
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return; // Don't run if the world isn't loaded

            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                long tickStartTime = System.nanoTime();
                // Actions that are performed every second
                BlockLabel label = getRecognitionLabel();
                long elapsedTime = System.currentTimeMillis() - startTime;
                String fileName = "screenshot_" + elapsedTime + ".png";
                // TODO output block distance and id to file. Implement python screenshot code, take screenshot based on socket communication.
                try {
                    sockClient.sendMessage("Block Recognition -" + label.getBlockStateString() + " " + label.getDistance() + " " + label.getDistance());
                    sockClient.sendCommand("Take Screenshot");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                long tickEndTime = System.nanoTime();
                long elapsedTickTime = (tickEndTime - tickStartTime) / 1_000_000;
                System.out.println("Elapsed time: " + elapsedTickTime + " ms");
            }
        });

        // Registering a shutdown event handler
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            // Client is stopping, close the socket connection here
            if (sockClient != null) {
                try {
                    sockClient.stopConnection();
                    System.out.println("Socket connection closed successfully.");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error closing socket connection.");
                }
            }
        });
    }

    public class BlockLabel {
        private final Integer blockStateId;
        private final double distance;
        private final String blockStateString;

        public BlockLabel(String blockStateString, Integer blockStateId, double distance) {
            this.blockStateString = blockStateString;
            this.blockStateId = blockStateId;
            this.distance = distance;
        }

        public Integer getBlockStateId() {
            return blockStateId;
        }

        public String getBlockStateString() {
            return blockStateString;
        }

        public double getDistance() {
            return distance;
        }
    }

    public BlockLabel getRecognitionLabel() {
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

        BlockLabel label = new BlockLabel("None found",-1, -1);
        // Check if a block was hit
        if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
            BlockPos blockPos = hitResult.getBlockPos();
            BlockState blockState = client.world.getBlockState(blockPos);

            // Calculate the distance from the player to the block
            double distance = eyePosition.distanceTo(new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));

            // Convert BlockState to its string representation
            String blockStateString = blockState.toString();

            // Get the ID from the map
            Integer blockStateId = blockstateMap.get(blockStateString);

            if (blockStateId != null) {
                // Here you can store or process the blockStateId and distance pair
                // For example, you might want to print it for now
                System.out.println("BlockState ID: " + blockStateId + ", Blockstate string: " + blockStateString + ", Distance: " + distance);
                label = new BlockLabel(blockStateString, blockStateId, distance);
            } else {
                // Handle the case where the block state is not found in the map
                System.out.println("BlockState not found in the map for: " + blockStateString);
            }
        }
        return label;
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
