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
            Draw.color(outerColor, 0.3f + pulse * 0.15f);
            Fill.circle(x, y, radius * 1.8f);

            // ===== 4. 核心 =====
            Draw.color(coreColor);
            Fill.circle(x, y, radius * 0.75f);

            // ===== 5. 高光 =====
            Draw.color(Color.white, 0.85f);
            Fill.circle(x, y, radius * 0.3f);

            // ===== 6. 旋转节点 =====
            for (int i = 0; i < 6; i++) {
                float angle = time * (30f + i * 5f) + (i * 60f);
                float dist = radius * 0.55f;
                Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 45f, 8f, 0.5f));
                Fill.circle(x + Angles.trnsx(angle, dist), y + Angles.trnsy(angle, dist), 2f + pulse * 1.5f);
            }

            // ===== 7. 星火 =====
            Mathf.rand.setSeed(unit.id);
            for (int i = 0; i < 5; i++) {
                float sa = Mathf.rand.random(360f), sd = Mathf.rand.random(radius * 0.6f, radius * 2.5f);
                Draw.color(coreColor, Mathf.rand.random(0.15f, 0.45f));
                Fill.circle(x + Angles.trnsx(sa, sd), y + Angles.trnsy(sa, sd), Mathf.rand.random(1f, 2.5f));
            }
            Mathf.rand.setSeed(0);

            // ★ 关键：绘制完毕后再次重置，彻底隔离
            Draw.reset();
            Draw.z(0f);
        }

        /**
         * 动态流动粒子流
         * - 粒子沿喷流方向持续移动（流动的视觉效果）
         * - 使用 (time - dist/speed) 制造"波"在传播的感觉
         * - 每个粒子有独立的闪烁相位
         */
        private void drawFlowingJet(float x, float y, float angle, float length, float pulse, float time, int unitId, float phaseOffset) {
            // 沿喷流方向的采样点（固定间距，形成连续的流）
            float step = 4f; // 每 4 像素一个粒子团
            int count = (int)(length / step);
            count = Mathf.clamp(count, 20, 120);

            // 喷流整体亮度脉动（所有粒子同步闪烁）
            float jetBrightness = 0.6f + pulse * 0.4f;

            for (int i = 0; i < count; i++) {
                float t = (float) i / count;      // 0（核心）→ 1（末端）
                float dist = t * length;

                // 锥型扩散：越往外越宽
                float spread = t * 7f;

                // ★ 流动效果：相位随距离和时间变化
                // 越靠近末端，相位越滞后，形成"向外传播"的视觉效果
                float flowPhase = time * 8f - dist * 0.5f + phaseOffset;

                // 粒子在垂直喷流方向上有正弦摆动 → 蛇形流动感
                float wobble = Mathf.sin(flowPhase) * (2f + t * 4f);

                float perpAngle = angle + 90f;
                float alongX = Angles.trnsx(angle, dist);
                float alongY = Angles.trnsy(angle, dist);
                float perpX = Angles.trnsx(perpAngle, wobble);
                float perpY = Angles.trnsy(perpAngle, wobble);

                float px = x + alongX + perpX;
                float py = y + alongY + perpY;

                // ★ 每个粒子团有独立的闪烁（基于 flowPhase）
                float flicker = 0.5f + 0.5f * Mathf.sin(flowPhase * 1.3f + i * 0.7f);
                // 核心端持续亮，末端闪烁更明显
                float brightness = jetBrightness * (0.7f + 0.3f * flicker);
                brightness = brightness * (1f - t * 0.5f); // 末端整体变暗

                // 粒子大小：核心粗，末端细
                float pSize = (3.5f - t * 3f) + pulse * 0.5f;
                pSize = pSize > 0.6f ? pSize : 0.6f;

                // 颜色渐变：白 → 蓝 → 紫 → 暗
                Color c;
                if (t < 0.25f) {
                    c = Color.white.lerp(coreColor, t / 0.25f);
                } else if (t < 0.6f) {
                    c = coreColor.lerp(jetColor, (t - 0.25f) / 0.35f);
                } else {
                    c = jetColor.lerp(outerColor, (t - 0.6f) / 0.4f);
                }

                // 主粒子团
                Draw.color(c, brightness);
                Fill.circle(px, py, pSize);

                // 光晕（每隔几个粒子加一层更宽更透明的）
                if (i % 2 == 0) {
                    Draw.color(c, brightness * 0.25f);
                    Fill.circle(px, py, pSize * 2.5f);
                }

                // ★ 高速"子弹"粒子：偶尔有更亮更小的粒子从核心射向末端
                if (flicker > 0.85f && t > 0.15f) {
                    Draw.color(Color.white, (flicker - 0.85f) * 6f * (1f - t));
                    Fill.circle(px, py, pSize * 0.5f);
                }
            }
        }
    }
}
