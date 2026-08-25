package redteamai;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.content.StatusEffects;
import mindustry.gen.Groups;
import mindustry.gen.Player;
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
        new DysonEngineerUnitType("dyson-engineer").load();
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

        public boolean hasDysonSwarm = false;

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
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    if (u.hasEffect(StatusEffects.invincible)) continue;
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

            // 戴森云特效
            if (hasDysonSwarm) {
                Draw.z(105f);
                float cloudTime = time * 0.8f;
                for (int i = 0; i < 3; i++) {
                    float r = baseRadius + 12f + i * 7f;
                    float alpha = 0.25f + 0.1f * Mathf.sin(cloudTime * 0.5f + i * 2f);
                    Draw.color(Color.valueOf("ffd700"), alpha);
                    Lines.stroke(2.5f + Mathf.sin(cloudTime + i * 60f, 5f, 1.5f));
                    Lines.circle(x, y, r + Mathf.sin(cloudTime + i * 60f, 10f, 3f));
                }
                Mathf.rand.setSeed(42);
                for (int i = 0; i < 20; i++) {
                    float a = cloudTime * (1.2f + i * 0.1f) + i * 137f;
                    float r = baseRadius + 15f + (i % 3) * 7f;
                    Draw.color(Color.valueOf("fffacd"), 0.6f);
                    Fill.circle(x + Angles.trnsx(a, r), y + Angles.trnsy(a, r), 1.5f);
                }
                Mathf.rand.setSeed(0);
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
            unit.health = health;

            float swing = Mathf.sin(Time.time, 25f, 8f);
            float damage = dps * Time.delta;
            for (int sign : new int[]{1, -1}) {
                float a = (sign > 0 ? 0f : 180f) + swing;
                float ex = unit.x + Angles.trnsx(a, jetLength);
                float ey = unit.y + Angles.trnsy(a, jetLength);
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (u.hasEffect(StatusEffects.invincible)) continue;
                    if (distanceToSegment(u.x, u.y, unit.x, unit.y, ex, ey) <= 4f + u.hitSize) {
                        u.damage(damage);
                    }
                }
            }

            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                if (u.hasEffect(StatusEffects.invincible)) continue;
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
            unit.health = health;

            float swing = Mathf.sin(Time.time, 40f, 6f);
            float damage = dps * Time.delta;
            for (int sign : new int[]{1, -1}) {
                float a = 90f * sign + swing;
                float ex = unit.x + Angles.trnsx(a, jetLength);
                float ey = unit.y + Angles.trnsy(a, jetLength);
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (u.hasEffect(StatusEffects.invincible)) continue;
                    if (distanceToSegment(u.x, u.y, unit.x, unit.y, ex, ey) <= 5f + u.hitSize) {
                        u.damage(damage);
                    }
                }
            }

            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                if (u.hasEffect(StatusEffects.invincible)) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                }
                if (dst <= unit.hitSize + u.hitSize + 5f) {
                    if (DEBUG) Log.info("[PulsarMod] 黑洞吞噬 " + u.type);
                    u.kill();
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
    //  ✅ 戴森工程师（修复点击检测 + 右上角按钮）
    // ====================================================================
    public static class DysonEngineerUnitType extends UnitType {

        private final float detectRange = 300f;
        private final float btnRadius = 18f;
        private final float btnOffsetX = 30f;
        private final float btnOffsetY = 30f;
        private boolean btnActive = false;

        public DysonEngineerUnitType(String name) {
            super(name);
            health = 5000;
            speed = 2.5f;
            rotateSpeed = 5f;
            hitSize = 16f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            region = Core.atlas.find("clear");
            drawBody = false;
            drawCell = false;
            localizedName = "戴森工程师";
        }

        @Override
        public void update(Unit unit) {
            // 检测附近黄矮星
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team != unit.team) continue;
                if (u.type instanceof YellowDwarfUnitType) {
                    if (Mathf.dst(unit.x, unit.y, u.x, u.y) <= detectRange) {
                        ((YellowDwarfUnitType) u.type).hasDysonSwarm = true;
                    }
                }
            }

            // ✅ 用 isJustTapped() 检测点击
            if (Core.input.isJustTapped()) {
                Vec2 tap = Core.input.mouseWorld();
                float bx = unit.x + btnOffsetX;
                float by = unit.y + btnOffsetY;

                if (Mathf.dst(tap.x, tap.y, bx, by) < btnRadius) {
                    btnActive = !btnActive;
                    if (btnActive) {
                        // ✅ 30秒无敌（30 * 60 ticks）
                        unit.apply(StatusEffects.invincible, 30f * 60f);
                        if (DEBUG) Log.info("[PulsarMod] 戴森工程师开启30秒无敌！");
                    } else {
                        unit.unapply(StatusEffects.invincible);
                        if (DEBUG) Log.info("[PulsarMod] 无敌已关闭");
                    }
                }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y;

            // 主体
            Draw.z(100f);
            Draw.color(Color.valueOf("4488ff"));
            Fill.circle(x, y, 8f);
            Draw.color(Color.valueOf("aaddff"));
            Fill.circle(x, y, 4f);

            // ✅ 右上角按钮
            float bx = x + btnOffsetX;
            float by = y + btnOffsetY;

            Draw.z(200f);
            Draw.color(btnActive ? Color.green : Color.gray);
            Fill.circle(bx, by, btnRadius);
            Draw.color(Color.white);
            Fill.triangle(
                bx - 5f, by - 5f,
                bx - 5f, by + 5f,
                bx + 6f, by
            );

            Draw.reset();
            Draw.z(0f);
        }
    }
}
