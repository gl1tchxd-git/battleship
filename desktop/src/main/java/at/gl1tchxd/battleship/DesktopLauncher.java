package at.gl1tchxd.battleship;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Battleship");
        config.setWindowedMode(1280, 720);
        config.setResizable(false);
        new Lwjgl3Application(new BattleshipGame(), config);
    }
}