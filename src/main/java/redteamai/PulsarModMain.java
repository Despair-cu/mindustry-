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
            health = 450; speed = 1.4f; rotateSpeed = 8f;
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
        public Color coreColor = Color.valueOf("5b6cff");
        public Color outerColor = Color.valueOf("9d4dff");
        public Color jetColor = Color.valueOf("00e5ff");
        public float pulseSpeed = 35f;
        public float baseRadius = 10f;
        public float dps = 60f;
        public float jetLengthMul = 40f;

        // 粒子束参数（纯数学计算，不存任何状态）
        public int particleCount = 120;
        public float particleSpeed = 12f;  // 像素/秒，控制流动速度

        public BluePulsarUnitType(String name) {
            super(name);
            health = 500; speed = 1.2f; rotateSpeed = 12f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            localizedName = "中子星";
        }

        @Override
        public void update(Unit unit) {
            float length = unit.hitSize * jetLengthMul;
            float jetAngle = unit.rotation + Mathf.sin(Time.time, 60f, 8f);
            float damage = dps * Time.delta;

            for (int sign : new int[]{1, -1}) {
                float ex = unit.x + Angles.trnsx(jetAngle, length * sign);
                float ey = unit.y + Angles.trnsy(jetAngle, length * sign);
                int hit = applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, unit.hitSize * 0.8f, damage);
                if (DEBUG && hit > 0) {
                    Log.info("[PulsarMod] 喷流命中 " + hit + " 个单位");
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

            // 纯数学计算绘制流动粒子束，零状态
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

        // ✅ 纯数学流动粒子束（无状态、不存数组、绝不闪退）
        private void drawFlowingJet(float x, float y, float angle, float length, float time, long seed) {
            float spacing = length / particleCount;  // 粒子间距
            float travel = time * particleSpeed;     // 已飞行的距离

            Mathf.rand.setSeed(seed);

            for (int i = 0; i < particleCount; i++) {
                // ✅ 核心公式：粒子位置 = (已飞行距离 + 偏移) % 总长度
                float dist = (travel + i * spacing) % length;
                float t = dist / length;  // 0→1 归一化位置

                // 锥形扩散：越往外越散
                float spread = t * 3f;
                float offset = Mathf.rand.random(-spread, spread);
                float finalAngle = angle + offset;

                float px = x + Angles.trnsx(finalAngle, dist);
                float py = y + Angles.trnsy(finalAngle, dist);

                // 颜色渐变：核心白→蓝→亮青→暗紫
                Color c;
                if (t < 0.2f) c = Color.white.lerp(Color.cyan, t / 0.2f);
                else if (t < 0.6f) c = Color.cyan.lerp(jetColor, (t - 0.2f) / 0.4f);
                else c = jetColor.lerp(outerColor, (t - 0.6f) / 0.4f);

                // 闪烁流动感
                float flicker = (Mathf.sin(dist * 0.1f - time * 0.3f) + 1f) / 2f;
                float alpha = (1f - t * 0.85f) * (0.5f + flicker * 0.5f);

                // 大小：核心粗，末端细
                float size = (1.0f - t * 0.8f) * Mathf.rand.random(0.6f, 1.0f);
                size = size > 0.15f ? size : 0.15f;

                Draw.color(c, alpha);
                Fill.circle(px, py, size);

                // 次级溅射（少量）
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
