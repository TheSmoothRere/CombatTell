package io.github.thesmoothrere.combattell.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

@Environment(EnvType.CLIENT)
public class TextParticle extends Particle {
    private final FormattedCharSequence formattedText;
    private final float textWidth;
    private final float initialScale;
    private final int color;

    public TextParticle(ClientLevel clientLevel, Vec3 pos, Vec3 velocity, String text, float initialScale, int color) {
        super(clientLevel, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);

        // Cache visual text structures immediately to preserve CPU cycles during rendering
        Font font = Minecraft.getInstance().font;
        this.formattedText = Component.literal(text).getVisualOrderText();
        this.textWidth = font.width(this.formattedText);

        this.initialScale = initialScale;
        this.color = color;
        this.gravity = 0.0F;
        this.lifetime = 30; // 1.5 seconds lifespan
    }

    public void extract(TextParticleRenderState state, Camera camera, float partialTickTime) {
        Vec3 cameraPos = camera.position();

        // Linearly interpolate positions between ticks for high refresh rate monitors
        float x = (float) (Mth.lerp(partialTickTime, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTickTime, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTickTime, this.zo, this.z) - cameraPos.z());

        // Animate alpha fading out over the particle's lifetime
        float currentAlpha = 1.0f - ((float) this.age / (float) this.lifetime);
        currentAlpha = Mth.clamp(currentAlpha, 0.0f, 1.0f);

        int packedAlpha = (int) (currentAlpha * 255.0f) << 24;
        int rgb = this.color & 0x00FFFFFF;
        int argbWithAlpha = packedAlpha | rgb;

        // Package the active snapshot into the state collector
        state.add(this.formattedText, this.textWidth, x, y, z, argbWithAlpha, this.initialScale);
    }

    @Override
    public void tick() {
        super.tick();
        // Float gently upwards and slow down horizontal speeds
        this.xd *= 0.92;
        this.yd += 0.015;
        this.zd *= 0.92;
    }

    @Override
    public @NonNull ParticleRenderType getGroup() {
        return TextParticleRenderType.TEXT;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }
}
