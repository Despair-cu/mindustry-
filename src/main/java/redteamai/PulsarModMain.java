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

    // ==================== 黄矮星 ====================
    public static class YellowDwarfUnitType extends UnitType {
        public Color coreColor = Color.valueOf("ffd37f");
        public Color outerColor = Color.valueOf("ff9d00");
        public float pulseSpeed = 40f;
        public float baseRadius = 12f;

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
        }
    }

    // ==================== 蓝紫脉冲星 ====================
    public static class BluePulsarUnitType extends UnitType {
        public Color coreColor = Color.valueOf("5b6cff");
        public Color outerColor = Color.valueOf("9d4dff");
        public Color jetColor = Color.valueOf("b388ff");
        public float pulseSpeed = 35f;
        public float baseRadius = 10f;

        // 每个单位实例独立的粒子流状态
        // key = unit.id, value = 该喷流的相位偏移
        private static final arc.struct.IntMap<Float> jetPhase = new arc.struct.IntMap<>();

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
        public void draw(Unit unit) {
            // ★ 关键：绘制前强制重置所有绘图状态，避免污染 UI
            Draw.reset();
            Draw.z(0f);

            float x = unit.x, y = unit.y, time = Time.time;
            float pulse = Mathf.sin(time, pulseSpeed, 1f);
            float radius = baseRadius + pulse * 2f;

            // 喷流方向（缓慢进动）
            float jetAngle = unit.rotation + Mathf.sin(time, 60f, 8f);

            // ===== 1. 先画喷流（在最底层，z 最低）=====
            Draw.z(90f); // 低于普通单位，这样喷流不会遮挡 UI
            float jetLength = radius * 18f + pulse * 6f;
            drawFlowingJet(x, y, jetAngle, jetLength, pulse, time, unit.id, 0f);
            drawFlowingJet(x, y, jetAngle + 180f, jetLength * 0.9f, pulse, time, unit.id, 180f);

            // ===== 2. 波纹 =====
            Draw.z(100f);
            float waveProgress = (time % 35f) / 35f;
            Draw.color(coreColor, (1f - waveProgress) * 0.5f);
            Lines.stroke(2f + pulse * 1.5f);
            Lines.circle(x, y, waveProgress * baseRadius * 4f);

            // ===== 3. 外发光 =====
            Draw.z(110f);
            Draw.color(outerColor, 0.