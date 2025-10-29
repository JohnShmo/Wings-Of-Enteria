package johnshmo.woe;

import com.fs.starfarer.api.BaseModPlugin;

public class ModPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() throws Exception {
        super.onApplicationLoad();
    }

    @Override
    public void onNewGame() {
        super.onNewGame();
        WOEGlobal.initialize();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);
        if (!newGame) {
            WOEGlobal.initialize();
        }
    }
}
