package redteamai;

import arc.util.Log;
import mindustry.mod.Mod;

public class PulsarModMain extends Mod {

    @Override
    public void loadContent() {
        Log.info("[PulsarMod] 加载脉冲星单位...");
        new PulsarUnitType("pulsar-unit").load();
        Log.info("[PulsarMod] 脉冲星单位注册完成");
    }
}
