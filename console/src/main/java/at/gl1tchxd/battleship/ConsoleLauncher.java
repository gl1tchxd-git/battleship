package at.gl1tchxd.battleship;

import at.gl1tchxd.battleship.logic.GameController;

public class ConsoleLauncher {
    public static void main(String[] args) {
        GameController gameController = new GameController();
        gameController.initializeGame(10, new int[]{1, 1, 1, 1, 1});
        System.out.println(gameController.placeShip(1, 1, 1, true));
        System.out.println(gameController.getGame().getBoard().toString(true));
        System.out.println(gameController.attack(1, 2));
    }
}
