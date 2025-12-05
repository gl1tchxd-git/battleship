Here are the existing methods in `GameController` needed for networking:

## Connection/Setup
- `setPlayerId(String playerId)`
- `getPlayerId()`
- `setOpponentId(String opponentId)`
- `getOpponentId()`
- `setOpponentReady(boolean ready)`
- `isOpponentReady()`
- `confirmPlacement()`
- `isPlacementComplete()`

## Turn Management
- `isMyTurn()`
- `setCurrentTurn(String playerId)`
- `switchTurn()`

## Attack Handling
- `receiveAttack(int row, int col)` → returns `AttackResult`
- `recordAttackResult(int row, int col, boolean hit)`
- `attackWithResult(int row, int col)` → alias for `receiveAttack`

## Game State
- `getGamePhase()`
- `setGamePhase(GamePhase phase)`
- `isGameOver()`
- `getRemainingShips()`
- `resetGame()`

## Data Export
- `exportBoardState()` → returns `int[][]`
- `exportFleet()` → returns `Ship[]`

## Initialization
- `initializeGame(int boardSize, int[] shipConfig)`