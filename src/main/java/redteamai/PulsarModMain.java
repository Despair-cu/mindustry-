package redteamai;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Log;
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
        new BlackHoleUnitType("black-hole").load();   // ✅ 黑洞
        Log.info("[PulsarMod] 所有单位注册完成");
    }

    // ==================== 黄矮星（弱引力 + 吸入伤害） ====================
    public static class YellowDwarfUnitType extends UnitType {
        public Color coreColor = Color.valueOf("ffd37f");
        public Color outerColor = Color.valueOf("ff9d00");
        public float pulseSpeed = 40f;
        public float baseRadius = 22f;

        public float gravityRange = 150f;
        public float gravityStrength = 1.0f;
        public float suckDamage = 1000000f;

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
                    u.damage(suckDamage * Time.delta);
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

            for (int i = 0; i < 3; i++) {
                float angle = time * (25f + i * 10f) + (i * 120f);
                float dist = radius * 0.5f;
                Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 60f, 10f, 0.5f));
                Fill.circle(x + Angles.trnsx(angle, dist), y + Angles.trnsy(angle, dist), 2.5f + pulse * 1.2f);
            }

            Mathf.rand.setSeed(unit.id);
            for (int i = 0; i < 4; i++) {
                float sa = Mathf.rand.random(360f), sd = Mathf.rand.random(radius * 0.8f, radius * 2.2f);
                Draw.color(coreColor, Mathf.rand.random(0.2f, 0.5f));
                Fill.circle(x + Angles.trnsx(sa, sd), y + Angles.trnsy(sa, sd), Mathf.rand.random(1f, 2.5f));
            }
            Mathf.rand.setSeed(0);

            Draw.reset();
            Draw.z(0f);
        }
    }

    // ==================== 中子星（亮蓝粒子喷流，完全原样） ====================
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
            localizedName = "中子星";
        }

        @Override
        public void update(Unit unit) {
            unit.health = health;

            float length = unit.hitSize * jetLengthMul;
            float jetAngle = unit.rotation + Mathf.sin(Time.time, 60f, 8f);
            float damage = dps * Time.delta;

            for (int sign : new int[]{1, -1}) {
                float ex = unit.x + Angles.trnsx(jetAngle, length * sign);
                float ey = unit.y + Angles.trnsy(jetAngle, length * sign);
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

                    if (DEBUG) {
                        Log.info("[PulsarMod] 吸引 " + u.type + " dst=" + (int) dst);
                    }
                }

                float dynamicKillRange = unit.hitSize + u.hitSize + 5f;
                if (dst <= dynamicKillRange) {
                    if (DEBUG) Log.info("[PulsarMod] 吞噬 " + u.type);
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
            float radius = baseRadius + pulse * 2f;

            float jetLength = radius * jetLengthMul;
            float jetAngle = unit.rotation + Mathf.sin(time, 60f, 8f);

            drawFlowingJet(x, y, jetAngle, jetLength, time, unit.id);
            drawFlowingJet(x, y, jetAngle + 180f, jetLength * 0.9f, time, unit.id + 1000);

            Draw.z(100f);
            float waveProgress = (time % 35f) / 35f;
            Draw.color(coreColor, (1f - waveProgress) * 0.5f);
            Lines.stroke(2f + pulse * 1.5f);
            Lines.circle(x, y, waveProgress * baseRadius * 4f);

            Draw.z(110f);
            Draw.color(outerColor, 0.3f + pulse * 0.15f);
            Fill.circle(x, y, radius * 1.8f);

            Draw.color(coreColor);
            Fill.circle(x, y, radius * 0.75f);

            Draw.color(Color.white, 0.85f);
            Fill.circle(x, y, radius * 0.3f);

            for (int i = 0; i < 6; i++) {
                float angle = time * (30f + i * 5f) + (i * 60f);
                float dist = radius * 0.55f;
                Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 45f, 8f, 0.5f));
                Fill.circle(x + Angles.trnsx(angle, dist), y + Angles.trnsy(angle, dist), 2f + pulse * 1.5f);
            }

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
                    float spx = x + Angles.trnsx(sprayAngle, sprayDist);
                    float spy = y + Angles.trnsy(sprayAngle, sprayDist);
                    Draw.color(c, alpha * 0.4f);
                    Fill.circle(spx, spy, size * 0.5f);
                }
            }
            Mathf.rand.setSeed(0);
        }
    }

    // ==================== 黑洞（扭曲光线 + 引力透镜） ====================
    public static class BlackHoleUnitType extends UnitType {
        public float baseRadius = 14f;
        public float pulseSpeed = 30f;

        public float gravityRange = 220f;
        public float gravityStrength = 2.5f;
        public float suckDamage = 2000000f;

        public int diskParticles = 180;
        public float diskSpeed = 3.0f;

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
                    u.damage(suckDamage * Time.delta);
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
            Draw.z(30f);

            float x = unit.x, y = unit.y, time = Time.time;
            float pulse = Mathf.sin(time, pulseSpeed, 1f);
            float radius = baseRadius + pulse * 1.5f;

            // 1. 引力透镜弧（扭曲光线）
            Draw.z(85f);
            drawGravitationalLensing(x, y, radius, time);

            // 2. 螺旋吸积盘
            Draw.z(90f);
            drawAccretionDisk(x, y, radius, time);

            // 3. 外发光晕
            Draw.z(100f);
            for (int i = 4; i >= 1; i--) {
                float r = radius * (2.0f + i * 0.5f);
                Draw.color(Color.valueOf("7b4bff"), 0.08f / i);
                Fill.circle(x, y, r);
            }

            // 4. 事件视界亮环
            Draw.z(105f);
            Draw.color(Color.valueOf("aa88ff"), 0.9f);
            Lines.stroke(2.5f + pulse * 0.5f);
            Lines.circle(x, y, radius * 1.15f);

            // 5. 纯黑核心
            Draw.z(110f);
            Draw.color(Color.black);
            Fill.circle(x, y, radius * 0.95f);

            // 6. 奇点高光
            Draw.color(Color.white, 0.6f);
            Fill.circle(x, y, radius * 0.15f);

            Draw.reset();
            Draw.z(0f);
        }

        private void drawGravitationalLensing(float x, float y, float radius, float time) {
            int arcs = 5;
            float baseR = radius * 3.2f;

            for (int a = 0; a < arcs; a++) {
                float arcPhase = time * 0.4f + a * (360f / arcs);
                float eccentricity = 1f + 0.35f * Mathf.sin(time * 0.3f + a * 1.7f);

                Draw.color(Color.valueOf("9d7bff"), 0.35f + 0.15f * Mathf.sin(time + a));
                Lines.stroke(1.5f);

                int segments = 30;
                float arcSpan = 140f;
                float prevSx = 0, prevSy = 0;
                for (int s = 0; s <= segments; s++) {
                    float ang = arcPhase + (s / (float) segments) * arcSpan;
                    float r = baseR * (1f - 0.25f * Mathf.cos(ang * Mathf.degRad));
                    r *= eccentricity;
                    float sx = x + Angles.trnsx(ang, r);
                    float sy = y + Angles.trnsy(ang, r);
                    if (s > 0) {
                        Lines.line(prevSx, prevSy, sx, sy);
                    }
                    prevSx = sx;
                    prevSy = sy;
                }
            }
            Lines.stroke(1f);
        }

        private void drawAccretionDisk(float x, float y, float radius, float time) {
            float diskRadius = radius * 4.5f;
            Mathf.rand.setSeed(12345);

            for (int i = 0; i < diskParticles; i++) {
                float t = Mathf.rand.random(0.3f, 1f);
                float angle = time * diskSpeed * (1f + (1f - t) * 2f) + t * 360f * 3f;
                float r = radius * 1.3f + t * (diskRadius - radius);

                float warp = Mathf.sin(time * 2f + t * 6f + i) * (1f - t) * 6f;
                float px = x + Angles.trnsx(angle, r);
                float py = y + Angles.trnsy(angle, r) + warp;

                Color c;
                if (t < 0.4f) c = Color.white.lerp(Color.valueOf("7b9bff"), t / 0.4f);
                else c = Color.valueOf("7b9bff").lerp(Color.valueOf("3b1b8f"), (t - 0.4f) / 0.6f);

                float flicker = (Mathf.sin(time * 5f + i * 0.7f) + 1f) / 2f;
                float alpha = (0.4f + flicker * 0.6f) * (1f - t * 0.5f);
                float size = (2.5f - t * 2f) + Mathf.sin(time * 4f + i) * 0.5f;
                size = size > 0.4f ? size : 0.4f;

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
