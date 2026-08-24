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

        public int particleCount = 120;
        public float particleSpeed = 8f;

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

            if (!initialized) {
                initParticles(unit, length);
                initialized = true;
            }

            float delta = Time.delta;
            updateJetParticles(northParticles, length, delta, unit.id);
            updateJetParticles(southParticles, length, delta, unit.id + 1000);

            applyJetDamage(unit, jetAngle, length);
        }

        private void initParticles(Unit unit, float length) {
            northParticles = new Seq<>(particleCount);
            southParticles = new Seq<>(particleCount);
            Mathf.rand.setSeed(unit.id);
            for (int i = 0; i < particleCount; i++) northParticles.add(createParticle(length));
            Mathf.rand.setSeed(unit.id + 1000);
            for (int i = 0; i < particleCount; i++) southParticles.add(createParticle(length));
            Mathf.rand.setSeed(0);
        }

        private JetParticle createParticle(float length) {
            JetParticle p = new JetParticle();
            p.dist = Mathf.rand.random(0f, length);  // ✅ random 双参数
            p.offset = Mathf.rand.random(-2.5f, 2.5f); // ✅ random 双参数
            p.speed = particleSpeed * Mathf.rand.random(0.8f, 1.2f); // ✅ random
            p.size = Mathf.rand.random(0.4f, 1.0f);  // ✅ random
            p.phase = Mathf.rand.random(360f);
            return p;
        }

        private void updateJetParticles(Seq<JetParticle> particles, float length, float delta, long seed) {
            Mathf.rand.setSeed(seed);
            for (int i = 0; i < particles.size; i++) {
                JetParticle p = particles.items[i];
                p.dist += p.speed * delta;
                if (p.dist > length) {
                    p.dist = 0f;
                    p.offset = Mathf.rand.random(-2.5f, 2.5f); // ✅ random
                    p.size = Mathf.rand.random(0.4f, 1.0f);      // ✅ random
                    p.phase = Mathf.rand.random(360f);
                }
            }
            Mathf.rand.setSeed(0);
        }

        private void applyJetDamage(Unit source, float jetAngle, float length) {
            float damage = dps * Time.delta;
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

            drawParticleJet(northParticles, x, y, jetAngle, jetLength, time, unit.id);
            drawParticleJet(southParticles, x, y, jetAngle + 180f, jetLength * 0.9f, time, unit.id + 1000);

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

        private void drawParticleJet(Seq<JetParticle> particles, float x, float y, float angle, float length, float time, long seed) {
            if (particles == null) return;
            Mathf.rand.setSeed(seed);
            for (int i = 0; i < particles.size; i++) {
                JetParticle p = particles.items[i];
                float t = p.dist / length;

                float spread = t * 3f;
                float finalAngle = angle + p.offset + Mathf.sin(time / 20f + p.phase) * 0.05f;
                float finalOffset = p.offset * (1f + t * 1.5f);

                float px = x + Angles.trnsx(finalAngle, p.dist) + Angles.trnsx(finalAngle + 90f, finalOffset);
                float py = y + Angles.trnsy(finalAngle, p.dist) + Angles.trnsy(finalAngle + 90f, finalOffset);

                Color c;
                if (t < 0.3f) c = Color.white.lerp(Color.cyan, t / 0.3f);
                else if (t < 0.7f) c = Color.cyan.lerp(jetColor, (t - 0.3f) / 0.4f);
                else c = jetColor.lerp(outerColor, (t - 0.7f) / 0.3f);

                float flicker = (Mathf.sin(time / 8f + p.phase) + 1f) / 2f;
                float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);

                float size = (1.2f - t * 0.9f) * p.size;
                size = size > 0.15f ? size : 0.15f;

                Draw.color(c, alpha);
                Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }
    }

    public static class JetParticle {
        public float dist;
        public float offset;
        public float speed;
        public float size;
        public float phase;
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
