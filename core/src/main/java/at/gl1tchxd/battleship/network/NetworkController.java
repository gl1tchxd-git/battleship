package at.gl1tchxd.battleship.network;

import at.gl1tchxd.battleship.logic.GameController;

public class NetworkController {
    GameController gameController;
    private boolean isHost;
    private Socket socket;

    public NetworkController(GameController gameController) {
        this.gameController = gameController;
    }

    public void hostGame() {
        this.isHost = true;
    }


}
