package at.gl1tchxd.battleship;

import at.gl1tchxd.battleship.logic.GameController;
import at.gl1tchxd.battleship.network.NetworkController;
import at.gl1tchxd.battleship.screens.MainMenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class BattleshipGame extends Game {
    private SpriteBatch batch;
    private ScreenViewport viewport;
    private Skin skin;
    private GameController gameController;
    private NetworkController networkController;

    @Override
    public void create() {
        this.batch = new SpriteBatch(); // Add this line
        this.viewport = new ScreenViewport();
        this.setScreen(new MainMenuScreen(this));
        this.skin = new Skin();
        createDefaultSkin();
        this.gameController = new GameController();
        this.networkController = new NetworkController(gameController);
    }

    @Override
    public void render() {
        // Clear the screen with dark blue color
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        super.render(); // Important: this calls the active screen's render method
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
    }

    public GameController getGameController() {
        return gameController;
    }

    public NetworkController getNetworkController() {
        return networkController;
    }

    public Skin getSkin() {
        return skin;
    }

    private void createDefaultSkin() {
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        // Create a white 1x1 pixel texture for drawables
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new com.badlogic.gdx.graphics.Texture(pixmap));
        pixmap.dispose();

        // Label style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // TextField style
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.8f, 1f));
        textFieldStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.8f));
        skin.add("default", textFieldStyle);

        // TextButton style
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.up = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.3f, 0.9f));
        buttonStyle.down = skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 0.9f));
        buttonStyle.over = skin.newDrawable("white", new Color(0.4f, 0.4f, 0.4f, 0.9f));
        skin.add("default", buttonStyle);
    }
}