package io.github.thesmoothrere.combattell.client.particle;

public final class ParticleConfig {
    private String health;
    private float initialScale;
    private int color;
    private int lifetimeTicks;
    private double riseSpeed;

    // A single helper method to update all values at once
    public void set(String health, float initialScale, int color, int lifetimeTicks, double riseSpeed) {
        this.health = health;
        this.initialScale = initialScale;
        this.color = color;
        this.lifetimeTicks = lifetimeTicks;
        this.riseSpeed = riseSpeed;
    }

    public String health() {
        return health;
    }

    public float initialScale() {
        return initialScale;
    }

    public int color() {
        return color;
    }

    public int lifetimeTicks() {
        return lifetimeTicks;
    }

    public double riseSpeed() {
        return riseSpeed;
    }
}
