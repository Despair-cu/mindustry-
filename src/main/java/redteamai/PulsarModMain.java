package redteamai;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.mod.Mod;
import mindustry.type.UnitType;
import mindustry.world.blocks.defense.ShieldWall;

public class PulsarModMain extends Mod {

    public static boolean DEBUG = false;

    @Override
    public void loadContent() {
        Log.info("[PulsarMod] 加载恒星单位...");
        new YellowDwarfUnitType("yellow-dwarf").load();
        new BluePulsarUnitType("blue-pulsar").load();
        new BlackHoleUnitType("black-hole").load();
        new UnstableGravityWaveUnitType("unstable-gravity-wave").load();
        new DaoXuUnitType("daoxu-dreadnought").load();
        Log.info("[PulsarMod] 所有单位注册完成");
    }

    // ==================== 黄矮星 ====================
    // （你原来的代码，一字未动）
    public static class YellowDwarfUnitType extends UnitType {
        private final Color coreColor = Color.valueOf("ffd37f");
        private final Color outerColor = Color.valueOf("ff9d00");
        private final float pulseSpeed = 40f, baseRadius = 22f;
        private final float gravityRange = 150f, gravityStrength = 1.0f, suckDamage = 1000000f;

        public YellowDwarfUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE; speed = 0f; rotateSpeed = 8f;
            hitSize = baseRadius * 2f; constructor = UnitEntity::create;
            weapons = new Seq<>(); outlineColor = Color.valueOf("00000000");
            localizedName = "黄矮星";
        }

        @Override
        public void update(Unit unit) {
            unit.health = health;
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                    u.damage(suckDamage * Time.delta);
                }
                if (dst <= unit.hitSize + u.hitSize + 5f) { u.kill(); }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y, time = Time.time;
            float pulse = Mathf.sin(time, pulseSpeed, 1f);
            float radius = baseRadius + pulse * 3f;
            Draw.z(100f);
            float wave = (time % 40f) / 40f;
            Draw.color(coreColor, (1f - wave) * 0.4f);
            Lines.stroke(2f + pulse);
            Lines.circle(x, y, wave * baseRadius * 3.5f);
            Draw.color(outerColor, 0.25f + pulse * 0.1f);
            Fill.circle(x, y, radius * 1.6f);
            Draw.color(coreColor); Fill.circle(x, y, radius * 0.7f);
            Draw.color(Color.white, 0.8f); Fill.circle(x, y, radius * 0.35f);
            for (int i = 0; i < 3; i++) {
                float a = time * (25f + i * 10f) + i * 120f;
                float d = radius * 0.5f;
                Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 60f, 10f, 0.5f));
                Fill.circle(x + Angles.trnsx(a, d), y + Angles.trnsy(a, d), 2.5f + pulse * 1.2f);
            }
            Draw.reset(); Draw.z(0f);
        }
    }

    // ==================== 中子星 ====================
    // （你原来的代码，一字未动）
    public static class BluePulsarUnitType extends UnitType {
        private final Color coreColor = Color.valueOf("00e5ff");
        private final Color outerColor = Color.valueOf("0099cc");
        private final Color jetColor = Color.valueOf("00e5ff");
        private final float baseRadius = 5f;
        private final int particleCount = 400;
        private final float particleSpeed = 30f, jetLength = 1000f, dps = 150f;
        private final float gravityRange = 180f, gravityStrength = 4.0f;

        public BluePulsarUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE; speed = 0f; rotateSpeed = 12f;
            hitSize = baseRadius * 2f; constructor = UnitEntity::create;
            weapons = new Seq<>(); outlineColor = Color.valueOf("00000000");
            region = Core.atlas.find("clear"); drawBody = false; drawCell = false;
            localizedName = "中子星";
        }

        @Override
        public void update(Unit unit) {
            unit.health = health;
            float swing = Mathf.sin(Time.time, 25f, 8f);
            float damage = dps * Time.delta;
            for (int sign : new int[]{1, -1}) {
                float a = (sign > 0 ? 0f : 180f) + swing;
                float ex = unit.x + Angles.trnsx(a, jetLength);
                float ey = unit.y + Angles.trnsy(a, jetLength);
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (distanceToSegment(u.x, u.y, unit.x, unit.y, ex, ey) <= 4f + u.hitSize)
                        u.damage(damage);
                }
            }
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                }
                if (dst <= unit.hitSize + u.hitSize + 5f) { u.kill(); }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y, time = Time.time;
            Draw.z(85f);
            float swing = Mathf.sin(time, 25f, 8f);
            drawNeutronJet(x, y, 0f + swing, time, unit.id);
            drawNeutronJet(x, y, 180f + swing, time, unit.id + 1000);
            Draw.z(110f);
            Draw.color(coreColor); Fill.circle(x, y, baseRadius * 1.5f);
            Fill.circle(x, y, baseRadius * 0.5f);
            Draw.reset(); Draw.z(0f);
        }

        private void drawNeutronJet(float x, float y, float angle, float time, long seed) {
            float spacing = 3.0f; float travel = time * particleSpeed;
            Mathf.rand.setSeed(seed);
            for (int i = 0; i < particleCount; i++) {
                float dist = (travel + i * spacing) % jetLength;
                float t = dist / jetLength;
                float spread = t * 3f;
                float offset = Mathf.rand.random(-spread, spread);
                float a = angle + offset;
                float px = x + Angles.trnsx(a, dist);
                float py = y + Angles.trnsy(a, dist);
                Color c;
                if (t < 0.2f) c = Color.white.lerp(jetColor, t / 0.2f);
                else if (t < 0.7f) c = jetColor;
                else c = jetColor.lerp(outerColor, (t - 0.7f) / 0.3f);
                float flicker = (Mathf.sin(dist * 0.15f - time * 0.4f) + 1f) / 2f;
                float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);
                float size = (1.0f - t * 0.6f) * Mathf.rand.random(0.7f, 1.2f);
                size = Math.max(size, 0.15f);
                Draw.color(c, alpha); Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }

        private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1; float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
            float t = ((px - x1) * dx + (py - y1) * dy) / len2; t = Mathf.clamp(t, 0f, 1f);
            return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
        }
    }

    // ==================== 黑洞 ====================
    // （你原来的代码，一字未动）
    public static class BlackHoleUnitType extends UnitType {
        private final float baseRadius = 6f;
        private final float gravityRange = 350f, gravityStrength = 5.0f;
        private final int jetParticleCount = 380;
        private final float jetParticleSpeed = 28f, jetLength = 220f, dps = 300f;
        private final int diskParticles = 160;
        private final float diskRx = 24f, diskRy = 9f, diskSpeed = 12f;
        private final Color jetColor = Color.valueOf("c0c8d0");
        private final Color jetOuter = Color.valueOf("808890");
        private final Color coreColor = Color.valueOf("505050");
        private final Color diskInner = Color.valueOf("fff200");
        private final Color diskMid = Color.valueOf("ffae00");
        private final Color diskOuter = Color.valueOf("00b3ff");

        public BlackHoleUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE; speed = 0f; rotateSpeed = 0f;
            hitSize = baseRadius * 2f; constructor = UnitEntity::create;
            weapons = new Seq<>(); outlineColor = Color.valueOf("00000000");
            localizedName = "黑洞";
        }

        @Override
        public void update(Unit unit) {
            unit.health = health;
            float swing = Mathf.sin(Time.time, 40f, 6f);
            float damage = dps * Time.delta;
            for (int sign : new int[]{1, -1}) {
                float a = 90f * sign + swing;
                float ex = unit.x + Angles.trnsx(a, jetLength);
                float ey = unit.y + Angles.trnsy(a, jetLength);
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (distanceToSegment(u.x, u.y, unit.x, unit.y, ex, ey) <= 5f + u.hitSize)
                        u.damage(damage);
                }
            }
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                }
                if (dst <= unit.hitSize + u.hitSize + 5f) { u.remove(); }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y, time = Time.time;
            Draw.z(85f);
            float swing = Mathf.sin(time, 40f, 6f);
            drawBlackHoleJets(x, y, swing, time);
            Draw.z(95f);
            drawAccretionDisk(x, y, time);
            Draw.z(110f);
            Draw.color(coreColor); Fill.circle(x, y, baseRadius * 1.3f);
            Draw.color(Color.valueOf("888888"), 0.5f); Fill.circle(x, y, baseRadius * 0.4f);
            Draw.reset(); Draw.z(0f);
        }

        private void drawBlackHoleJets(float x, float y, float swing, float time) {
            float spacing = 1.0f; float travel = time * jetParticleSpeed;
            Mathf.rand.setSeed(0);
            for (int sign : new int[]{1, -1}) {
                float angle = 90f * sign + swing;
                for (int i = 0; i < jetParticleCount; i++) {
                    float dist = (travel + i * spacing) % jetLength;
                    float t = dist / jetLength;
                    float spread = t * 3.5f;
                    float offset = Mathf.rand.random(-spread, spread);
                    float a = angle + offset;
                    float px = x + Angles.trnsx(a, dist);
                    float py = y + Angles.trnsy(a, dist);
                    Color c;
                    if (t < 0.2f) c = Color.white.lerp(jetColor, t / 0.2f);
                    else if (t < 0.7f) c = jetColor;
                    else c = jetColor.lerp(jetOuter, (t - 0.7f) / 0.3f);
                    float flicker = (Mathf.sin(dist * 0.15f - time * 0.5f) + 1f) / 2f;
                    float alpha = (1f - t * 0.75f) * (0.6f + flicker * 0.4f);
                    float size = (1.5f - t * 1.0f) * Mathf.rand.random(0.8f, 1.5f);
                    size = Math.max(size, 0.25f);
                    Draw.color(c, alpha); Fill.circle(px, py, size);
                }
            }
            Mathf.rand.setSeed(0);
        }

        private void drawAccretionDisk(float x, float y, float time) {
            Mathf.rand.setSeed(777);
            for (int i = 0; i < diskParticles; i++) {
                float t = Mathf.rand.random(0f, 1f);
                float angle = time * diskSpeed * (1f + (1f - t) * 1.5f) + t * 360f * 2f;
                float rx = diskRx * (0.3f + t * 0.7f);
                float ry = diskRy * (0.3f + t * 0.7f);
                float px = x + Angles.trnsx(angle, rx);
                float py = y + Angles.trnsy(angle, ry);
                Color c = (t < 0.4f) ? diskInner.lerp(diskMid, t / 0.4f) : diskMid.lerp(diskOuter, (t - 0.4f) / 0.6f);
                float flicker = (Mathf.sin(time * 8f + i * 0.9f) + 1f) / 2f;
                float alpha = (0.7f + flicker * 0.3f) * (1f - t * 0.6f);
                float size = (2.0f - t * 1.3f) + Mathf.sin(time * 6f + i) * 0.3f;
                size = Math.max(size, 0.3f);
                Draw.color(c, alpha); Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }

        private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1; float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
            float t = ((px - x1) * dx + (py - y1) * dy) / len2; t = Mathf.clamp(t, 0f, 1f);
            return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
        }
    }

    // ==================== 不稳定引力波 ====================
    // （你原来的代码，一字未动）
    public static class UnstableGravityWaveUnitEntity extends UnitEntity {
        public float shockwaveTimer = 0f;
        public boolean shockwaveActive = false;
        public float shockwaveRadius = 0f;
        public float prevRadius = 0f;
        public Seq<Unit> hitUnits = new Seq<>();
        public Seq<Building> hitBuildings = new Seq<>();
    }

    public static class UnstableGravityWaveUnitType extends UnitType {
        private final Color coreColor = Color.valueOf("ff4040");
        private final Color outerColor = Color.valueOf("cc0000");
        private final Color jetColor = Color.valueOf("ff4040");
        private final float baseRadius = 5f;
        private final int particleCount = 400;
        private final float particleSpeed = 30f, jetLength = 1000f, dps = 150f;
        private final float gravityRange = 180f, gravityStrength = 4.0f;
        private final float shockwaveInterval = 10f * 60f;
        private final float shockwaveSpeed = 150f;
        private final float shockwaveThickness = 20f;
        private final float shockwaveMaxRadius = 10000f;

        public UnstableGravityWaveUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE; speed = 0f; rotateSpeed = 12f;
            hitSize = baseRadius * 2f; constructor = UnstableGravityWaveUnitEntity::create;
            weapons = new Seq<>(); outlineColor = Color.valueOf("00000000");
            region = Core.atlas.find("clear"); drawBody = false; drawCell = false;
            localizedName = "不稳定引力波";
        }

        @Override
        public void update(Unit unit) {
            if (!(unit instanceof UnstableGravityWaveUnitEntity)) return;
            UnstableGravityWaveUnitEntity entity = (UnstableGravityWaveUnitEntity) unit;

            unit.health = health;
            float swing = Mathf.sin(Time.time, 25f, 8f);
            float damage = dps * Time.delta;
            for (int sign : new int[]{1, -1}) {
                float a = (sign > 0 ? 0f : 180f) + swing;
                float ex = unit.x + Angles.trnsx(a, jetLength);
                float ey = unit.y + Angles.trnsy(a, jetLength);
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (distanceToSegment(u.x, u.y, unit.x, unit.y, ex, ey) <= 4f + u.hitSize)
                        u.damage(damage);
                }
            }
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                }
                if (dst <= unit.hitSize + u.hitSize + 5f) { u.kill(); }
            }

            entity.shockwaveTimer += Time.delta;
            if (!entity.shockwaveActive && entity.shockwaveTimer >= shockwaveInterval) {
                entity.shockwaveActive = true;
                entity.shockwaveRadius = unit.hitSize;
                entity.prevRadius = 0f;
                entity.hitUnits.clear();
                entity.hitBuildings.clear();
                if (DEBUG) Log.info("[PulsarMod] 不稳定引力波释放引力波！");
            }

            if (entity.shockwaveActive) {
                entity.prevRadius = entity.shockwaveRadius;
                entity.shockwaveRadius += shockwaveSpeed * (Time.delta / 60f);

                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (u.type instanceof YellowDwarfUnitType || u.type instanceof BluePulsarUnitType ||
                        u.type instanceof BlackHoleUnitType || u.type instanceof UnstableGravityWaveUnitType) continue;
                    if (entity.hitUnits.contains(u)) continue;
                    float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                    if (dst >= entity.prevRadius && dst <= entity.shockwaveRadius + shockwaveThickness) {
                        if (!isBlockedByShield(entity, u.x, u.y)) {
                            u.remove();
                            entity.hitUnits.add(u);
                        }
                    }
                }

                for (Building b : Groups.build) {
                    if (b == null || !b.isValid() || b.team == unit.team) continue;
                    if (entity.hitBuildings.contains(b)) continue;
                    float dst = Mathf.dst(unit.x, unit.y, b.x, b.y);
                    if (dst >= entity.prevRadius && dst <= entity.shockwaveRadius + shockwaveThickness) {
                        if (!isBlockedByShield(entity, b.x, b.y)) {
                            b.kill();
                            entity.hitBuildings.add(b);
                        }
                    }
                }

                if (entity.shockwaveRadius > shockwaveMaxRadius) {
                    entity.shockwaveActive = false;
                    entity.shockwaveTimer = 0f;
                }
            }
        }

        private boolean isBlockedByShield(UnstableGravityWaveUnitEntity entity, float tx, float ty) {
            float sx = entity.x, sy = entity.y;
            float totalDist = Mathf.dst(sx, sy, tx, ty);

            for (Building b : Groups.build) {
                if (b == null || !b.isValid()) continue;

                ShieldWall.ShieldWallBuild shield = null;
                if (b instanceof ShieldWall.ShieldWallBuild) {
                    shield = (ShieldWall.ShieldWallBuild) b;
                    if (shield.shield <= 0) continue;
                } else {
                    String bn = b.block.name.toLowerCase();
                    if (!(bn.contains("shield") || bn.contains("force") ||
                          bn.contains("力墙") || bn.contains("防护"))) continue;
                }

                float shieldRadius = shield != null ? shield.shieldRadius : 60f;
                float distToSource = Mathf.dst(sx, sy, b.x, b.y);

                if (distToSource < totalDist) {
                    if (distToSource <= entity.shockwaveRadius + shieldRadius &&
                        distToSource + shieldRadius >= entity.prevRadius) {
                        if (shield != null) {
                            shield.shield -= 500f * Time.delta;
                            if (shield.shield <= 0f) shield.breakTimer = 1f;
                        } else {
                            b.damage(500f * Time.delta);
                        }
                        if (DEBUG) Log.info("[PulsarMod] 力墙挡下引力波！");
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void draw(Unit unit) {
            if (!(unit instanceof UnstableGravityWaveUnitEntity)) return;
            UnstableGravityWaveUnitEntity entity = (UnstableGravityWaveUnitEntity) unit;

            Draw.reset();
            float x = unit.x, y = unit.y, time = Time.time;
            Draw.z(85f);
            float swing = Mathf.sin(time, 25f, 8f);
            drawNeutronJet(x, y, 0f + swing, time, unit.id);
            drawNeutronJet(x, y, 180f + swing, time, unit.id + 1000);
            Draw.z(110f);
            Draw.color(coreColor); Fill.circle(x, y, baseRadius * 1.5f);
            Fill.circle(x, y, baseRadius * 0.5f);

            if (entity.shockwaveActive) {
                Draw.z(130f);
                float alpha = 0.5f * (1f - entity.shockwaveRadius / shockwaveMaxRadius);
                Draw.color(coreColor, alpha * 2f);
                Lines.stroke(shockwaveThickness);
                Lines.circle(x, y, entity.shockwaveRadius);
                Draw.color(Color.white, alpha * 1.5f);
                Lines.stroke(3f + Mathf.sin(time * 10f) * 2f);
                Lines.circle(x, y, entity.shockwaveRadius);
                for (int i = 0; i < 48; i++) {
                    float a = time * 3f + i * 7.5f;
                    float px = x + Angles.trnsx(a, entity.shockwaveRadius);
                    float py = y + Angles.trnsy(a, entity.shockwaveRadius);
                    Draw.color(coreColor, alpha * 2f);
                    Fill.circle(px, py, 3f + Mathf.sin(time * 5f + i) * 2f);
                }
            }

            Draw.reset(); Draw.z(0f);
        }

        private void drawNeutronJet(float x, float y, float angle, float time, long seed) {
            float spacing = 3.0f; float travel = time * particleSpeed;
            Mathf.rand.setSeed(seed);
            for (int i = 0; i < particleCount; i++) {
                float dist = (travel + i * spacing) % jetLength;
                float t = dist / jetLength;
                float spread = t * 3f;
                float offset = Mathf.rand.random(-spread, spread);
                float a = angle + offset;
                float px = x + Angles.trnsx(a, dist);
                float py = y + Angles.trnsy(a, dist);
                Color c;
                if (t < 0.2f) c = Color.white.lerp(jetColor, t / 0.2f);
                else if (t < 0.7f) c = jetColor;
                else c = jetColor.lerp(outerColor, (t - 0.7f) / 0.3f);
                float flicker = (Mathf.sin(dist * 0.15f - time * 0.4f) + 1f) / 2f;
                float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);
                float size = (1.0f - t * 0.6f) * Mathf.rand.random(0.7f, 1.2f);
                size = Math.max(size, 0.15f);
                Draw.color(c, alpha); Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }

        private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1; float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
            float t = ((px - x1) * dx + (py - y1) * dy) / len2; t = Mathf.clamp(t, 0f, 1f);
            return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
        }
    }

    // ==================== 道虚-无畏舰 ====================

    public static class DaoXuUnitEntity extends UnitEntity {
        public boolean charging = false;
        public int chargeTicks = 0;
        public float blinkCooldown = 0f;
        public boolean blinkReady = true;
        public int weaponCooldowns[] = new int[20];
        public float engineWarmup = 0f;
        public float autoChargeTimer = 0f;
        public Seq<DaoXuBullet> bullets = new Seq<>();
        public boolean laserFiring = false;
        public float laserTimer = 0f;
        public float laserAngle = 0f;
        public float laserTargetX = 0f, laserTargetY = 0f;
    }

    public static class DaoXuBullet {
        public float x, y, vx, vy;
        public float damage;
        public int life;
        public float size;
    }

    public static class DaoXuUnitType extends UnitType {

        private static final float BLINK_RANGE = 5500f;
        private static final float BLINK_COOLDOWN = 600f;
        private static final int CHARGE_NEEDED = 23;
        private static final float AUTO_CHARGE_INTERVAL = 60f * 15f; // 15秒自动蓄能一次
        private final float laserRange = 1500f;

        private final float[][] weaponDefs = {
            { -120f,  80f, 800f,  120f, 12, 12f, 4f, 1 },
            {  120f,  80f, 800f,  120f, 12, 12f, 4f, 1 },
            { -130f,  60f, 800f,  120f, 12, 12f, 4f, 1 },
            {  130f,  60f, 800f,  120f, 12, 12f, 1f, 1 },
            { -100f, 100f, 900f,  280f, 22, 14f, 2f, 1 },
            {  100f, 100f, 900f,  280f, 22, 14f, 2f, 1 },
            { -140f,  20f, 700f,   80f,  6, 16f, 6f, 2 },
            {  140f,  20f, 700f,   80f,  6, 16f, 6f, 2 },
            {    0f, 140f,1000f,  600f, 45, 18f, 1f, 1 },
            { -150f, -40f,1100f, 1200f, 80, 20f, 0f, 1 },
            {  150f, -40f,1100f, 1200f, 80, 20f, 0f, 1 },
            { -110f, -80f, 850f,  350f, 28, 15f, 3f, 1 },
            {  110f, -80f, 850f,  350f, 28, 15f, 3f, 1 },
            { -160f,   0f, 950f,  200f, 18, 13f, 5f, 3 },
            {  160f,   0f, 950f,  200f, 18, 13f, 5f, 3 },
            {  -90f, 120f, 750f,  150f, 15, 11f, 3f, 1 },
            {   90f, 120f, 750f,  150f, 15, 11f, 3f, 1 },
            { -170f,  50f, 600f,   60f,  4, 17f, 8f, 2 },
            {  170f,  50f, 600f,   60f,  4, 17f, 8f, 2 },
            {    0f, -140f,1300f, 2000f,120, 25f, 0f, 1 },
        };

        // 子弹贴图，走 atlas
        private final TextureRegion bulletRegion = Core.atlas.find("daoxu-bullet");

        public DaoXuUnitType(String name) {
            super(name);
            health = 200000;
            speed = 0.65f;
            hitSize = 300f;
            constructor = DaoXuUnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            region = Core.atlas.find("daoxu");       // 船体贴图
            outlineRegion = Core.atlas.find("daoxu-outline");
            drawBody = true;                          // ← 关键：必须 true 才会画 region
            localizedName = "道虚-无畏舰";
        }

        @Override
        public void update(Unit unit) {
            if (!(unit instanceof DaoXuUnitEntity)) return;
            DaoXuUnitEntity e = (DaoXuUnitEntity) unit;
            unit.health = health;

            e.engineWarmup = Mathf.lerp(e.engineWarmup, unit.vel().len() / speed, 0.05f);

            if (e.blinkCooldown > 0f) {
                e.blinkCooldown -= Time.delta;
                if (e.blinkCooldown <= 0f) { e.blinkCooldown = 0f; e.blinkReady = true; }
            }

            // ===== 自动蓄能：每 15 秒触发一次聚能激光 =====
            e.autoChargeTimer += Time.delta;
            if (!e.charging && !e.laserFiring && e.autoChargeTimer >= AUTO_CHARGE_INTERVAL) {
                e.charging = true;
                e.chargeTicks = 0;
                e.autoChargeTimer = 0f;
            }

            if (e.charging) {
                e.chargeTicks++;
                if (e.chargeTicks >= CHARGE_NEEDED) {
                    Unit target = null;
                    float closest = laserRange;
                    for (Unit u : Groups.unit) {
                        if (u == null || u.dead || u.team == unit.team) continue;
                        float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                        if (dst < closest) { closest = dst; target = u; }
                    }
                    if (target != null) {
                        e.laserFiring = true;
                        e.laserTimer = 30f;
                        e.laserAngle = Angles.angle(unit.x, unit.y, target.x, target.y);
                        e.laserTargetX = target.x;
                        e.laserTargetY = target.y;
                        for (Unit u : Groups.unit) {
                            if (u == null || u.dead || u.team == unit.team) continue;
                            if (Math.abs(Angles.angle(unit.x, unit.y, u.x, u.y) - e.laserAngle) < 4f &&
                                Mathf.dst(unit.x, unit.y, u.x, u.y) < laserRange) {
                                u.damage(50000f);
                            }
                        }
                    }
                    e.charging = false;
                    e.chargeTicks = 0;
                }
            }

            if (e.laserFiring) {
                e.laserTimer -= Time.delta;
                if (e.laserTimer <= 0f) e.laserFiring = false;
            }

            // ===== 20 组武器自动开火（有敌人就打）=====
            Unit fireTarget = null;
            float fireClosest = Float.MAX_VALUE;
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < fireClosest) { fireClosest = dst; fireTarget = u; }
            }

            if (fireTarget != null) {
                float baseAng = unit.rotation;
                for (int i = 0; i < 20; i++) {
                    if (e.weaponCooldowns[i] > 0) { e.weaponCooldowns[i]--; continue; }
                    float[] def = weaponDefs[i];
                    float wRange = def[2], wDmg = def[3];
                    int wCool = (int) def[4];
                    float wSpd = def[5], wSpread = def[6];
                    int wCount = (int) def[7];
                    if (fireClosest > wRange) continue;

                    float wx = unit.x + Angles.trnsx(baseAng, def[0]) + Angles.trnsx(baseAng + 90f, def[1]);
                    float wy = unit.y + Angles.trnsy(baseAng, def[0]) + Angles.trnsy(baseAng + 90f, def[1]);
                    float angToTarget = Angles.angle(wx, wy, fireTarget.x, fireTarget.y);

                    for (int s = 0; s < wCount; s++) {
                        float shotAng = angToTarget + Mathf.range(wSpread);
                        DaoXuBullet b = new DaoXuBullet();
                        b.x = wx + Angles.trnsx(shotAng, 10f);
                        b.y = wy + Angles.trnsy(shotAng, 10f);
                        b.vx = Angles.trnsx(shotAng, wSpd);
                        b.vy = Angles.trnsy(shotAng, wSpd);
                        b.damage = wDmg;
                        b.life = 60;
                        b.size = (i >= 9 && i <= 10) ? 8f : 4f;
                        e.bullets.add(b);
                    }
                    e.weaponCooldowns[i] = wCool;
                }
            }

            // ===== 更新弹幕（贴图绘制由 draw() 负责，这里只做逻辑）=====
            Seq<DaoXuBullet> toRemove = new Seq<>();
            for (DaoXuBullet b : e.bullets) {
                b.x += b.vx;
                b.y += b.vy;
                b.life--;
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (Mathf.dst(b.x, b.y, u.x, u.y) < u.hitSize / 2f + 6f) {
                        u.damage(b.damage);
                        toRemove.add(b);
                        break;
                    }
                }
                if (b.life <= 0) toRemove.add(b);
            }
            e.bullets.removeAll(toRemove);
        }

        @Override
        public void draw(Unit unit) {
            // 先画船体贴图（drawBody=true 时 super.draw 会画 region）
            super.draw(unit);
            if (!(unit instanceof DaoXuUnitEntity)) return;
            DaoXuUnitEntity e = (DaoXuUnitEntity) unit;

            // 蓄能光环
            if (e.charging) {
                float progress = (float) e.chargeTicks / CHARGE_NEEDED;
                Draw.z(108f);
                Draw.color(Color.valueOf("00e5ff"), progress * 0.4f);
                Fill.circle(unit.x, unit.y, 160f + progress * 30f);
                Draw.reset();
            }

            // 聚能激光束
            if (e.laserFiring) {
                Draw.z(115f);
                float alpha = e.laserTimer / 30f;
                Draw.color(Color.valueOf("00e5ff"), alpha);
                Lines.stroke(10f);
                Lines.line(unit.x, unit.y, e.laserTargetX, e.laserTargetY);
                Draw.reset();
            }

            // 弹幕：用贴图绘制
            if (bulletRegion != null && !bulletRegion.equals(Core.atlas.find("clear"))) {
                Draw.z(112f);
                for (DaoXuBullet b : e.bullets) {
                    Draw.rect(bulletRegion, b.x, b.y, b.size, b.size, Angles.angle(b.vx, b.vy));
                }
                Draw.reset();
            }

            // 折跃就绪指示
            if (e.blinkReady) {
                Draw.z(106f);
                Draw.color(Color.valueOf("00ff88"), 0.15f + Mathf.sin(Time.time, 20f, 0.1f));
                Lines.stroke(2f);
                Lines.circle(unit.x, unit.y, 180f + Mathf.sin(Time.time, 15f, 10f));
                Draw.reset();
            }
            Draw.z(0f);
        }
    }
