package com.example.pulsarmod;

import mindustry.mod.Mod;
import mindustry.game.EventType;
import arc.util.Log;

public class PulsarModMain extends Mod {
    public PulsarModMain() {
        Log.info("PulsarMod initialized.");
    }

    @Override
    public void loadContent() {
        // 注册我们的脉冲星单位
        new PulsarUnitType("pulsar-unit").load();
        Log.info("PulsarUnit registered.");
    }
}
