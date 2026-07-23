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

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class TextParticleRenderState implements ParticleGroupRenderState {
    public record PreparedText(FormattedCharSequence text, float width, float x, float y, float z, int color, float scale) {}
    private final List<PreparedText> activeTexts = new ArrayList<>();

    public void add(FormattedCharSequence text, float width, float x, float y, float z, int color, float scale) {
        this.activeTexts.add(new PreparedText(text, width, x, y, z, color, scale));
    }

    @Override
    public void clear() {
        this.activeTexts.clear();
    }

    @Override
    @NullMarked
    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (this.activeTexts.isEmpty()) return;

        // Re-use a single PoseStack instance for the entire batch to optimize allocations
        PoseStack poseStack = new PoseStack();

        for (PreparedText data : this.activeTexts) {
            poseStack.pushPose();

            // 1. Move to the particle's spatial position in the world relative to camera
            poseStack.translate(data.x, data.y, data.z);

            // ----- RPG DYNAMIC DISTANCE SCALING -----
            // Since data.x, data.y, data.z are relative to the camera,
            // the distance is simply the length of this position vector.
            float dynamicScale = scaleBasedDistanceFactor(data);
            // ----------------------------------------

            // 2. Dynamic Billboarding (Face player camera angles precisely)
            poseStack.mulPose(Axis.YP.rotationDegrees(-camera.yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot));
            poseStack.scale(-dynamicScale, -dynamicScale, dynamicScale);

            // 4. Center the numbers horizontally using cached widths
            float xOffset = -data.width / 2.0F;

            // 5. Submit text node directly to the transparent render pass graph
            submitNodeCollector.submitText(
                    poseStack,
                    xOffset, 0.0F,
                    data.text,
                    false,
                    Font.DisplayMode.NORMAL,
                    LightCoordsUtil.FULL_BRIGHT, // Full bright lightmaps so text glows in shadows
                    data.color,
                    0, // Background color (Transparent background rectangle)
                    0
            );

            poseStack.popPose();
        }
    }

    private static float scaleBasedDistanceFactor(PreparedText data) {
        double currentDistance = Math.sqrt(data.x * data.x + data.y * data.y + data.z * data.z);

        // Linear scale scaling: Grow 1:1 with distance so it stays perfectly uniform on screen
        float distanceMultiplier = (float) currentDistance * 0.15f;

        // Clamp the scale multiplier so it doesn't get unreadably tiny up close
        // or cover the screen if you zoom/teleport far away.
        float clampedDistanceScale = Mth.clamp(distanceMultiplier, 1.0F, 5.0F);

        // Multiply the original base scale (stored inside data when spawned) by our dynamic factor
        return data.scale * clampedDistanceScale;
    }
}
