package io.github.thesmoothrere.combattell.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public class TextParticleRenderState implements ParticleGroupRenderState {

    private static final int INITIAL_CAPACITY = 256;

    // Mojang Pattern: Flat parallel primitive arrays + an object reference array
    private FormattedCharSequence[] textValues = new FormattedCharSequence[INITIAL_CAPACITY];
    private float[] floatValues = new float[INITIAL_CAPACITY * 5]; // 5 floats per particle (width, x, y, z, initialScale)
    private int[] colorValues = new int[INITIAL_CAPACITY];         // 1 int per particle

    private int capacity = INITIAL_CAPACITY;
    private int activeCount = 0;

    // Allocate once for the class lifespan to completely protect submit() passes
    private final PoseStack poseStack = new PoseStack();

    public void add(FormattedCharSequence text, float width, float x, float y, float z, int color, float initialScale) {
        if (this.activeCount >= this.capacity) {
            this.grow();
        }

        // Store the text object reference
        this.textValues[this.activeCount] = text;

        // Pack spatial & scaling properties contiguously into the float array
        int floatIdx = this.activeCount * 5;
        this.floatValues[floatIdx++] = width;
        this.floatValues[floatIdx++] = x;
        this.floatValues[floatIdx++] = y;
        this.floatValues[floatIdx++] = z;
        this.floatValues[floatIdx] = initialScale;

        // Pack color data
        this.colorValues[this.activeCount] = color;

        this.activeCount++;
    }

    @Override
    public void clear() {
        if (this.activeCount > 0) {
            Arrays.fill(this.textValues, 0, this.activeCount, null);
            this.activeCount = 0; // Pointer safely dropped
        }
    }

    @Override
    @NullMarked
    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (this.activeCount == 0) return;

        for (int i = 0; i < this.activeCount; i++) {
            FormattedCharSequence text = this.textValues[i];

            int floatIdx = i * 5;
            float width = this.floatValues[floatIdx++];
            float x = this.floatValues[floatIdx++];
            float y = this.floatValues[floatIdx++];
            float z = this.floatValues[floatIdx++];
            float initialScale = this.floatValues[floatIdx];

            int color = this.colorValues[i];

            try {
                poseStack.pushPose();

                // 1. Move to the particle position relative to the camera
                poseStack.translate(x, y, z);

                // 2. Compute dynamic distance initialScale factor using flat variable data
                float dynamicScale = scaleBasedDistanceFactor(x, y, z, initialScale);

                // 3. Precision camera tracking billboarding
                poseStack.mulPose(Axis.YP.rotationDegrees(-camera.yRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot));
                poseStack.scale(-dynamicScale, -dynamicScale, dynamicScale);

                float xOffset = -width / 2.0F;

                // 4. Send to engine draw passes
                submitNodeCollector.submitText(
                        poseStack,
                        xOffset, 0.0F,
                        text,
                        false,
                        Font.DisplayMode.NORMAL,
                        LightCoordsUtil.FULL_BRIGHT,
                        color,
                        0,
                        0
                );
            } finally {
                poseStack.popPose();
            }
        }
    }

    private void grow() {
        this.capacity *= 2;
        this.textValues = Arrays.copyOf(this.textValues, this.capacity);
        this.floatValues = Arrays.copyOf(this.floatValues, this.capacity * 5);
        this.colorValues = Arrays.copyOf(this.colorValues, this.capacity);
    }

    private static float scaleBasedDistanceFactor(float x, float y, float z, float initialScale) {
        double currentDistance = Math.sqrt(x * x + y * y + z * z);
        float distanceMultiplier = (float) currentDistance * 0.15f;
        float clampedDistanceScale = Mth.clamp(distanceMultiplier, 1.0F, 5.0F);
        return initialScale * clampedDistanceScale;
    }
}
