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
        new BlackHoleUnitType("black-hole").load();
        Log.info("[PulsarMod] 所有单位注册完成");
    }

    // ==================== 黄矮星 ====================
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

    // ==================== 中子星 ====================
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

    // ==================== 黑洞（双向狂暴射线 + 纯黑核心 + 小椭圆黄蓝吸积盘） ====================
    public static class BlackHoleUnitType extends UnitType {
        public float baseRadius = 12f;
        public float pulseSpeed = 25f;

        // 引力（最强）
        public float gravityRange = 250f;
        public float gravityStrength = 5.0f;
        public float suckDamage = 3000000f;

        // 狂暴粒子射线（双向，比中子星更猛）
        public int particleCount = 320;
        public float particleSpeed = 22f;
        public float jetLengthMul = 55f;
        public float dps = 200f;

        // 小椭圆吸积盘
        public int diskParticles = 140;
        public float diskRx = 18f;
        public float diskRy = 7f;
        public float diskSpeed = 6.0f;

        // 颜色
        public Color diskColorInner = Color.valueOf("fff200");
        public Color diskColorMid = Color.valueOf("ffae00");
        public Color diskColorOuter = Color.valueOf("00b3ff");

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

            float length = unit.hitSize * jetLengthMul;
            float jetAngle = unit.rotation + Mathf.sin(Time.time, 40f, 12f);
            float damage = dps * Time.delta;

            // ✅ 双向狂暴粒子射线伤害（和中子星一样双向，但更粗更猛）
            for (int sign : new int[]{1, -1}) {
                float a = jetAngle + (sign > 0 ? 0 : 180);
                float ex = unit.x + Angles.trnsx(a, length);
                float ey = unit.y + Angles.trnsy(a, length);
                applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, unit.hitSize * 1.2f, damage);
            }

            // 引力 + 代码杀
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

            float jetLength = radius * jetLengthMul;
            float jetAngle = unit.rotation + Mathf.sin(time, 40f, 12f);

            // 1. 双向狂暴粒子射线
            Draw.z(80f);
            drawViolentJets(x, y, jetAngle, jetLength, time, unit.id);

            // 2. 小椭圆吸积盘（亮黄→蓝）
            Draw.z(95f);
            drawEllipticalDisk(x, y, time);

            // 3. 事件视界外圈光晕
            Draw.z(100f);
            Draw.color(Color.valueOf("5b2bff"), 0.25f);
            Fill.circle(x, y, radius * 1.6f);
            Draw.color(Color.valueOf("5b2bff"), 0.15f);
            Fill.circle(x, y, radius * 2.2f);

            // 4. 事件视界边缘亮环
            Draw.z(105f);
            Draw.color(Color.valueOf("ffd24a"), 0.9f);
            Lines.stroke(2.0f + pulse * 0.5f);
            Lines.circle(x, y, radius * 1.05f);

            // 5. 纯黑核心
            Draw.z(110f);
            Draw.color(Color.black);
            Fill.circle(x, y, radius * 0.95f);

            // 6. 奇点微光
            Draw.color(Color.valueOf("fff200"), 0.8f);
            Fill.circle(x, y, radius * 0.12f);

            Draw.reset();
            Draw.z(0f);
        }

        // ✅ 双向狂暴射线（只画两条）
        private void drawViolentJets(float x, float y, float baseAngle, float length, float time, long seed) {
            Mathf.rand.setSeed(seed);

            for (int sign : new int[]{1, -1}) {
                float angle = baseAngle + (sign > 0 ? 0 : 180);
                float spacing = length / particleCount;
                float travel = time * particleSpeed;

                for (int i = 0; i < particleCount / 2; i++) {
                    float dist = (travel + i * spacing * 2) % length;
                    float t = dist / length;

                    float spread = t * 7f;
                    float offset = Mathf.rand.random(-spread, spread);
                    float finalAngle = angle + offset;

                    float px = x + Angles.trnsx(finalAngle, dist);
                    float py = y + Angles.trnsy(finalAngle, dist);

                    Color c;
                    if (t < 0.2f) c = Color.white.lerp(Color.valueOf("7bffff"), t / 0.2f);
                    else if (t < 0.6f) c = Color.valueOf("00e5ff");
                    else c = Color.valueOf("00e5ff").lerp(Color.valueOf("0044aa"), (t - 0.6f) / 0.4f);

                    float flicker = (Mathf.sin(dist * 0.15f - time * 0.5f) + 1f) / 2f;
                    float alpha = (1f - t * 0.8f) * (0.6f + flicker * 0.4f);

                    float size = (1.8f - t * 1.4f) * Mathf.rand.random(0.7f, 1.3f);
                    size = size > 0.2f ? size : 0.2f;

                    Draw.color(c, alpha);
                    Fill.circle(px, py, size);

                    if (Mathf.rand.chance(0.12f)) {
                        float sprayAngle = finalAngle + Mathf.rand.range(20f);
                        float sprayDist = dist + Mathf.rand.random(5f, 15f);
                        float spx = x + Angles.trnsx(sprayAngle, sprayDist);
                        float spy = y + Angles.trnsy(sprayAngle, sprayDist);
                        Draw.color(c, alpha * 0.5f);
                        Fill.circle(spx, spy, size * 0.6f);
                    }
                }
            }
            Mathf.rand.setSeed(0);
        }

        // 小椭圆吸积盘
        private void drawEllipticalDisk(float x, float y, float time) {
            Mathf.rand.setSeed(777);
            for (int i = 0; i < diskParticles; i++) {
                float t = Mathf.rand.random(0f, 1f);
                float angle = time * diskSpeed * (1f + (1f - t) * 1.5f) + t * 360f * 2f;

                float rx = diskRx * (0.3f + t * 0.7f);
                float ry = diskRy * (0.3f + t * 0.7f);

                float px = x + Angles.trnsx(angle, rx);
                float py = y + Angles.trnsy(angle, ry);

                Color c;
                if (t < 0.4f) c = diskColorInner.lerp(diskColorMid, t / 0.4f);
                else c = diskColorMid.lerp(diskColorOuter, (t - 0.4f) / 0.6f);

                float flicker = (Mathf.sin(time * 8f + i * 0.9f) + 1f) / 2f;
                float alpha = (0.7f + flicker * 0.3f) * (1f - t * 0.6f);
                float size = (2.2f - t * 1.5f) + Mathf.sin(time * 6f + i) * 0.4f;
                size = size > 0.3f ? size : 0.3f;

                Draw.color(c, alpha);
                Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }
    }

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
