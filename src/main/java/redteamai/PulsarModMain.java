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
            Draw.reset(); // ✅ 开头重置，防止污染UI

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
            Draw.reset(); // ✅ 结尾重置
        }
    }

    // ==================== 蓝紫脉冲星 ====================
    public static class BluePulsarUnitType extends UnitType {
        public Color coreColor = Color.valueOf("5b6cff");
        public Color outerColor = Color.valueOf("9d4dff");
        public Color jetColor = Color.valueOf("b388ff");
        public float pulseSpeed = 35f;
        public float baseRadius = 10f;

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
            Draw.reset(); // ✅ 关键：防止污染游戏界面

            float x = unit.x, y = unit.y, time = Time.time;
            float pulse = Mathf.sin(time, pulseSpeed, 1f);
            float radius = baseRadius + pulse * 2f;

            // 喷流长度（更长！）
            float jetLength = radius * 20f + pulse * 8f;
            float jetAngle = unit.rotation + Mathf.sin(time, 60f, 8f);

            Draw.z(95f);

            // 两极动态粒子流
            drawJet(x, y, jetAngle, jetLength, pulse, unit.id);
            drawJet(x, y, jetAngle + 180f, jetLength * 0.9f, pulse, unit.id + 1);

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

            // 星火
            Mathf.rand.setSeed(unit.id);
            for (int i = 0; i < 5; i++) {
                float sa = Mathf.rand.random(360f), sd = Mathf.rand.random(radius * 0.6f, radius * 2.5f);
                Draw.color(coreColor, Mathf.rand.random(0.15f, 0.45f));
                Fill.circle(x + Angles.trnsx(sa, sd), y + Angles.trnsy(sa, sd), Mathf.rand.random(1f, 2.5f));
            }
            Mathf.rand.setSeed(0);
            Draw.reset(); // ✅ 关键：必须重置，否则UI会被染色
        }

        // 动态粒子流
        private void drawJet(float x, float y, float angle, float length, float pulse, long seed) {
            Mathf.rand.setSeed(seed);

            // 粒子密度随长度增加：每单位距离一个粒子
            int particleCount = (int)(length / 3f);
            particleCount = Mathf.clamp(particleCount, 15, 80); // 限制上限，避免卡顿

            for (int i = 0; i < particleCount; i++) {
                float t = (float) i / particleCount; // 0（核心端）→ 1（末端）
                float dist = t * length;

                // 越往外扩散越大（锥形喷流）
                float spread = t * 6f;
                float offsetAngle = Mathf.rand.random(-spread, spread);
                // 添加螺旋效果：粒子沿喷流有轻微的旋转
                float spiral = Mathf.sin(t * 12f + pulse * 2f) * t * 3f;
                float finalAngle = angle + offsetAngle + spiral;

                float px = x + Angles.trnsx(finalAngle, dist);
                float py = y + Angles.trnsy(finalAngle, dist);

                // 粒子大小：核心端粗，末端细
                float pSize = (4f - t * 3.5f) + pulse * 0.5f;
                pSize = pSize > 0.5f ? pSize : 0.5f;

                // 颜色渐变：核心白 → 蓝 → 紫 → 暗紫（末端）
                Color particleColor;
                if (t < 0.3f) {
                    particleColor = Color.white.lerp(coreColor, t / 0.3f);
                } else if (t < 0.7f) {
                    particleColor = coreColor.lerp(jetColor, (t - 0.3f) / 0.4f);
                } else {
                    particleColor = jetColor.lerp(outerColor, (t - 0.7f) / 0.3f);
                }

                // 透明度：核心端最亮，末端渐隐
                float alpha = (1f - t * 0.8f) * (0.85f + pulse * 0.15f);

                Draw.color(particleColor, alpha);
                Fill.circle(px, py, pSize);

                // 部分粒子加一层"光晕"（更大更透明）
                if (i % 3 == 0) {
                    Draw.color(particleColor, alpha * 0.3f);
                    Fill.circle(px, py, pSize * 2.2f);
                }
            }

            Mathf.rand.setSeed(0);
        }
    }
}
