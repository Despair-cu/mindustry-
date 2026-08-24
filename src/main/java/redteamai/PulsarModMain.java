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
        }

        @Override
        public void draw(Unit unit) {
            float x = unit.x, y = unit.y, time = Time.time;
            float pulse = Mathf.sin(time, pulseSpeed, 1f);
            float radius = baseRadius + pulse * 2f;

            float jetLength = radius * 6f + pulse * 4f;
            float jetAngle = unit.rotation + Mathf.sin(time, 60f, 8f);

            Draw.z(95f);
            Draw.color(outerColor, 0.6f + pulse * 0.2f);
            Lines.stroke(5f + pulse * 2f);
            Lines.lineAngle(x, y, jetAngle, jetLength);
            Draw.color(jetColor, 0.9f);
            Lines.stroke(2.5f + pulse);
            Lines.lineAngle(x, y, jetAngle, jetLength * 0.85f);
            Draw.color(Color.white, 0.7f + pulse * 0.3f);
            Lines.stroke(1f);
            Lines.lineAngle(x, y, jetAngle, jetLength * 0.6f);

            Draw.color(outerColor, 0.6f + pulse * 0.2f);
            Lines.stroke(5f + pulse * 2f);
            Lines.lineAngle(x, y, jetAngle + 180f, jetLength * 0.9f);
            Draw.color(jetColor, 0.9f);
            Lines.stroke(2.5f + pulse);
            Lines.lineAngle(x, y, jetAngle + 180f, jetLength * 0.75f);
            Draw.color(Color.white, 0.7f + pulse * 0.3f);
            Lines.stroke(1f);
            Lines.lineAngle(x, y, jetAngle + 180f, jetLength * 0.5f);

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

            Mathf.rand.setSeed(unit.id);
            for (int i = 0; i < 5; i++) {
                float sa = Mathf.rand.random(360f), sd = Mathf.rand.random(radius * 0.6f, radius * 2.5f);
                Draw.color(coreColor, Mathf.rand.random(0.15f, 0.45f));
                Fill.circle(x + Angles.trnsx(sa, sd), y + Angles.trnsy(sa, sd), Mathf.rand.random(1f, 2.5f));
            }
            Mathf.rand.setSeed(0);
            Draw.reset();
        }
    }
}
