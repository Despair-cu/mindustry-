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
import mindustry.Vars;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.mod.Mod;
import mindustry.type.UnitType;
import mindustry.game.Team;
import mindustry.game.TeamData;

public class PulsarModMain extends Mod {

    // 调试开关：true=打印伤害日志 + 画判定线，调试完改 false
    public static boolean DEBUG = true;

    @Override
    public void loadContent() {
        Log.info("[PulsarMod] 加载恒星单位...");
        new YellowDwarfUnitType("yellow-dwarf").load();
        new BluePulsarUnitType("blue-pulsar").load();
        Log.info("[PulsarMod] 所有单位注册完成");
    }

    // ==================== 黄矮星（变大版） ====================
    public static class YellowDwarfUnitType extends UnitType {
        public Color coreColor = Color.valueOf("ffd37f");
        public Color outerColor = Color.valueOf("ff9d00");
        public float pulseSpeed = 40f;
        public float baseRadius = 22f;

        public YellowDwarfUnitType(String name) {
            super(name);
            health = 450;
            speed = 1.4f;
            rotateSpeed = 8f;
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

    // ==================== 中子星（亮蓝伤害喷流 + 日志） ====================
    public static class BluePulsarUnitType extends UnitType {
        public Color coreColor = Color.valueOf("5b6cff");
        public Color outerColor = Color.valueOf("9d4dff");
        public Color jetColor = Color.valueOf("00e5ff");
        public float pulseSpeed = 35f;
        public float baseRadius = 10f;

        // 伤害参数（可调）
        public float dps = 60f;       // 每秒伤害
        public float jetLengthMul = 25f; // 喷流长度倍数

        public BluePulsarUnitType(String name) {
            super(name);
            health = 500;
            speed = 1.2f;
            rotateSpeed = 12f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            localizedName = "中子星";
        }

        @Override
        public void update(Unit unit) {
            float length = unit.hitSize * jetLengthMul;
            // 每帧伤害 = DPS * deltaTime，保证帧率无关
            float damage = dps * Time.delta;
            float jetAngle = unit.rotation + Mathf.sin(Time.time, 60f, 8f);

            if (DEBUG) {
                Log.info("[PulsarMod] update: id=" + unit.id + " len=" + length + " dmg=" + damage);
            }

            for (int sign : new int[]{1, -1}) {
                float ex = unit.x + Angles.trnsx(jetAngle, length * sign);
                float ey = unit.y + Angles.trnsy(jetAngle, length * sign);
                int hit = applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, unit.hitSize * 0.6f, damage);
                if (DEBUG && hit > 0) {
                    Log.info("[PulsarMod] 喷流命中 " + hit + " 个单位 @ (" + (int) ex + "," + (int) ey + ")");
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

            // 调试：画判定线（红色），确认伤害范围和视觉一致
            if (DEBUG) {
                Draw.z(29f);
                Draw.color(Color.red, 0.5f);
                Lines.stroke(1f);
                for (int sign : new int[]{1, -1}) {
                    float ex = x + Angles.trnsx(jetAngle, jetLength * sign);
                    float ey = y + Angles.trnsy(jetAngle, jetLength * sign);
                    Lines.line(x, y, ex, ey);
                }
            }

            // 流动喷流
            drawFlowingJet(x, y, jetAngle, jetLength, unit.id);
            drawFlowingJet(x, y, jetAngle + 180f, jetLength * 0.9f, unit.id + 1);

            // 波纹
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

        // 亮蓝、极细、无呼吸、流动喷流
        private void drawFlowingJet(float x, float y, float angle, float length, long seed) {
            Mathf.rand.setSeed(seed);
            float step = 3.5f;
            for (float d = 5f; d < length; d += step) {
                float t = d / length;
                float flowPhase = Time.time / 10f - d * 0.2f;
                float wobble = Mathf.sin(flowPhase) * t * 2f;
                float finalAngle = angle + wobble;

                float px = x + Angles.trnsx(finalAngle, d);
                float py = y + Angles.trnsy(finalAngle, d);

                float pSize = 1.5f - t * 0.5f;
                pSize = pSize > 0.3f ? pSize : 0.3f;

                float flicker = (Mathf.sin(flowPhase * 1.5f) + 1f) / 2f;
                float alpha = (1f - t) * (0.6f + flicker * 0.4f);

                Draw.color(jetColor, alpha);
                Fill.circle(px, py, pSize);
            }
            Mathf.rand.setSeed(0);
        }
    }

    // ===== 伤害辅助 =====
    private static int applyDamageAlongLine(Unit source, float x1, float y1, float x2, float y2, float width, float damage) {
        int hit = 0;
        Team sourceTeam = source.team;
        for (TeamData data : Vars.state.teams.data) {
            if (data.team == sourceTeam) continue;
            for (Unit u : data.units) {
                if (u == null || u.dead || u.isFlying()) continue;
                if (distanceToSegment(u.x, u.y, x1, y1, x2, y2) <= width + u.hitSize) {
                    u.damage(damage);
                    hit++;
                    if (DEBUG && hit <= 3) { // 每帧最多打3条，避免刷屏
                        Log.info("[PulsarMod] 命中 " + u.type + " HP剩余=" + (u.health - damage));
                    }
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
