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

    public static boolean DEBUG = true;

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

        // 粒子束参数
        public int particleCount = 120;   // 每条喷流的粒子数
        public float particleSpeed = 8f;  // 粒子飞行速度（单位/秒）

        // 每单位实体维护两个喷流的粒子列表（用 Unit 的 transient 字段不行，这里用静态映射）
        // 为简单起见，直接在 draw 里用确定性随机 + 基于时间的相位来"模拟"运动
        // 但真正的运动需要在 update 里推进，所以我们在 update 里维护粒子

        // 临时粒子状态（update 里推进，draw 里读取）
        private transient Seq<JetParticle> northParticles;
        private transient Seq<JetParticle> southParticles;
        private boolean initialized = false;

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

            // 初始化粒子（每个单位独立）
            if (!initialized) {
                initParticles(unit, length);
                initialized = true;
            }

            float delta = Time.delta;

            // 推进两条喷流的粒子
            updateJetParticles(northParticles, length, delta, unit.id);
            updateJetParticles(southParticles, length, delta, unit.id + 1000);

            // 伤害：沿当前粒子分布的范围造成伤害
            applyJetDamage(unit, jetAngle, length);
        }

        private void initParticles(Unit unit, float length) {
            northParticles = new Seq<>(particleCount);
            southParticles = new Seq<>(particleCount);
            // 用单位id做种子，保证稳定
            Mathf.rand.setSeed(unit.id);
            for (int i = 0; i < particleCount; i++) {
                northParticles.add(createParticle(length));
            }
            Mathf.rand.setSeed(unit.id + 1000);
            for (int i = 0; i < particleCount; i++) {
                southParticles.add(createParticle(length));
            }
            Mathf.rand.setSeed(0);
        }

        private JetParticle createParticle(float length) {
            JetParticle p = new JetParticle();
            p.dist = Mathf.rand.random(0f, length);  // 初始均匀分布，一出现就是完整束
            p.offset = Mathf.rand.range(2.5f);        // 垂直偏移（锥形）
            p.speed = particleSpeed * Mathf.rand.range(0.8f, 1.2f); // 速度略有差异
            p.size = Mathf.rand.range(0.4f, 1.0f);
            p.phase = Mathf.rand.random(360f);         // 闪烁相位
            return p;
        }

        private void updateJetParticles(Seq<JetParticle> particles, float length, float delta, long seed) {
            Mathf.rand.setSeed(seed);
            for (int i = 0; i < particles.size; i++) {
                JetParticle p = particles.items[i];
                p.dist += p.speed * delta;  // ✅ 粒子真的往外飞
                if (p.dist > length) {
                    // 飞到末端 → 重置回核心，循环喷涌
                    p.dist = 0f;
                    p.offset = Mathf.rand.range(2.5f);
                    p.size = Mathf.rand.range(0.4f, 1.0f);
                    p.phase = Mathf.rand.random(360f);
                }
            }
            Mathf.rand.setSeed(0);
        }

        private void applyJetDamage(Unit source, float jetAngle, float length) {
            float damage = dps * Time.delta;
            Team sourceTeam = source.team;
            // 简化：对喷流末端附近 + 沿途的敌人造成伤害
            // 用一条宽线段覆盖整条喷流
            for (int sign : new int[]{1, -1}) {
                float ex = source.x + Angles.trnsx(jetAngle, length * sign);
                float ey = source.y + Angles.trnsy(jetAngle, length * sign);
                int hit = applyDamageAlongLine(source, source.x, source.y, ex, ey, source.hitSize * 0.8f, damage);
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

            // 绘制两条粒子束
            drawParticleJet(northParticles, x, y, jetAngle, jetLength, time, unit.id);
            drawParticleJet(southParticles, x, y, jetAngle + 180f, jetLength * 0.9f, time, unit.id + 1000);

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

        // 绘制一条由运动粒子组成的喷流
        private void drawParticleJet(Seq<JetParticle> particles, float x, float y, float angle, float length, float time, long seed) {
            if (particles == null) return;
            Mathf.rand.setSeed(seed);
            for (int i = 0; i < particles.size; i++) {
                JetParticle p = particles.items[i];
                float t = p.dist / length;  // 0(核心) → 1(末端)

                // 锥形扩散：越往外偏移越大
                float spread = t * 3f;
                float finalAngle = angle + p.offset + Mathf.sin(time / 20f + p.phase) * 0.05f;
                float finalOffset = p.offset * (1f + t * 1.5f);

                // 垂直方向偏移（用角度垂直分量）
                float px = x + Angles.trnsx(finalAngle, p.dist) + Angles.trnsx(finalAngle + 90f, finalOffset);
                float py = y + Angles.trnsy(finalAngle, p.dist) + Angles.trnsy(finalAngle + 90f, finalOffset);

                // 颜色渐变：核心白→蓝→青（亮蓝末端）
                Color c;
                if (t < 0.3f) c = Color.white.lerp(Color.cyan, t / 0.3f);
                else if (t < 0.7f) c = Color.cyan.lerp(jetColor, (t - 0.3f) / 0.4f);
                else c = jetColor.lerp(outerColor, (t - 0.7f) / 0.3f);

                // 闪烁
                float flicker = (Mathf.sin(time / 8f + p.phase) + 1f) / 2f;
                float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);

                // 核心附近更大更亮，末端细小
                float size = (1.2f - t * 0.9f) * p.size;
                size = size > 0.15f ? size : 0.15f;

                Draw.color(c, alpha);
                Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }
    }

    // 喷流粒子数据类
    public static class JetParticle {
        public float dist;    // 沿喷流方向的距离
        public float offset;  // 垂直偏移
        public float speed;   // 飞行速度
        public float size;    // 基础大小
        public float phase;   // 闪烁相位
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
                if (DEBUG && hit <= 3) {
                    Log.info("[PulsarMod] 命中 " + u.type + " HP剩余=" + (u.health - damage));
                }
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
