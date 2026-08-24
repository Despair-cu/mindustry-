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
        Log.info("[PulsarMod] 所有单位注册完成");
    }

    public static class YellowDwarfUnitType extends UnitType {
        public Color coreColor = Color.valueOf("ffd37f");
        public Color outerColor = Color.valueOf("ff9d00");
        public float pulseSpeed = 40f;
        public float baseRadius = 22f;

        public YellowDwarfUnitType(String name) {
            super(name);
            health = 450; speed = 0f; rotateSpeed = 8f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            localizedName = "黄矮星";
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

    public static class BluePulsarUnitType extends UnitType {
        public Color coreColor = Color.valueOf("00e5ff");
        public Color outerColor = Color.valueOf("0099cc");
        public Color jetColor = Color.valueOf("00e5ff");
        public float pulseSpeed = 35f;
        public float baseRadius = 10f;
        public float dps = 60f;
        public float jetLengthMul = 40f;

        public int particleCount = 180;
        public float particleSpeed = 12f;

        // 引力/吞噬参数
        public float gravityRange = 150f;   // 引力作用范围
        public float gravityStrength = 3.0f; // 每帧拉动距离（像素）
        public float killRange = 15f;       // 小于这个距离直接杀掉

        public BluePulsarUnitType(String name) {
            super(name);
            health = 999999f;
            speed = 0f;                      // ✅ 不可移动
            rotateSpeed = 12f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            invincible = true;               // ✅ 不可死亡
            localizedName = "中子星";
        }

        @Override
        public void update(Unit unit) {
            float length = unit.hitSize * jetLengthMul;
            float jetAngle = unit.rotation + Mathf.sin(Time.time, 60f, 8f);
            float damage = dps * Time.delta;

            // 喷流伤害
            for (int sign : new int[]{1, -1}) {
                float ex = unit.x + Angles.trnsx(jetAngle, length * sign);
                float ey = unit.y + Angles.trnsy(jetAngle, length * sign);
                applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, unit.hitSize * 0.8f, damage);
            }

            // ✅ 引力 + 吞噬（代码杀）
            Team sourceTeam = unit.team;
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.isFlying()) continue;
                if (u.team == sourceTeam) continue;

                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);

                if (dst < gravityRange) {
                    // 计算指向核心的方向
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y); // 从核心指向敌人
                    // 把敌人往反方向（即核心方向）拉
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);

                    if (DEBUG) {
                        Log.info("[PulsarMod] 吸引 " + u.type + " dst=" + (int) dst);
                    }
                }

                // ✅ 到达核心范围 → 代码杀
                if (dst <= killRange) {
                    if (DEBUG) Log.info("[PulsarMod] 吞噬 " + u.type);
                    u.kill(); // 被吸的单位直接死亡
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

            // 核心波纹
            Draw.z(100f);
            float waveProgress = (time % 35f) / 35f;
            Draw.color(coreColor, (1f - waveProgress) * 0.5f);
            Lines.stroke(2f + pulse * 1.5f);
            Lines.circle(x, y, waveProgress * baseRadius * 4f);

            // 外发光
            Draw.z(110f);
            Draw.color(outerColor, 0.3f + pulse * 0.15f);
            Fill.circle(x, y, radius * 1.8f);

            // 核心
            Draw.color(coreColor);
            Fill.circle(x, y, radius * 0.75f);

            // 高光
            Draw.color(Color.white, 0.85f);
            Fill.circle(x, y, radius * 0.3f);

            // 旋转节点
            for (int i = 0; i < 6; i++) {
                float angle = time * (30f + i * 5f) + (i * 60f);
                float dist = radius * 0.55f;
                Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 45f, 8f, 0.5f));
                Fill.circle(x + Angles.trnsx(angle, dist), y + Angles.trnsy(angle, dist), 2f + pulse * 1.5f);
            }

            Draw.reset();
            Draw.z(0f);
        }

        // ✅ 亮蓝流动粒子束（加宽、浓密）
        private void drawFlowingJet(float x, float y, float angle, float length, float time, long seed) {
            float spacing = length / particleCount;
            float travel = time * particleSpeed;

            Mathf.rand.setSeed(seed);
            for (int i = 0; i < particleCount; i++) {
                float dist = (travel + i * spacing) % length;
                float t = dist / length;

                // ✅ 加宽：spread 从 t*3 提到 t*5
                float spread = t * 5f;
                float offset = Mathf.rand.random(-spread, spread);
                float finalAngle = angle + offset;

                float px = x + Angles.trnsx(finalAngle, dist);
                float py = y + Angles.trnsy(finalAngle, dist);

                // ✅ 全部亮蓝渐变
                Color c;
                if (t < 0.3f) c = Color.white.lerp(jetColor, t / 0.3f);
                else if (t < 0.7f) c = jetColor;
                else c = jetColor.lerp(outerColor, (t - 0.7f) / 0.3f);

                float flicker = (Mathf.sin(dist * 0.1f - time * 0.3f) + 1f) / 2f;
                float alpha = (1f - t * 0.85f) * (0.5f + flicker * 0.5f);

                float size = (1.0f - t * 0.8f) * Mathf.rand.random(0.6f, 1.0f);
                size = size > 0.15f ? size : 0.15f;

                Draw.color(c, alpha);
                Fill.circle(px, py, size);

                // 次级溅射
                if (Mathf.rand.chance(0.08f)) {
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

    // ✅ 伤害判定（沿线段）
    private static int applyDamageAlongLine(Unit source, float x1, float y1, float x2, float y2, float width, float damage) {
        int hit = 0;
        Team sourceTeam = source.team;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead || u.isFlying()) continue;
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
