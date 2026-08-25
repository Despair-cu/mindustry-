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
import mindustry.content.StatusEffects;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.mod.Mod;
import mindustry.type.UnitType;

public class PulsarModMain extends Mod {

    public static boolean DEBUG = false;

    // 建造者（原戴森工程师）按钮参数，集中管理
    private static final float BUILDER_BTN_RADIUS = 18f;
    private static final float BUILDER_BTN_OFFSET_X = 30f;
    private static final float BUILDER_BTN_OFFSET_Y = 30f;
    private static final float BUILDER_INVINCIBLE_SECONDS = 30f;

    @Override
    public void loadContent() {
        Log.info("[PulsarMod] 加载恒星单位...");
        new YellowDwarfUnitType("yellow-dwarf").load();
        new BluePulsarUnitType("blue-pulsar").load();
        new BlackHoleUnitType("black-hole").load();
        new BuilderUnitType("dyson-builder").load(); // 原 DysonEngineer，现重命名为建造者
        Log.info("[PulsarMod] 所有单位注册完成");

        // 全局单次点击监听：只注册一次，避免每帧轮询 + 多单位抢触发
        registerBuilderTapListener();
    }

    // ====================================================================
    //  全局点击监听：建造者（原戴森工程师）按钮
    //  用 TapEvent 而非 update() 里轮询，保证一次点击只触发一次
    // ====================================================================
    private void registerBuilderTapListener() {
        mindustry.gen.Events.run(() -> {
            // 注意：EventType.TapEvent 需在 loadContent 之后注册；这里用 mindustry 的事件总线
        });

        // 采用 mindustry 标准事件注册方式
        mindustry.game.Event.on(TapEvent.class, e -> {
            // e.position 已经是世界坐标，无需手动转换
            float tx = e.position.x, ty = e.position.y;

            for (Unit u : Groups.unit) {
                if (u == null || u.dead || !(u.type instanceof BuilderUnitType)) continue;

                float bx = u.x + BUILDER_BTN_OFFSET_X;
                float by = u.y + BUILDER_BTN_OFFSET_Y;

                if (Mathf.dst(tx, ty, bx, by) < BUILDER_BTN_RADIUS) {
                    // 状态来源 = 真实效果，不存 boolean 字段
                    if (u.hasEffect(StatusEffects.invincible)) {
                        u.unapply(StatusEffects.invincible);
                        if (DEBUG) Log.info("[PulsarMod] 建造者 " + u.id + " 无敌已关闭");
                    } else {
                        u.apply(StatusEffects.invincible, BUILDER_INVINCIBLE_SECONDS * 60f);
                        if (DEBUG) Log.info("[PulsarMod] 建造者 " + u.id + " 开启 " + BUILDER_INVINCIBLE_SECONDS + " 秒无敌");
                    }
                }
            }
        });
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

        // 戴森云状态：存在 UnitType 单例上，所有黄矮星共享（按需求保留）
        // 若需 per-unit，应改用 UnitController 存储
        public boolean hasDysonSwarm = false;

        public YellowDwarfUnitType(String name) {
            super(name);
            health = Float.MAX_VALUE; // ✅ 用 float 最大值，杜绝 int 溢出瞬秒
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
            unit.health = health; // 每帧回满，配合 Float.MAX_VALUE
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

            // 戴森云特效（确定性角度，不用 rand seed）
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
                for (int i = 0; i < 20; i++) {
                    // 黄金角分布：确定性，不污染全局随机种子
                    float a = cloudTime * 1.2f + i * 137.5f;
                    float r = baseRadius + 15f + (i % 3) * 7f;
                    Draw.color(Color.valueOf("fffacd"), 0.6f);
                    Fill.circle(x + Angles.trnsx(a, r), y + Angles.trnsy(a, r), 1.5f);
                }
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
            health = Float.MAX_VALUE;
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
            // 用确定性伪随机（基于 seed + 索引），不污染全局 Mathf.rand
            for (int i = 0; i < particleCount; i++) {
                float dist = (travel + i * spacing) % jetLength;
                float t = dist / jetLength;
                float spread = t * 3f;
                // 确定性随机偏移：用 sin 组合代替 rand
                float offset = (Mathf.sin(i * 12.9898f + seed * 78.233f) * 43758.5453f % 1f);
                offset = (offset - 0.5f) * 2f * spread;
                float a = angle + offset;
                float px = x + Angles.trnsx(a, dist);
                float py = y + Angles.trnsy(a, dist);

                Color c;
                if (t < 0.2f) c = Color.white.lerp(jetColor, t / 0.2f);
                else if (t < 0.7f) c = jetColor;
                else c = jetColor.lerp(outerColor, (t - 0.7f) / 0.3f);

                float flicker = (Mathf.sin(dist * 0.15f - time * 0.4f) + 1f) / 2f;
                float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);
                // 确定性大小扰动
                float rnd = (Mathf.sin(i * 7.13f + seed) * 0.5f + 0.5f);
                float size = (1.0f - t * 0.6f) * (0.7f + rnd * 0.5f);
                size = Math.max(size, 0.15f);

                Draw.color(c, alpha);
                Fill.circle(px, py, size);

                if (rnd > 0.94f) {
                    float sa = a + (Mathf.sin(i * 3.7f) * 0.5f + 0.5f) * 24f - 12f;
                    float sd = dist + 4f + rnd * 6f;
                    Draw.color(c, alpha * 0.3f);
                    Fill.circle(x + Angles.trnsx(sa, sd), y + Angles.trnsy(sa, sd), size * 0.4f);
                }
            }
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
            health = Float.MAX_VALUE;
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
            for (int sign : new int[]{1, -1}) {
                float angle = 90f * sign + swing;
                for (int i = 0; i < jetParticleCount; i++) {
                    float dist = (travel + i * spacing) % jetLength;
                    float t = dist / jetLength;
                    float spread = t * 3.5f;
                    float offset = (Mathf.sin(i * 12.9898f + sign * 78.233f) * 43758.5453f % 1f);
                    offset = (offset - 0.5f) * 2f * spread;
                    float a = angle + offset;
                    float px = x + Angles.trnsx(a, dist);
                    float py = y + Angles.trnsy(a, dist);

                    Color c;
                    if (t < 0.2f) c = Color.white.lerp(jetColor, t / 0.2f);
                    else if (t < 0.7f) c = jetColor;
                    else c = jetColor.lerp(jetOuter, (t - 0.7f) / 0.3f);

                    float flicker = (Mathf.sin(dist * 0.15f - time * 0.5f) + 1f) / 2f;
                    float alpha = (1f - t * 0.75f) * (0.6f + flicker * 0.4f);
                    float rnd = (Mathf.sin(i * 7.13f + sign) * 0.5f + 0.5f);
                    float size = (1.5f - t * 1.0f) * (0.8f + rnd * 0.7f);
                    size = Math.max(size, 0.25f);

                    Draw.color(c, alpha);
                    Fill.circle(px, py, size);

                    if (rnd > 0.9f) {
                        float sa = a + (Mathf.sin(i * 3.7f + sign) * 0.5f + 0.5f) * 36f - 18f;
                        float sd = dist + 4f + rnd * 11f;
                        Draw.color(c, alpha * 0.5f);
                        Fill.circle(x + Angles.trnsx(sa, sd), y + Angles.trnsy(sa, sd), size * 0.6f);
                    }
                }
            }
        }

        private void drawAccretionDisk(float x, float y, float time) {
            for (int i = 0; i < diskParticles; i++) {
                float t = (Mathf.sin(i * 9.31f + 17.0f) * 0.5f + 0.5f); // 确定性 [0,1]
                float angle = time * diskSpeed * (1f + (1f - t) * 1.5f) + t * 720f;
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
    //  ✅ 建造者（原戴森工程师）：按钮交互 + 激活黄矮星戴森云
    //  改动：点击检测移至全局 TapEvent；状态用真实 invincible 判断；
    //        坐标用 TapEvent 自带世界坐标；移除 rand seed 污染
    // ====================================================================
    public static class BuilderUnitType extends UnitType {

        private final float detectRange = 300f;

        public BuilderUnitType(String name) {
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
            localizedName = "建造者"; // 原“戴森工程师”
        }

        @Override
        public void update(Unit unit) {
            // 检测附近黄矮星，激活其戴森云（共享开关，逻辑与原文一致）
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team != unit.team) continue;
                if (u.type instanceof YellowDwarfUnitType) {
                    if (Mathf.dst(unit.x, unit.y, u.x, u.y) <= detectRange) {
                        ((YellowDwarfUnitType) u.type).hasDysonSwarm = true;
                    }
                }
            }
            // ✅ 点击检测已移至全局 TapEvent，update 不再轮询输入
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

            // 右上角按钮
            float bx = x + BUILDER_BTN_OFFSET_X;
            float by = y + BUILDER_BTN_OFFSET_Y;
            boolean active = unit.hasEffect(StatusEffects.invincible); // ✅ 状态 = 真实效果

            Draw.z(200f);
            Draw.color(active ? Color.green : Color.gray);
            Fill.circle(bx, by, BUILDER_BTN_RADIUS);

            // ✅ 三角形图标：Fill.poly(float[], count)
            Draw.color(Color.white);
            Fill.poly(new float[]{
                bx - 5f, by - 5f,
                bx - 5f, by + 5f,
                bx + 6f, by
            }, 3);

            Draw.reset();
            Draw.z(0f);
        }
    }
}
