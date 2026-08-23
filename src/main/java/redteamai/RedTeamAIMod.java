private void replaceAI(Unit u) {
    // 已经是自定义AI就直接跳过
    if (u.controller() instanceof EnhancedGroundAI) return;
    
    try {
        EnhancedGroundAI ai = new EnhancedGroundAI();
        u.controller(ai);
        ai.unit(u);
        ai.init();
    } catch (Exception ex) {
        mindustry.Vars.log.err("[RedTeamAI] 替换失败: " + u.type.name + " - " + ex.getMessage());
    }
}
