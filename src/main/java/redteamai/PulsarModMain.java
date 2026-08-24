package redteamai;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.game.Team;
import mindustry.mod.Mod;
import mindustry.type.UnitType;

public class PulsarModMain extends Mod {

    public static boolean DEBUG = false;

    @Override
    public void loadContent() {
        Log.info("[PulsarMod] 加载恒星单位...");
        new YellowDwarfUnitType("yellow-dwarf").load();
        new BluePulsarUnitType("blue-pulsar").load();
        new BlackHoleUnitType("black-hole").load();
        Log.info("[PulsarMod] 所有单位注册完成");
    }

    // ==================== 黄矮星：吸入途中100万伤害（独有），无射线 ====================
    public static class YellowDwarfUnitType extends UnitType {
        public Color coreColor = Color.valueOf("ffd37f");
        public Color outerColor = Color.valueOf("ff9d00");
        public float pulseSpeed = 40f;
        public float baseRadius = 22f;

        public float gravityRange = 150f;
        public float gravityStrength = 1.0f;
        public float suckDamage = 1000000f;   // ✅ 黄矮星独有

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
            unit.health = health;
            Team sourceTeam = unit.team;
            for (Unit u : Groups.unit) {
                if (u == null || u.dead) continue;
                if (u.team == sourceTeam) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                    u.damage(suckDamage * Time.delta);   // ✅ 只有黄矮星有吸入伤害
                }
                float dynamicKillRange = unit.hitSize + u.hitSize + 5f;
                if (dst <= dynamicKillRange) {
                    if (DEBUG) Log.info("[PulsarMod] 黄矮星吞噬 " + u.type);
                    u.kill();
                }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            Draw.z(30f);
            float x = unit.x, y = unit.y, time = Time.time;
            float pulse = Mathf.sin(time, pulseSpeed, 1f);
            float radius = baseRadius + pulse * 3f;
            float waveProgress = (time % 40f) / 40f;
            Draw.z(100f);
            Draw.color(coreColor, (1f - waveProgress) * 0.4f);
            Lines.stroke(2f + pulse);
            Lines.circle(x, y, waveProgress * baseRadius * 3.5f);
            Draw.z(110f);
            Draw.color(outerColor, 0.25f + pulse * 0.1f);
            Fill.circle(x, y, radius * 1.6f);
            Draw.color(coreColor);
            Fill.circle(x, y, radius * 0.7f);
            Draw.color(Color.white, 0.8f);
            Fill.circle(x, y, radius * 0.35f);
            Draw.reset();
            Draw.z(0f);
        }
    }

    // ==================== 中子星：射线左右，无吸入伤害 ====================
    public static class BluePulsarUnitType extends UnitType {
        public Color coreColor = Color.valueOf("00e5ff");
        public Color outerColor = Color.valueOf("0099cc");
        public Color jetColor = Color.valueOf("00e5ff");
        public float pulseSpeed = 35f;
        public float baseRadius = 10f;
        public float dps = 80f;
        public float jetLengthMul = 40f;

        public int particleCount = 200;
        public float particleSpeed = 14f;

        public float gravityRange = 180f;
        public float gravityStrength = 4.0f;

        public BluePulsarUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE;
            speed = 0f;
            rotateSpeed = 12f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            region = Core.atlas.find("clear");   // 屏蔽原版贴图
            localizedName = "中子星";
        }

        @Override
        public void update(Unit unit) {
            unit.health = health;
            float length = unit.hitSize * jetLengthMul;
            float damage = dps * Time.delta;

            // ✅ 射线左右（0° / 180°）
            for (int sign : new int[]{1, -1}) {
                float a = (sign > 0 ? 0f : 180f);
                float ex = unit.x + Angles.trnsx(a, length);
                float ey = unit.y + Angles.trnsy(a, length);
                applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, unit.hitSize * 0.8f, damage);
            }

            Team sourceTeam = unit.team;
            for (Unit u : Groups.unit) {
                if (u == null || u.dead) continue;
                if (u.team == sourceTeam) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);

                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                    // ✅ 无吸入伤害
                }

                float dynamicKillRange = unit.hitSize + u.hitSize + 5f;
                if (dst <= dynamicKillRange) {
                    if (DEBUG) Log.info("[PulsarMod] 中子星吞噬 " + u.type);
                    u.kill();
                }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y, time = Time.time;
            float pulse = Mathf.sin(time, pulseSpeed, 1f);
            float radius = baseRadius + pulse * 2f;
            float length = radius * jetLengthMul;

            // ✅ 射线左右
            Draw.z(85f);
            drawFlowingJet(x, y, 0f, length, time, unit.id);
            drawFlowingJet(x, y, 180f, length * 0.9f, time, unit.id + 1000);

            // 实心核心（无洞）
            Draw.z(110f);
            Draw.color(coreColor);
            Fill.circle(x, y, radius * 0.75f);
            Draw.color(Color.white, 0.85f);
            Fill.circle(x, y, radius * 0.3f);

            Draw.reset();
            Draw.z(0f);
        }

        private void drawFlowingJet(float x, float y, float angle, float length, float time, long seed) {
            float spacing = length / particleCount;
            float travel = time * particleSpeed;
            Mathf.rand.setSeed(seed);
            for (int i = 0; i < particleCount; i++) {
                float dist = (travel + i * spacing) % length;
                float t = dist / length;
                float spread = t * 5f;
                float offset = Mathf.rand.random(-spread, spread);
                float finalAngle = angle + offset;
                float px = x + Angles.trnsx(finalAngle, dist);
                float py = y + Angles.trnsy(finalAngle, dist);
                Color c;
                if (t < 0.25f) c = Color.white.lerp(jetColor, t / 0.25f);
                else if (t < 0.7f) c = jetColor;
                else c = jetColor.lerp(outerColor, (t - 0.7f) / 0.3f);
                float flicker = (Mathf.sin(dist * 0.1f - time * 0.3f) + 1f) / 2f;
                float alpha = (1f - t * 0.85f) * (0.5f + flicker * 0.5f);
                float size = (1.0f - t * 0.8f) * 1.0f;
                size = size > 0.15f ? size : 0.15f;
                size *= Mathf.rand.random(0.6f, 1.0f);
                Draw.color(c, alpha);
                Fill.circle(px, py, size);
                if (Mathf.rand.chance(0.06f)) {
                    float sprayAngle = finalAngle + Mathf.rand.range(15f);
                    float sprayDist = dist + Mathf.rand.random(3f, 10f);
                    Draw.color(c, alpha * 0.4f);
                    Fill.circle(x + Angles.trnsx(sprayAngle, sprayDist),
                                 y + Angles.trnsy(sprayAngle, sprayDist), size * 0.5f);
                }
            }
            Mathf.rand.setSeed(0);
        }
    }

    // ==================== 黑洞：射线上下，无吸入伤害 ====================
    public static class BlackHoleUnitType extends UnitType {
        public float baseRadius = 6f;

        public float gravityRange = 250f;
        public float gravityStrength = 5.0f;

        public int particleCount = 320;
        public float particleSpeed = 22f;
        public float jetLength = 45f;
        public float dps = 200f;

        public int diskParticles = 150;
        public float diskRx = 20f;
        public float diskRy = 7f;
        public float diskSpeed = 15f;

        public BlackHoleUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE;
            speed = 0f;
            rotateSpeed = 0f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            region = Core.atlas.find("clear");   // 屏蔽原版贴图，保证纯黑
            localizedName = "黑洞";
        }

        @Override
        public void update(Unit unit) {
            unit.health = health;

            // ✅ 射线上下（90° / 270°）+ 射线伤害
            for (int sign : new int[]{1, -1}) {
                float a = 90f * sign;
                float ex = unit.x + Angles.trnsx(a, jetLength);
                float ey = unit.y + Angles.trnsy(a, jetLength);
                applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, unit.hitSize * 1.2f, dps * Time.delta);
            }

            Team sourceTeam = unit.team;
            for (Unit u : Groups.unit) {
                if (u == null || u.dead) continue;
                if (u.team == sourceTeam) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);

                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                    // ✅ 无吸入伤害
                }

                float dynamicKillRange = unit.hitSize + u.hitSize + 5f;
                if (dst <= dynamicKillRange) {
                    if (DEBUG) Log.info("[PulsarMod] 黑洞吞噬 " + u.type);
                    u.kill();
                }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y, time = unit.time;

            // 1. 上下双向狂暴射线（底层）
            Draw.z(85f);
            drawViolentJets(x, y, time);

            // 2. 椭圆黄蓝吸积盘（中层）
            Draw.z(95f);
            drawEllipticalDisk(x, y, time);

            // 3. 纯黑核心（顶层）
            Draw.z(110f);
            Draw.color(Color.black);
            Fill.circle(x, y, baseRadius);
            Draw.color(Color.valueOf("fff200"));
            Fill.circle(x, y, 1.5f);

            Draw.reset();
            Draw.z(0f);
        }

        private void drawViolentJets(float x, float y, float time) {
            Color jetColor = Color.valueOf("00e5ff");
            float spacing = jetLength / particleCount;
            float travel = time * particleSpeed;
            Mathf.rand.setSeed(0);
            for (int sign : new int[]{1, -1}) {
                float angle = 90f * sign;   // ✅ 上下
                for (int i = 0; i < particleCount; i++) {
                    float dist = (travel + i * spacing) % jetLength;
                    float t = dist / jetLength;
                    float spread = t * 3f;
                    float offset = Mathf.rand.random(-spread, spread);
                    float finalAngle = angle + offset;
                    float px = x + Angles.trnsx(finalAngle, dist);
                    float py = y + Angles.trnsy(finalAngle, dist);
                    Color c = (t < 0.2f) ? Color.white.lerp(jetColor, t / 0.2f) : jetColor;
                    float flicker = (Mathf.sin(dist * 0.2f - time * 0.5f) + 1f) / 2f;
                    float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);
                    float size = (0.8f - t * 0.6f) * Mathf.rand.random(0.5f, 1.0f);
                    size = Math.max(size, 0.15f);
                    Draw.color(c, alpha);
                    Fill.circle(px, py, size);
                    if (Mathf.rand.chance(0.08f)) {
                        float sprayAngle = finalAngle + Mathf.rand.range(15f);
                        float sprayDist = dist + Mathf.rand.random(2f, 8f);
                        Draw.color(c, alpha * 0.4f);
                        Fill.circle(x + Angles.trnsx(sprayAngle, sprayDist),
                                     y + Angles.trnsy(sprayAngle, sprayDist), size * 0.5f);
                    }
                }
            }
            Mathf.rand.setSeed(0);
        }

        private void drawEllipticalDisk(float x, float y, float time) {
            Color inner = Color.valueOf("fff200");
            Color mid = Color.valueOf("ffae00");
            Color outer = Color.valueOf("00b3ff");
            Mathf.rand.setSeed(777);
            for (int i = 0; i < diskParticles; i++) {
                float t = Mathf.rand.random(0f, 1f);
                float angle = time * diskSpeed * (1f + (1f - t) * 1.5f) + t * 360f * 2f;
                float rx = diskRx * (0.3f + t * 0.7f);
                float ry = diskRy * (0.3f + t * 0.7f);
                float px = x + Angles.trnsx(angle, rx);
                float py = y + Angles.trnsy(angle, ry);
                Color c = (t < 0.4f) ? inner.lerp(mid, t / 0.4f) : mid.lerp(outer, (t - 0.4f) / 0.6f);
                float flicker = (Mathf.sin(time * 8f + i * 0.9f) + 1f) / 2f;
                float alpha = (0.7f + flicker * 0.3f) * (1f - t * 0.6f);
                float size = (1.8f - t * 1.2f) + Mathf.sin(time * 6f + i) * 0.3f;
                size = Math.max(size, 0.3f);
                Draw.color(c, alpha);
                Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }
    }

    // ===== 共用伤害辅助 =====
    private static int applyDamageAlongLine(Unit source, float x1, float y1, float x2, float y2, float width, float damage) {
        int hit = 0;
        Team sourceTeam = source.team;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead) continue;
            if (u.team == sourceTeam) continue;
            if (distanceToSegment(u.x, u.y, x1, y1, x2, y2) <= width + u.hitSize) {
                u.damage(damage);
                hit++;
            }
        }
        return hit;
    }

    private static float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        float len2 = dx * dx + dy * dy;
        if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
        float t = ((px - x1) * dx + (py - y1) * dy) / len2;
        t = Mathf.clamp(t, 0f, 1f);
        float cx = x1 + t * dx, cy = y1 + t * dy;
        return Mathf.dst(px, py, cx, cy);
    }
}
