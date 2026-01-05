package at.gl1tchxd.battleship.screens;

import at.gl1tchxd.battleship.BattleshipGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * End game screen displayed when player wins or loses.
 * Shows appropriate victory/defeat image and Continue button.
 */
public class EndScreen implements Screen {
    private final BattleshipGame game;
    private final boolean won;

    private Stage stage;
    private SpriteBatch batch;
    private Texture backgroundImage;
    private ImageTextButton continueButton;

    private static final float TRANSITION_DELAY = 1.0f; // 1 second delay
    private float timeElapsed = 0f;

    public EndScreen(BattleshipGame game, boolean won) {
        this.game = game;
        this.won = won;
    }

    @Override
    public void show() {
        // Play appropriate sound based on win/loss
        if (won) {
            game.playGameWonSound();
        } else {
            game.playGameLostSound();
        }

        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();

        // Load appropriate background image
        String imagePath = won ? "sprites/end_game-won.png" : "sprites/end_game-lost.png";
        backgroundImage = new Texture(Gdx.files.internal(imagePath));

        Gdx.input.setInputProcessor(stage);

        // Create Continue button (styled like START button)
        continueButton = new ImageTextButton("Continue", game.getSkin());
        continueButton.getLabel().setFontScale(2f); // Same scale as START button

        // Position button in center, lower half (same positioning as START button)
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        continueButton.setPosition(
            screenWidth / 2f - continueButton.getWidth() / 2f,
            screenHeight / 2f - 200 // Same offset as START button
        );

        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClickSound();
                returnToMainMenu();
            }
        });

        // Initially hide the button
        continueButton.setVisible(false);

        stage.addActor(continueButton);
    }

    private void returnToMainMenu() {
        // Clean up network connections
        if (game.getNetworkController() != null) {
            if (game.getNetworkController().isHost()) {
                game.getNetworkController().stopHosting();
            } else {
                game.getNetworkController().stopJoining();
            }
        }

        // Return to main menu
        game.setScreen(new MainMenuScreen(game));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Track time elapsed
        timeElapsed += delta;

        // Show button after delay
        if (timeElapsed >= TRANSITION_DELAY && !continueButton.isVisible()) {
            continueButton.setVisible(true);
        }

        // Draw background image
        batch.begin();
        batch.draw(backgroundImage, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // Draw UI (button will only be visible after delay)
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        // Reposition button (same as START button positioning)
        if (continueButton != null) {
            continueButton.setPosition(
                width / 2f - continueButton.getWidth() / 2f,
                height / 2f - 200
            );
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (batch != null) batch.dispose();
        if (backgroundImage != null) backgroundImage.dispose();
    }
}

