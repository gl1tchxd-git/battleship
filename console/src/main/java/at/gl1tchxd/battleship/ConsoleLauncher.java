package at.gl1tchxd.battleship;

import at.gl1tchxd.battleship.logic.GameController;

import java.util.Arrays;

public class ConsoleLauncher {
    public static void main(String[] args) {
        GameController gameController = new GameController();
        gameController.initializeGame(10, new int[]{1, 1, 1, 1, 1});
        System.out.println(gameController.placeShip(0, 1, 1, true));
        System.out.println(gameController.placeShip(1, 0, 2, false));
        System.out.println(gameController);
        System.out.println(gameController.attackWithResult(1, 1).isShipSunk());
        System.out.println(gameController.attackWithResult(1, 2).isShipSunk());
        System.out.println(gameController.attackWithResult(1, 3).isShipSunk());
        System.out.println(gameController.getRemainingShips());
        System.out.println(gameController.attackWithResult(1, 4).isShipSunk());
        System.out.println(gameController);
        System.out.println(gameController.getRemainingShips());
        gameController.getTrackingBoard().markCell(1, 1, false);
        System.out.println(gameController.getTrackingBoard());

        System.out.println(Arrays.toString(gameController.exportSunk()));
        gameController.resetGame();
        gameController.initializeGame(10, new int[]{1, 0, 0, 0, 1});
        gameController.placeShip(0, 1, 1, true);
        gameController.placeShip(0, 2, 1, true);
        gameController.placeShip(1, 5, 1, false);
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 10; c++) {
                gameController.attackWithResult(r, c);
            }
        }
        System.out.println(gameController);
        System.out.println(gameController.getRemainingShips());
        System.out.println(Arrays.deepToString(gameController.exportSunk()));
        System.out.println(gameController.isGameOver());
    }
}
