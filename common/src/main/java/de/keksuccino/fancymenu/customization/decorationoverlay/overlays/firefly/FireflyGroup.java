package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPointer;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * A group of fireflies that drifts slowly over the screen and follows the pointer when it comes close.
 */
final class FireflyGroup {

    float x;
    float y;
    float velocityX;
    float velocityY;
    float driftX;
    float driftY;
    float nextDriftChange;
    /** Radius at a Group Size of 1 */
    final float baseRadius;
    float radius;
    /** How eager the group follows the pointer, from 0 to 1 */
    float charge;
    boolean following;
    final List<Firefly> members = new ArrayList<>();

    FireflyGroup(float x, float y, float baseRadius) {
        this.x = x;
        this.y = y;
        this.baseRadius = baseRadius;
        this.radius = baseRadius;
    }

    /**
     * Drifts the group, or moves it to the pointer while it follows, and keeps it on the screen.
     */
    void move(float deltaSeconds, float time, @NotNull PixelPointer pointer, boolean followPointer, int screenWidth, int screenHeight, @NotNull PixelRandom random) {
        float dx = pointer.x() - this.x;
        float dy = pointer.y() - this.y;
        float distanceSquared = dx * dx + dy * dy;
        float startRadius = 70.0F + this.radius * 0.5F;
        float stopRadius = 110.0F + this.radius * 0.65F;
        this.following = followPointer && pointer.isInside() && (this.following ? (distanceSquared <= stopRadius * stopRadius) : (distanceSquared <= startRadius * startRadius));
        if (this.following) {
            float distance = (float)Math.sqrt(distanceSquared);
            this.charge = PixelMath.clamp(this.charge + deltaSeconds * 0.35F, 0.0F, 1.0F);
            float speed = PixelMath.clamp(distance * 0.25F, 8.0F, 65.0F) * (0.35F + 0.65F * this.charge);
            float wantX = (distance > 0.001F) ? dx / distance * speed : 0.0F;
            float wantY = (distance > 0.001F) ? dy / distance * speed : 0.0F;
            this.velocityX += (wantX - this.velocityX) * 1.6F * deltaSeconds;
            this.velocityY += (wantY - this.velocityY) * 1.6F * deltaSeconds;
        } else {
            this.charge = PixelMath.clamp(this.charge - deltaSeconds * 0.4F, 0.0F, 1.0F);
            if (time >= this.nextDriftChange) {
                float speed = random.range(3.0F, 10.0F);
                float angle = random.range(0.0F, PixelMath.TAU);
                this.driftX = (float)Math.cos(angle) * speed;
                this.driftY = (float)Math.sin(angle) * speed;
                this.nextDriftChange = time + 3.5F + random.next() * 5.0F;
            }
            this.velocityX += (this.driftX - this.velocityX) * 0.35F * deltaSeconds;
            this.velocityY += (this.driftY - this.velocityY) * 0.35F * deltaSeconds;
        }
        this.x += this.velocityX * deltaSeconds;
        this.y += this.velocityY * deltaSeconds;
        float pad = this.radius + 6.0F;
        float maxX = Math.max(pad, screenWidth - pad);
        float maxY = Math.max(pad, screenHeight - pad);
        if (this.x < pad) {
            this.x = pad;
            this.velocityX = Math.abs(this.velocityX);
        } else if (this.x > maxX) {
            this.x = maxX;
            this.velocityX = -Math.abs(this.velocityX);
        }
        if (this.y < pad) {
            this.y = pad;
            this.velocityY = Math.abs(this.velocityY);
        } else if (this.y > maxY) {
            this.y = maxY;
            this.velocityY = -Math.abs(this.velocityY);
        }
    }

    /**
     * Adds a firefly at a random spot inside of the group.
     */
    void addMember(@NotNull PixelRandom random) {
        float angle = random.range(0.0F, PixelMath.TAU);
        float distance = random.next() * this.radius;
        Firefly firefly = new Firefly();
        firefly.homeX = (float)Math.cos(angle) * distance;
        firefly.homeY = (float)Math.sin(angle) * distance;
        firefly.x = this.x + firefly.homeX;
        firefly.y = this.y + firefly.homeY;
        firefly.big = random.chance(0.18F);
        firefly.heading = random.range(0.0F, PixelMath.TAU);
        firefly.speed = random.range(7.0F, 14.0F);
        firefly.wobbleFrequency = random.range(0.6F, 1.4F);
        firefly.wobblePhase = random.range(0.0F, PixelMath.TAU);
        firefly.nextFlash = random.range(0.0F, 5.0F);
        firefly.doubleFlash = random.chance(0.3F);
        firefly.landingCooldown = random.range(1.0F, 8.0F);
        this.members.add(firefly);
    }

}
