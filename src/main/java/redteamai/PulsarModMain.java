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
import mindustry.entities.Damage;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.mod.Mod;
import mindustry.type.UnitType;

public class PulsarModMain extends Mod {

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
        public float baseRadius = 22f; // ✅ 从12→22，整体变大

        public YellowDwarfUnitType(String name) {
            super(name);
            health = 450;
            speed = 1.4f;
            rotateSpeed = 8f;
            hitSize = baseRadius * 2f; // ✅ 碰撞框也随之变大
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

            // 波纹
            float waveProgress = (time % 40f) / 40f;
            Draw.z(100f);
            Draw.color(coreColor, (1f - waveProgress) * 0.4f);
            Lines.stroke(2f + pulse);
            Lines.circle(x, y, waveProgress * baseRadius * 3.5f);

            // 外发光
            Draw.z(110f);
            Draw.color(outerColor, 0.25f + pulse * 0.1f);
            Fill.circle(x, y, radius * 1.6f);

            // 核心
            Draw.color(coreColor);
            Fill.circle(x, y, radius * 0.7f);

            // 高光
            Draw.color(Color.white, 0.8f);
            Fill.circle(x, y, radius * 0.35f);

            // 旋转节点
            for (int i = 0; i < 3; i++) {
                float angle = time * (25f + i * 10f) + (i * 120f);
                float dist = radius * 0.5f;
                Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 60f, 10f, 0.5f));
                Fill.circle(x + Angles.trnsx(angle, dist), y + Angles.trnsy(angle, dist), 2.5f + pulse * 1.2f);
            }

            // 星火
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

    // ==================== 蓝紫/中子星（亮蓝伤害喷流） ====================
    public static class BluePulsarUnitType extends UnitType {
        public Color coreColor = Color.valueOf("5b6cff");
        public Color outerColor = Color.valueOf("9d4dff");
        public Color jetColor = Color.valueOf("00e5ff"); // 亮蓝色
        public float pulseSpeed = 35f;
        public float baseRadius = 10f;

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
            // 喷流伤害
            float length = unit.hitSize * 25f;
            float damage = 15f;
            float jetAngle = unit.rotation + Mathf.sin(Time.time, 60f, 8f);
            for (int sign : new int[]{1, -1}) {
                float ex = unit.x + Angles.trnsx(jetAngle, length * sign);
                float ey = unit.y + Angles.trnsy(jetAngle, length * sign);
                Damage.applyDamage(unit.x, unit.y, ex, ey, length, damage, unit.team, false, null);
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            Draw.z(30f); // ✅ 低于UI层级，防止染色

            float x = unit.x, y = unit.y, time = Time.time;
            float pulse = Mathf.sin(time, pulseSpeed, 1f);
            float radius = baseRadius + pulse * 2f;

            float jetLength = radius * 25f; // ✅ 极长
            float jetAngle = unit.rotation + Mathf.sin(time, 60f, 8f);

            // 两极亮蓝动态粒子喷流
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
            Draw.z(0f); // ✅ 归零，保护UI
        }

        // 亮蓝色、极细、无呼吸、流动粒子喷流
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

                // 极细，无呼吸
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
}
