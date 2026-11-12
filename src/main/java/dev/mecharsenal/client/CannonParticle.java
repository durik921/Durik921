package dev.mecharsenal.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class CannonParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final Style style;
    private final float initialSize;

    private CannonParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                           SpriteSet sprites, Style style) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.style = style;
        this.initialSize = style.size;
        this.quadSize = initialSize;
        this.lifetime = style.lifetime + random.nextInt(style.lifetime / 3 + 1);
        this.gravity = style.gravity;
        this.friction = style.friction;
        this.hasPhysics = style == Style.SPARK;
        this.rCol = style.red;
        this.gCol = style.green;
        this.bCol = style.blue;
        this.setAlpha(0.94F);
        this.roll = random.nextFloat() * (float) (Math.PI * 2.0);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive()) {
            setSpriteFromAge(sprites);
            float ageRatio = age / (float) lifetime;
            setAlpha((1.0F - ageRatio) * 0.94F);
            quadSize = initialSize * switch (style) {
                case MUZZLE -> 0.8F + ageRatio * 0.35F;
                case IMPACT -> 0.55F + ageRatio * 1.2F;
                case SPARK -> 1.0F - ageRatio * 0.65F;
                case TRACER -> 1.0F - ageRatio * 0.8F;
            };
            if (style == Style.SPARK) {
                roll += 0.22F;
            }
        }
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public enum Style {
        MUZZLE(4, 0.70F, 0.0F, 0.84F, 1.0F, 0.72F, 0.28F),
        SPARK(9, 0.12F, 0.18F, 0.82F, 1.0F, 0.66F, 0.22F),
        IMPACT(10, 0.95F, 0.0F, 0.88F, 1.0F, 0.56F, 0.22F),
        TRACER(4, 0.12F, 0.0F, 0.82F, 0.52F, 1.0F, 0.78F);

        private final int lifetime;
        private final float size;
        private final float gravity;
        private final float friction;
        private final float red;
        private final float green;
        private final float blue;

        Style(int lifetime, float size, float gravity, float friction, float red, float green, float blue) {
            this.lifetime = lifetime;
            this.size = size;
            this.gravity = gravity;
            this.friction = friction;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Style style;

        public Provider(SpriteSet sprites, Style style) {
            this.sprites = sprites;
            this.style = style;
        }

        @Override
        public CannonParticle createParticle(SimpleParticleType type, ClientLevel level,
                                             double x, double y, double z,
                                             double xd, double yd, double zd) {
            return new CannonParticle(level, x, y, z, xd, yd, zd, sprites, style);
        }
    }
}
