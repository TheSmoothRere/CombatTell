package io.github.thesmoothrere.combattell.client;

import io.github.thesmoothrere.combattell.Constants;
import io.github.thesmoothrere.combattell.client.particle.TextParticleGroup;
import io.github.thesmoothrere.combattell.client.particle.TextParticleRenderType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry;

@Environment(EnvType.CLIENT)
public class CombatTellClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Constants.LOGGER.info(Constants.MOD_NAME + " client initialized!");
        ParticleGroupRegistry.register(TextParticleRenderType.TEXT, TextParticleGroup::new);
    }
}
