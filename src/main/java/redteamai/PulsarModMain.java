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
            float x = unit.x, y = unit.y, time = Time.time;
            float pulse = Mathf.sin(time, pulseSpeed, 1f);
            float radius = baseRadius + pulse * 2f;

            // 喷流长度（加长了！之前是 radius*6，现在 radius*12）
            float jetLength = radius * 12f + pulse * 6f;
            float jetAngle = unit.rotation + Mathf.sin(time, 60f, 8f);

            Draw.z(95f);

            // ===== 两极粒子喷射 =====
            drawJet(x, y, jetAngle, jetLength, pulse, time, unit.id);
            drawJet(x, y, jetAngle + 180f, jetLength * 0.9f, pulse, time, unit.id + 1);

            // ===== 波纹 =====
            Draw.z(100f);
            float waveProgress = (time % 35f) / 35f;
            Draw.color(coreColor, (1f - waveProgress) * 0.5f);
            Lines.stroke(2f + pulse * 1.5f);
            Lines.circle(x, y, waveProgress * baseRadius * 4f);

            // ===== 外发光 =====
            Draw.z(110f);
            Draw.color(outerColor, 0.3f + pulse * 0.15f);
            Fill.circle(x, y, radius * 1.8f);

            // ===== 核心 =====
            Draw.color(coreColor);
            Fill.circle(x, y, radius * 0.75f);

            // ===== 高光 =====
            Draw.color(Color.white, 0.85f);
            Fill.circle(x, y, radius * 0.3f);

            // ===== 旋转节点 =====
            for (int i = 0; i < 6; i++) {
                float angle = time * (30f + i * 5f) + (i * 60f);
                float dist = radius * 0.55f;
                Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 45f, 8f, 0.5f));
                Fill.circle(x + Angles.trnsx(angle, dist), y + Angles.trnsy(angle, dist), 2f + pulse * 1.5f);
            }

            // ===== 星火 =====
            Mathf.rand.setSeed(unit.id);
            for (int i = 0; i < 5; i++) {
                float sa = Mathf.rand.random(360f), sd = Mathf.rand.random(radius * 0.6f, radius * 2.5f);
                Draw.color(coreColor, Mathf.rand.random(0.15f, 0.45f));
                Fill.circle(x + Angles.trnsx(sa, sd), y + Angles.trnsy(sa, sd), Mathf.rand.random(1f, 2.5f));
            }
            Mathf.rand.setSeed(0);
            Draw.reset();
        }

        // 粒子喷射方法
        private void drawJet(float x, float y, float angle, float length, float pulse, float time, long seed) {
            Mathf.rand.setSeed(seed);

            int particleCount = 20; // 每根喷流的粒子数

            for (int i = 0; i < particleCount; i++) {
                float t = (float) i / particleCount; // 0~1，沿喷流方向
                float dist = t * length;

                // 粒子沿喷流方向有随机偏移（越往外越散开）
                float spread = t * 4f; // 扩散程度
                float offsetAngle = Mathf.rand.random(-spread, spread);
                float finalAngle = angle + offsetAngle;

                float px = x + Angles.trnsx(finalAngle, dist);
                float py = y + Angles.trnsy(finalAngle, dist);

                // 粒子大小：靠近核心大，越往外越小
                float pSize = (3f - t * 2.5f) + pulse * 0.5f;
                pSize = Mathf.max(pSize, 0.5f);

                // 颜色：靠近核心偏白，越往外越紫
                float colorMix = t;
                Color particleColor = Color.white.lerp(jetColor, colorMix * 0.7f);

                // 透明度：靠近核心不透明，尾部渐隐
                float alpha = (1f - t) * (0.8f + pulse * 0.2f);

                Draw.color(particleColor, alpha);
                Fill.circle(px, py, pSize);
            }

            Mathf.rand.setSeed(0);
        }
    }
}
