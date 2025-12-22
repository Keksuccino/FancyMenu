package de.keksuccino.fancymenu.util.rendering.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FireflyOverlay extends AbstractWidget implements NavigatableWidget {

    private static final int AREA_PER_GROUP = 180000;
    private static final int MIN_GROUPS = 2;
    private static final int MAX_GROUPS = 24;
    private static final int MIN_FIREFLIES = 4;
    private static final int MAX_FIREFLIES = 18;
    private static final float MIN_MULTIPLIER = 0.1F;
    private static final float MAX_MULTIPLIER = 4.0F;
    private static final float MIN_GROUP_RADIUS = 18.0F;
    private static final float MAX_GROUP_RADIUS = 58.0F;
    private static final float GROUP_DRIFT_MIN = 3.0F;
    private static final float GROUP_DRIFT_MAX = 10.0F;
    private static final float GROUP_DRIFT_RESPONSE = 0.35F;
    private static final float GROUP_FOLLOW_START_RADIUS = 70.0F;
    private static final float GROUP_FOLLOW_STOP_RADIUS = 110.0F;
    private static final float GROUP_FOLLOW_MIN_SPEED = 8.0F;
    private static final float GROUP_FOLLOW_MAX_SPEED = 65.0F;
    private static final float GROUP_FOLLOW_ACCEL = 1.6F;
    private static final float GROUP_FOLLOW_CHARGE_RATE = 0.35F;
    private static final float GROUP_FOLLOW_DECAY_RATE = 0.4F;

    private static final float FIREFLY_SPEED_MIN = 8.0F;
    private static final float FIREFLY_SPEED_MAX = 22.0F;
    private static final float FIREFLY_WANDER_ACCEL = 16.0F;
    private static final float FIREFLY_RETURN_FORCE = 26.0F;
    private static final float FIREFLY_LANDING_CHANCE = 0.025F;
    private static final float FIREFLY_LANDING_SPEED_MIN = 20.0F;
    private static final float FIREFLY_LANDING_SPEED_MAX = 45.0F;
    private static final float FIREFLY_TAKEOFF_SPEED_MIN = 18.0F;
    private static final float FIREFLY_TAKEOFF_SPEED_MAX = 40.0F;
    private static final float FIREFLY_MIN_LAND_TIME = 1.8F;
    private static final float FIREFLY_MAX_LAND_TIME = 4.6F;
    private static final float FIREFLY_LAND_COOLDOWN_MIN = 2.5F;
    private static final float FIREFLY_LAND_COOLDOWN_MAX = 7.0F;
    private static final float FIREFLY_FLICKER_SPEED_MIN = 0.25F;
    private static final float FIREFLY_FLICKER_SPEED_MAX = 0.7F;
    private static final float MAX_DELTA_SECONDS = 0.1F;

    private final RandomSource random = RandomSource.create();
    private final List<FireflyGroup> groups = new ArrayList<>();
    private final List<LandingArea> collisionAreas = new ArrayList<>();
    private LandingArea bottomArea;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private long lastUpdateMs = -1L;
    private float groupAmount = 1.0F;
    private float groupDensity = 1.0F;
    private float groupSize = 1.0F;
    private int fireflyColor = 0xFFFFE08A;
    private float fireflyAlphaScale = 1.0F;
    private boolean followMouse = true;
    private boolean landingEnabled = true;

    public FireflyOverlay(int width, int height) {
        super(0, 0, width, height, Component.empty());
    }

    public void setGroupAmount(float groupAmount) {
        this.groupAmount = Mth.clamp(groupAmount, MIN_MULTIPLIER, MAX_MULTIPLIER);
    }

    public float getGroupAmount() {
        return this.groupAmount;
    }

    public void setGroupDensity(float groupDensity) {
        this.groupDensity = Mth.clamp(groupDensity, MIN_MULTIPLIER, MAX_MULTIPLIER);
    }

    public float getGroupDensity() {
        return this.groupDensity;
    }

    public void setGroupSize(float groupSize) {
        float clamped = Mth.clamp(groupSize, 0.5F, 2.0F);
        if (Math.abs(clamped - this.groupSize) < 0.001F) {
            return;
        }
        this.groupSize = clamped;
        updateGroupSizeScale();
    }

    public float getGroupSize() {
        return this.groupSize;
    }

    public void setColor(int color) {
        this.fireflyColor = color;
        this.fireflyAlphaScale = resolveAlphaScale(color);
    }

    public int getColor() {
        return this.fireflyColor;
    }

    public void setFollowMouseEnabled(boolean enabled) {
        this.followMouse = enabled;
    }

    public void setLandingEnabled(boolean enabled) {
        this.landingEnabled = enabled;
    }

    public void addCollisionArea(int x, int y, int width, int height) {
        if (width <= 0 || height < 0) {
            return;
        }
        this.collisionAreas.add(new LandingArea(x, y, width, height));
    }

    public void clearCollisionAreas() {
        this.collisionAreas.clear();
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int overlayX = this.getX();
        int overlayY = this.getY();
        int overlayWidth = this.getWidth();
        int overlayHeight = this.getHeight();

        if (overlayWidth <= 0 || overlayHeight <= 0) {
            return;
        }

        boolean sizeChanged = ensureGroups(overlayWidth, overlayHeight);
        ensureLandingAreas(overlayWidth, overlayHeight, sizeChanged);
        float deltaSeconds = computeDeltaSeconds(sizeChanged);
        if (deltaSeconds > 0.0F) {
            updateGroups(overlayWidth, overlayHeight, mouseX, mouseY, deltaSeconds);
        }

        renderFireflies(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
    }

    private boolean ensureGroups(int width, int height) {
        float effectiveGroupAmount = Mth.clamp(this.groupAmount, MIN_MULTIPLIER, MAX_MULTIPLIER);
        int baseCount = Mth.clamp((width * height) / AREA_PER_GROUP, MIN_GROUPS, MAX_GROUPS);
        int maxGroups = Mth.floor(MAX_GROUPS * MAX_MULTIPLIER);
        int desiredGroups = Mth.clamp(Mth.floor(baseCount * effectiveGroupAmount), 0, maxGroups);
        if (effectiveGroupAmount > 0.01F && desiredGroups == 0) {
            desiredGroups = 1;
        }
        boolean sizeChanged = this.lastWidth != width || this.lastHeight != height;

        if (sizeChanged) {
            this.groups.clear();
            if (desiredGroups > 0) {
                for (int i = 0; i < desiredGroups; i++) {
                    this.groups.add(createGroup(width, height, this.groupDensity));
                }
            }
            this.lastWidth = width;
            this.lastHeight = height;
            this.lastUpdateMs = System.currentTimeMillis();
            return true;
        }

        while (this.groups.size() < desiredGroups) {
            this.groups.add(createGroup(width, height, this.groupDensity));
        }
        while (this.groups.size() > desiredGroups) {
            this.groups.remove(this.groups.size() - 1);
        }

        if (!this.groups.isEmpty()) {
            int desiredPerGroup = getDesiredFirefliesPerGroup(this.groupDensity);
            for (FireflyGroup group : this.groups) {
                ensureFireflies(group, desiredPerGroup);
            }
        }
        return false;
    }

    private int getDesiredFirefliesPerGroup(float densityMultiplier) {
        int basePerGroup = Mth.floor((MIN_FIREFLIES + MAX_FIREFLIES) * 0.5F);
        int desired = Mth.floor(basePerGroup * Mth.clamp(densityMultiplier, MIN_MULTIPLIER, MAX_MULTIPLIER));
        int maxPerGroup = Mth.floor(MAX_FIREFLIES * MAX_MULTIPLIER);
        return Mth.clamp(desired, 1, maxPerGroup);
    }

    private void ensureFireflies(FireflyGroup group, int desiredCount) {
        if (desiredCount <= 0) {
            group.fireflies.clear();
            return;
        }
        while (group.fireflies.size() < desiredCount) {
            Firefly firefly = createFirefly(group.radius);
            firefly.worldX = group.x + firefly.offsetX;
            firefly.worldY = group.y + firefly.offsetY;
            group.fireflies.add(firefly);
        }
        while (group.fireflies.size() > desiredCount) {
            group.fireflies.remove(group.fireflies.size() - 1);
        }
    }

    private void ensureLandingAreas(int width, int height, boolean sizeChanged) {
        int surfaceY = Math.max(0, height - 1);
        if (this.bottomArea == null || sizeChanged) {
            this.bottomArea = new LandingArea(0, surfaceY, width, 1);
        } else {
            this.bottomArea.setBounds(0, surfaceY, width, 1);
        }
    }

    private float computeDeltaSeconds(boolean sizeChanged) {
        long now = System.currentTimeMillis();
        if (this.lastUpdateMs < 0L || sizeChanged) {
            this.lastUpdateMs = now;
            return 0.0F;
        }
        float deltaSeconds = (now - this.lastUpdateMs) / 1000.0F;
        if (deltaSeconds <= 0.0F) {
            return 0.0F;
        }
        if (deltaSeconds > MAX_DELTA_SECONDS) {
            deltaSeconds = MAX_DELTA_SECONDS;
        }
        this.lastUpdateMs = now;
        return deltaSeconds;
    }

    private void updateGroups(int width, int height, int mouseX, int mouseY, float deltaSeconds) {
        long now = System.currentTimeMillis();
        for (FireflyGroup group : this.groups) {
            updateGroupMovement(group, width, height, mouseX, mouseY, deltaSeconds, now);
            updateFireflies(group, width, height, deltaSeconds);
        }
    }

    private void updateGroupMovement(FireflyGroup group, int width, int height, int mouseX, int mouseY, float deltaSeconds, long now) {
        boolean following = this.followMouse && updateFollowState(group, mouseX, mouseY);
        if (following) {
            float dx = mouseX - group.x;
            float dy = mouseY - group.y;
            float dist = Mth.sqrt(dx * dx + dy * dy);
            float desiredSpeed = Mth.clamp(dist * 0.25F, GROUP_FOLLOW_MIN_SPEED, GROUP_FOLLOW_MAX_SPEED);
            group.followCharge = Mth.clamp(group.followCharge + deltaSeconds * GROUP_FOLLOW_CHARGE_RATE, 0.0F, 1.0F);
            float chargeScale = 0.35F + 0.65F * group.followCharge;
            desiredSpeed *= chargeScale;
            float desiredVx = (dist > 0.001F) ? (dx / dist) * desiredSpeed : 0.0F;
            float desiredVy = (dist > 0.001F) ? (dy / dist) * desiredSpeed : 0.0F;
            group.vx += (desiredVx - group.vx) * GROUP_FOLLOW_ACCEL * deltaSeconds;
            group.vy += (desiredVy - group.vy) * GROUP_FOLLOW_ACCEL * deltaSeconds;
        } else {
            group.followCharge = Mth.clamp(group.followCharge - deltaSeconds * GROUP_FOLLOW_DECAY_RATE, 0.0F, 1.0F);
            if (now >= group.nextDriftChangeMs) {
                float speed = nextRange(GROUP_DRIFT_MIN, GROUP_DRIFT_MAX);
                float angle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
                group.targetVx = Mth.cos(angle) * speed;
                group.targetVy = Mth.sin(angle) * speed;
                group.nextDriftChangeMs = now + 3500L + this.random.nextInt(5000);
            }
            group.vx += (group.targetVx - group.vx) * GROUP_DRIFT_RESPONSE * deltaSeconds;
            group.vy += (group.targetVy - group.vy) * GROUP_DRIFT_RESPONSE * deltaSeconds;
        }

        group.x += group.vx * deltaSeconds;
        group.y += group.vy * deltaSeconds;

        float padding = group.radius + 6.0F;
        if (width <= padding * 2.0F) {
            group.x = width * 0.5F;
        } else if (group.x < padding) {
            group.x = padding;
            group.vx = Math.abs(group.vx);
        } else if (group.x > width - padding) {
            group.x = width - padding;
            group.vx = -Math.abs(group.vx);
        }

        if (height <= padding * 2.0F) {
            group.y = height * 0.5F;
        } else if (group.y < padding) {
            group.y = padding;
            group.vy = Math.abs(group.vy);
        } else if (group.y > height - padding) {
            group.y = height - padding;
            group.vy = -Math.abs(group.vy);
        }
    }

    private boolean updateFollowState(FireflyGroup group, int mouseX, int mouseY) {
        float dx = mouseX - group.x;
        float dy = mouseY - group.y;
        float startRadius = GROUP_FOLLOW_START_RADIUS + group.radius * 0.5F;
        float stopRadius = GROUP_FOLLOW_STOP_RADIUS + group.radius * 0.65F;
        float distSquared = dx * dx + dy * dy;
        if (group.following) {
            group.following = distSquared <= stopRadius * stopRadius;
        } else {
            group.following = distSquared <= startRadius * startRadius;
        }
        return group.following;
    }

    private void updateFireflies(FireflyGroup group, int width, int height, float deltaSeconds) {
        if (group.fireflies.isEmpty()) {
            return;
        }
        for (Firefly firefly : group.fireflies) {
            firefly.flickerTime += deltaSeconds * firefly.flickerSpeed;
            firefly.landingCooldown = Math.max(0.0F, firefly.landingCooldown - deltaSeconds);
            if (!this.landingEnabled && (firefly.state == FireflyState.LANDING || firefly.state == FireflyState.LANDED)) {
                beginTakeoff(group, firefly);
            }
            switch (firefly.state) {
                case LANDING -> updateLandingState(group, firefly, deltaSeconds);
                case LANDED -> updateLandedState(group, firefly, deltaSeconds);
                case TAKEOFF -> updateTakeoffState(group, firefly, deltaSeconds);
                default -> updateFlyingState(group, firefly, deltaSeconds, width, height);
            }
        }
    }

    private void updateFlyingState(FireflyGroup group, Firefly firefly, float deltaSeconds, int width, int height) {
        float jitterX = (this.random.nextFloat() * 2.0F - 1.0F) * FIREFLY_WANDER_ACCEL;
        float jitterY = (this.random.nextFloat() * 2.0F - 1.0F) * FIREFLY_WANDER_ACCEL;
        firefly.vx += jitterX * deltaSeconds;
        firefly.vy += jitterY * deltaSeconds;
        clampFireflySpeed(firefly);

        firefly.offsetX += firefly.vx * deltaSeconds;
        firefly.offsetY += firefly.vy * deltaSeconds;

        float distance = Mth.sqrt(firefly.offsetX * firefly.offsetX + firefly.offsetY * firefly.offsetY);
        if (distance > group.radius) {
            float pull = (distance - group.radius) / group.radius;
            float inv = 1.0F / Math.max(distance, 0.001F);
            firefly.vx -= firefly.offsetX * inv * FIREFLY_RETURN_FORCE * pull * deltaSeconds;
            firefly.vy -= firefly.offsetY * inv * FIREFLY_RETURN_FORCE * pull * deltaSeconds;
            firefly.offsetX = firefly.offsetX * (group.radius / distance);
            firefly.offsetY = firefly.offsetY * (group.radius / distance);
        }

        firefly.worldX = group.x + firefly.offsetX;
        firefly.worldY = group.y + firefly.offsetY;

        if (this.landingEnabled && firefly.landingCooldown <= 0.0F && this.random.nextFloat() < FIREFLY_LANDING_CHANCE * deltaSeconds) {
            tryStartLanding(firefly, group, width, height);
        }
    }

    private void updateLandingState(FireflyGroup group, Firefly firefly, float deltaSeconds) {
        float dx = firefly.targetX - firefly.worldX;
        float dy = firefly.targetY - firefly.worldY;
        float dist = Mth.sqrt(dx * dx + dy * dy);
        float step = firefly.landingSpeed * deltaSeconds;
        if (dist <= step || dist < 0.5F) {
            firefly.worldX = firefly.targetX;
            firefly.worldY = firefly.targetY;
            firefly.state = FireflyState.LANDED;
            firefly.stateTime = 0.0F;
            firefly.stateDuration = nextRange(FIREFLY_MIN_LAND_TIME, FIREFLY_MAX_LAND_TIME);
            firefly.landingCooldown = nextRange(FIREFLY_LAND_COOLDOWN_MIN, FIREFLY_LAND_COOLDOWN_MAX);
            return;
        }
        float inv = 1.0F / Math.max(dist, 0.001F);
        firefly.worldX += dx * inv * step;
        firefly.worldY += dy * inv * step;
    }

    private void updateLandedState(FireflyGroup group, Firefly firefly, float deltaSeconds) {
        firefly.stateTime += deltaSeconds;
        if (firefly.stateTime >= firefly.stateDuration) {
            beginTakeoff(group, firefly);
        }
    }

    private void beginTakeoff(FireflyGroup group, Firefly firefly) {
        firefly.state = FireflyState.TAKEOFF;
        firefly.takeoffSpeed = nextRange(FIREFLY_TAKEOFF_SPEED_MIN, FIREFLY_TAKEOFF_SPEED_MAX);
        float radius = group.radius * 0.85F;
        float angle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        float distance = this.random.nextFloat() * radius;
        firefly.offsetX = Mth.cos(angle) * distance;
        firefly.offsetY = Mth.sin(angle) * distance;
        firefly.takeoffOffsetX = firefly.offsetX;
        firefly.takeoffOffsetY = firefly.offsetY;
    }

    private void updateTakeoffState(FireflyGroup group, Firefly firefly, float deltaSeconds) {
        float targetX = group.x + firefly.takeoffOffsetX;
        float targetY = group.y + firefly.takeoffOffsetY;
        float dx = targetX - firefly.worldX;
        float dy = targetY - firefly.worldY;
        float dist = Mth.sqrt(dx * dx + dy * dy);
        float step = firefly.takeoffSpeed * deltaSeconds;
        if (dist <= step || dist < 0.6F) {
            firefly.state = FireflyState.FLYING;
            firefly.worldX = targetX;
            firefly.worldY = targetY;
            return;
        }
        float inv = 1.0F / Math.max(dist, 0.001F);
        firefly.worldX += dx * inv * step;
        firefly.worldY += dy * inv * step;
    }

    private void tryStartLanding(Firefly firefly, FireflyGroup group, int width, int height) {
        LandingArea area = pickLandingArea(width, height);
        if (area == null || !area.isValid()) {
            return;
        }
        float targetX;
        float targetY;
        if (area == this.bottomArea) {
            targetX = area.x + this.random.nextFloat() * area.width;
            targetY = area.y;
        } else {
            int areaWidth = Math.max(1, area.width);
            int areaHeight = Math.max(1, area.height);
            switch (this.random.nextInt(4)) {
                case 0 -> {
                    targetX = area.x + this.random.nextFloat() * areaWidth;
                    targetY = area.y;
                }
                case 1 -> {
                    targetX = area.x + this.random.nextFloat() * areaWidth;
                    targetY = area.y + areaHeight - 1.0F;
                }
                case 2 -> {
                    targetX = area.x;
                    targetY = area.y + this.random.nextFloat() * areaHeight;
                }
                default -> {
                    targetX = area.x + areaWidth - 1.0F;
                    targetY = area.y + this.random.nextFloat() * areaHeight;
                }
            }
        }
        firefly.state = FireflyState.LANDING;
        firefly.targetX = Mth.clamp(targetX, 0.0F, width - 1.0F);
        firefly.targetY = Mth.clamp(targetY, 0.0F, height - 1.0F);
        firefly.worldX = group.x + firefly.offsetX;
        firefly.worldY = group.y + firefly.offsetY;
        firefly.landingSpeed = nextRange(FIREFLY_LANDING_SPEED_MIN, FIREFLY_LANDING_SPEED_MAX);
    }

    private void clampFireflySpeed(Firefly firefly) {
        float speed = Mth.sqrt(firefly.vx * firefly.vx + firefly.vy * firefly.vy);
        if (speed < 0.001F) {
            float angle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            firefly.vx = Mth.cos(angle) * FIREFLY_SPEED_MIN;
            firefly.vy = Mth.sin(angle) * FIREFLY_SPEED_MIN;
            return;
        }
        if (speed > FIREFLY_SPEED_MAX) {
            float scale = FIREFLY_SPEED_MAX / speed;
            firefly.vx *= scale;
            firefly.vy *= scale;
        } else if (speed < FIREFLY_SPEED_MIN) {
            float scale = FIREFLY_SPEED_MIN / speed;
            firefly.vx *= scale;
            firefly.vy *= scale;
        }
    }

    private void renderFireflies(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        if (this.groups.isEmpty()) {
            return;
        }
        int baseAlpha = resolveBaseAlpha(this.fireflyAlphaScale);
        int rgb = this.fireflyColor & 0x00FFFFFF;
        for (FireflyGroup group : this.groups) {
            for (Firefly firefly : group.fireflies) {
                float flicker = 0.7F + 0.3F * Mth.sin(firefly.flickerTime);
                float brightness = firefly.brightness * flicker;
                int alpha = Mth.clamp(Mth.floor(baseAlpha * brightness), 0, 255);
                if (alpha <= 0) {
                    continue;
                }
                float renderX = firefly.worldX;
                float renderY = firefly.worldY;
                int x = overlayX + Mth.floor(renderX);
                int y = overlayY + Mth.floor(renderY);
                int size = firefly.size;
                if (x + size <= overlayX || x >= overlayX + overlayWidth) {
                    continue;
                }
                if (y + size <= overlayY || y >= overlayY + overlayHeight) {
                    continue;
                }
                int color = (alpha << 24) | rgb;
                graphics.fill(x, y, x + size, y + size, color);
            }
        }
    }

    private int resolveBaseAlpha(float alphaScale) {
        int baseAlpha = 230;
        return Mth.clamp(Math.round(baseAlpha * alphaScale), 0, 255);
    }

    private FireflyGroup createGroup(int width, int height, float densityMultiplier) {
        FireflyGroup group = new FireflyGroup();
        group.baseRadius = nextRange(MIN_GROUP_RADIUS, MAX_GROUP_RADIUS);
        group.radius = group.baseRadius * this.groupSize;
        group.x = this.random.nextFloat() * width;
        group.y = this.random.nextFloat() * height;
        group.nextDriftChangeMs = 0L;
        group.followCharge = 0.0F;
        int desiredPerGroup = getDesiredFirefliesPerGroup(densityMultiplier);
        for (int i = 0; i < desiredPerGroup; i++) {
            Firefly firefly = createFirefly(group.radius);
            firefly.worldX = group.x + firefly.offsetX;
            firefly.worldY = group.y + firefly.offsetY;
            group.fireflies.add(firefly);
        }
        return group;
    }

    private Firefly createFirefly(float radius) {
        Firefly firefly = new Firefly();
        float angle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        float distance = this.random.nextFloat() * radius;
        firefly.offsetX = Mth.cos(angle) * distance;
        firefly.offsetY = Mth.sin(angle) * distance;
        float velocityAngle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        float speed = nextRange(FIREFLY_SPEED_MIN, FIREFLY_SPEED_MAX);
        firefly.vx = Mth.cos(velocityAngle) * speed;
        firefly.vy = Mth.sin(velocityAngle) * speed;
        firefly.flickerSpeed = nextRange(FIREFLY_FLICKER_SPEED_MIN, FIREFLY_FLICKER_SPEED_MAX);
        firefly.flickerTime = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        firefly.brightness = nextRange(0.6F, 1.0F);
        firefly.size = (this.random.nextFloat() < 0.18F) ? 2 : 1;
        firefly.state = FireflyState.FLYING;
        firefly.landingCooldown = nextRange(0.0F, FIREFLY_LAND_COOLDOWN_MAX);
        return firefly;
    }

    private void updateGroupSizeScale() {
        for (FireflyGroup group : this.groups) {
            group.radius = group.baseRadius * this.groupSize;
            if (group.radius <= 0.01F) {
                continue;
            }
            for (Firefly firefly : group.fireflies) {
                float distance = Mth.sqrt(firefly.offsetX * firefly.offsetX + firefly.offsetY * firefly.offsetY);
                if (distance > group.radius) {
                    float scale = group.radius / Math.max(distance, 0.001F);
                    firefly.offsetX *= scale;
                    firefly.offsetY *= scale;
                    if (firefly.state == FireflyState.TAKEOFF) {
                        firefly.takeoffOffsetX = firefly.offsetX;
                        firefly.takeoffOffsetY = firefly.offsetY;
                    }
                }
            }
        }
    }

    private LandingArea pickLandingArea(int width, int height) {
        int totalWidth = width;
        for (LandingArea area : this.collisionAreas) {
            totalWidth += Math.max(0, area.width);
        }
        if (totalWidth <= 0) {
            return null;
        }
        int roll = this.random.nextInt(totalWidth);
        if (roll < width) {
            return this.bottomArea;
        }
        roll -= width;
        for (LandingArea area : this.collisionAreas) {
            int areaWidth = Math.max(0, area.width);
            if (roll < areaWidth) {
                return area;
            }
            roll -= areaWidth;
        }
        return this.bottomArea;
    }

    private float nextRange(float min, float max) {
        return min + this.random.nextFloat() * (max - min);
    }

    private float resolveAlphaScale(int color) {
        int alpha = FastColor.ARGB32.alpha(color);
        if (alpha <= 0) {
            alpha = 255;
        }
        return Mth.clamp(alpha / 255.0F, 0.0F, 1.0F);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
    }

    private enum FireflyState {
        FLYING,
        LANDING,
        LANDED,
        TAKEOFF
    }

    private static final class LandingArea {
        private int x;
        private int y;
        private int width;
        private int height;

        private LandingArea(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean isValid() {
            return this.width > 0;
        }
    }

    private static final class FireflyGroup {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float targetVx;
        private float targetVy;
        private long nextDriftChangeMs;
        private float baseRadius;
        private float radius;
        private float followCharge;
        private boolean following;
        private final List<Firefly> fireflies = new ArrayList<>();
    }

    private static final class Firefly {
        private float offsetX;
        private float offsetY;
        private float vx;
        private float vy;
        private float worldX;
        private float worldY;
        private float flickerTime;
        private float flickerSpeed;
        private float brightness;
        private int size;
        private FireflyState state;
        private float targetX;
        private float targetY;
        private float stateTime;
        private float stateDuration;
        private float landingSpeed;
        private float takeoffSpeed;
        private float landingCooldown;
        private float takeoffOffsetX;
        private float takeoffOffsetY;
    }

}
