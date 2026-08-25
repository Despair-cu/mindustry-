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
import mindustry.game.Team;
import mindustry.mod.Mod;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;

public class PulsarModMain extends Mod {

    public static boolean DEBUG = false;

    public static StatusEffect invincible;

    @Override
    public void loadContent() {
        invincible = new StatusEffect("pulsar-invincible") {
            {
                healthMultiplier = Float.POSITIVE_INFINITY;
                show = false;
                damageMultiplier = 1f;
            }
        };

        Log.info("[PulsarMod] 加载恒星单位...");
        new YellowDwarfUnitType("yellow-dwarf").load();
        new BluePulsarUnitType("blue-pulsar").load();
        new BlackHoleUnitType("black-hole").load();
        new ShockwaveUnitType("shockwave-star").load();
        Log.info("[PulsarMod] 所有单位注册完成");
    }

    // ====================================================================
    //  黄矮星
    // ====================================================================
    public static class YellowDwarfUnitType extends UnitType {

        private final Color coreColor = Color.valueOf("ffd37f");
        private final Color outerColor = Color.valueOf("ff9d00");
        private final float pulseSpeed = 40f;
        private final float baseRadius = 22f;

        private final float gravityRange = 150f;
        private final float gravityStrength = 1.0f;
        private final float suckDamage = 1000000f;

        public YellowDwarfUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE;
            speed = 0f;
            rotateSpeed = 8f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            localizedName = "黄矮星";
        }

        @Override
        public void update(Unit unit) {
            unit.apply(invincible, 5f);

            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                    u.damage(suckDamage * Time.delta);
                }
                if (dst <= unit.hitSize + u.hitSize + 5f) {
                    if (DEBUG) Log.info("[PulsarMod] 黄矮星吞噬 " + u.type);
                    u.kill();
                }
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
            Draw.color(coreColor);
            Fill.circle(x, y, radius * 0.7f);
            Draw.color(Color.white, 0.8f);
            Fill.circle(x, y, radius * 0.35f);

            for (int i = 0; i < 3; i++) {
                float a = time * (25f + i * 10f) + i * 120f;
                float d = radius * 0.5f;
                Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 60f, 10f, 0.5f));
                Fill.circle(x + Angles.trnsx(a, d), y + Angles.trnsy(a, d), 2.5f + pulse * 1.2f);
            }

            Draw.reset();
            Draw.z(0f);
        }
    }

    // ====================================================================
    //  中子星
    // ====================================================================
    public static class BluePulsarUnitType extends UnitType {

        private final Color coreColor = Color.valueOf("00e5ff");
        private final Color outerColor = Color.valueOf("0099cc");
        private final Color jetColor = Color.valueOf("00e5ff");

        private final float baseRadius = 5f;
        private final int particleCount = 400;
        private final float particleSpeed = 30f;
        private final float jetLength = 1000f;
        private final float dps = 150f;

        private final float gravityRange = 180f;
        private final float gravityStrength = 4.0f;

        public BluePulsarUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE;
            speed = 0f;
            rotateSpeed = 12f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            region = Core.atlas.find("clear");
            drawBody = false;
            drawCell = false;
            localizedName = "中子星";
        }

        @Override
        public void update(Unit unit) {
            unit.apply(invincible, 5f);

            float swing = Mathf.sin(Time.time, 25f, 8f);
            float damage = dps * Time.delta;
            for (int sign : new int[]{1, -1}) {
                float a = (sign > 0 ? 0f : 180f) + swing;
                float ex = unit.x + Angles.trnsx(a, jetLength);
                float ey = unit.y + Angles.trnsy(a, jetLength);
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (distanceToSegment(u.x, u.y, unit.x, unit.y, ex, ey) <= 4f + u.hitSize) {
                        u.damage(damage);
                    }
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
                if (dst <= unit.hitSize + u.hitSize + 5f) {
                    if (DEBUG) Log.info("[PulsarMod] 中子星吞噬 " + u.type);
                    u.kill();
                }
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
            Draw.color(coreColor);
            Fill.circle(x, y, baseRadius * 1.5f);
            Fill.circle(x, y, baseRadius * 0.5f);

            Draw.reset();
            Draw.z(0f);
        }

        private void drawNeutronJet(float x, float y, float angle, float time, long seed) {
            float spacing = 3.0f;
            float travel = time * particleSpeed;
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

                Draw.color(c, alpha);
                Fill.circle(px, py, size);

                if (Mathf.rand.chance(0.06f)) {
                    float sa = a + Mathf.rand.range(12f);
                    float sd = dist + Mathf.rand.random(3f, 10f);
                    Draw.color(c, alpha * 0.3f);
                    Fill.circle(x + Angles.trnsx(sa, sd), y + Angles.trnsy(sa, sd), size * 0.4f);
                }
            }
            Mathf.rand.setSeed(0);
        }

        private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1;
            float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
            float t = ((px - x1) * dx + (py - y1) * dy) / len2;
            t = Mathf.clamp(t, 0f, 1f);
            return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
        }
    }

    // ====================================================================
    //  黑洞
    // ====================================================================
    public static class BlackHoleUnitType extends UnitType {

        private final float baseRadius = 6f;

        private final float gravityRange = 350f;
        private final float gravityStrength = 5.0f;

        private final int jetParticleCount = 380;
        private final float jetParticleSpeed = 28f;
        private final float jetLength = 220f;
        private final float dps = 300f;

        private final int diskParticles = 160;
        private final float diskRx = 24f;
        private final float diskRy = 9f;
        private final float diskSpeed = 12f;

        private final Color jetColor = Color.valueOf("c0c8d0");
        private final Color jetOuter = Color.valueOf("808890");
        private final Color coreColor = Color.valueOf("505050");

        private final Color diskInner = Color.valueOf("fff200");
        private final Color diskMid = Color.valueOf("ffae00");
        private final Color diskOuter = Color.valueOf("00b3ff");

        public BlackHoleUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE;
            speed = 0f;
            rotateSpeed = 0f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            localizedName = "黑洞";
        }

        @Override
        public void update(Unit unit) {
            unit.apply(invincible, 5f);

            float swing = Mathf.sin(Time.time, 40f, 6f);
            float damage = dps * Time.delta;
            for (int sign : new int[]{1, -1}) {
                float a = 90f * sign + swing;
                float ex = unit.x + Angles.trnsx(a, jetLength);
                float ey = unit.y + Angles.trnsy(a, jetLength);
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (distanceToSegment(u.x, u.y, unit.x, unit.y, ex, ey) <= 5f + u.hitSize) {
                        u.damage(damage);
                    }
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
                if (dst <= unit.hitSize + u.hitSize + 5f) {
                    if (DEBUG) Log.info("[PulsarMod] 黑洞湮灭 " + u.type);
                    u.remove();
                }
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
            Draw.color(coreColor);
            Fill.circle(x, y, baseRadius * 1.3f);
            Draw.color(Color.valueOf("888888"), 0.5f);
            Fill.circle(x, y, baseRadius * 0.4f);

            Draw.reset();
            Draw.z(0f);
        }

        private void drawBlackHoleJets(float x, float y, float swing, float time) {
            float spacing = 1.0f;
            float travel = time * jetParticleSpeed;
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

                    Draw.color(c, alpha);
                    Fill.circle(px, py, size);

                    if (Mathf.rand.chance(0.1f)) {
                        float sa = a + Mathf.rand.range(18f);
                        float sd = dist + Mathf.rand.random(4f, 15f);
                        Draw.color(c, alpha * 0.5f);
                        Fill.circle(x + Angles.trnsx(sa, sd), y + Angles.trnsy(sa, sd), size * 0.6f);
                    }
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

                Draw.color(c, alpha);
                Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }

        private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1;
            float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
            float t = ((px - x1) * dx + (py - y1) * dy) / len2;
            t = Mathf.clamp(t, 0f, 1f);
            return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
        }
    }

    // ====================================================================
    //  冲击波星：每20秒全图蓝色冲击波，秒杀一切（天体免疫，力墙可挡）
    // ====================================================================
    public static class ShockwaveUnitType extends UnitType {

        private final Color coreColor = Color.valueOf("00e5ff");
        private final Color outerColor = Color.valueOf("0099cc");

        private final float baseRadius = 5f;

        private final float shockwaveInterval = 20f * 60f; // 20秒
        private final float shockwaveSpeed = 600f;         // 更快的扩散速度
        private final float shockwaveMaxRadius = 10000f;   // ✅ 全图覆盖（10000远超常规地图）
        private final float shockwaveThickness = 12f;      // 波环厚度

        private float shockwaveTimer = 0f;
        private float shockwaveRadius = -1f;
        private final Seq<Building> hitShields = new Seq<>();

        public ShockwaveUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE;
            speed = 0f;
            rotateSpeed = 12f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            region = Core.atlas.find("clear");
            drawBody = false;
            drawCell = false;
            localizedName = "冲击波星";
        }

        @Override
        public void update(Unit unit) {
            unit.apply(invincible, 5f);

            shockwaveTimer += Time.delta;
            if (shockwaveTimer >= shockwaveInterval) {
                shockwaveTimer = 0f;
                shockwaveRadius = 0f;
                hitShields.clear();
                if (DEBUG) Log.info("[PulsarMod] 冲击波星释放全图冲击波！");
            }

            if (shockwaveRadius >= 0f) {
                float prevRadius = shockwaveRadius;
                shockwaveRadius += shockwaveSpeed * Time.delta;

                // 对单位
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead) continue;
                    if (u == unit || u.team == unit.team || isCelestialUnit(u)) continue;

                    float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                    if (dst >= prevRadius && dst <= shockwaveRadius + shockwaveThickness) {
                        if (!isBlockedByShield(unit.x, unit.y, u.x, u.y)) {
                            if (DEBUG) Log.info("[PulsarMod] 冲击波秒杀单位 " + u.type);
                            u.remove();
                        }
                    }
                }

                // 对建筑
                for (Building b : Groups.build) {
                    if (b == null || !b.isValid()) continue;
                    if (b.team == unit.team) continue;

                    float dst = Mathf.dst(unit.x, unit.y, b.x, b.y);
                    if (dst >= prevRadius && dst <= shockwaveRadius + shockwaveThickness) {
                        if (!isBlockedByShield(unit.x, unit.y, b.x, b.y)) {
                            if (DEBUG) Log.info("[PulsarMod] 冲击波摧毁建筑 " + b.block);
                            b.kill();
                        }
                    }
                }

                if (shockwaveRadius > shockwaveMaxRadius) {
                    shockwaveRadius = -1f;
                }
            }
        }

        private boolean isBlockedByShield(float x1, float y1, float x2, float y2) {
            for (Building b : Groups.build) {
                if (b == null || !b.isValid()) continue;
                if (!isShieldBuilding(b)) continue;
                if (hitShields.contains(b)) continue;

                float distToSegment = distanceToSegment(b.x, b.y, x1, y1, x2, y2);
                float blockSize = b.block.size * 4f;
                if (distToSegment <= blockSize + 3f) {
                    b.damage(200f);
                    hitShields.add(b);
                    if (DEBUG) Log.info("[PulsarMod] 力墙 " + b.block + " 挡下冲击波！");
                    return true;
                }
            }
            return false;
        }

        private boolean isShieldBuilding(Building b) {
            String name = b.block.name.toLowerCase();
            return name.contains("shield") || name.contains("force") || name.contains("wall");
        }

        private boolean isCelestialUnit(Unit u) {
            String typeName = u.type.name;
            return typeName.equals("yellow-dwarf") ||
                   typeName.equals("blue-pulsar") ||
                   typeName.equals("black-hole") ||
                   typeName.equals("shockwave-star");
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y, time = Time.time;

            // 画冲击波（全图级蓝色光环）
            if (shockwaveRadius >= 0f) {
                Draw.z(120f);
                float alpha = 1f - (shockwaveRadius / shockwaveMaxRadius);
                Draw.color(Color.valueOf("00e5ff"), alpha * 0.9f);
                Lines.stroke(4f + Mathf.sin(time, 15f, 3f));
                Lines.circle(x, y, shockwaveRadius);
                Draw.color(Color.valueOf("00e5ff"), alpha * 0.4f);
                Lines.stroke(12f);
                Lines.circle(x, y, shockwaveRadius);
                Draw.reset();
            }

            // 本体（中子星同款）
            Draw.z(110f);
            Draw.color(coreColor);
            Fill.circle(x, y, baseRadius * 1.5f);
            Fill.circle(x, y, baseRadius * 0.5f);
            Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time, 8f, 0.5f));
            Fill.circle(x, y, baseRadius * 0.8f);

            Draw.reset();
            Draw.z(0f);
        }

        private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1;
            float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
            float t = ((px - x1) * dx + (py - y1) * dy) / len2;
            t = Mathf.clamp(t, 0f, 1f);
            return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
        }
    }
}
