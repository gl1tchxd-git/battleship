# Battleship P2P Networking

Simple and straightforward P2P networking implementation for the Battleship game.

## Overview

This networking solution provides:
- **Simple P2P TCP connections** between two players
- **Enum-based packet types** for type-safe message handling
- **Lightweight data packets** - only sends essential information, not full Ship objects
- **Easy integration** with the existing GameController

## Files

- **PacketType.java** - Enum defining all message types (CONNECT, ATTACK, ATTACK_RESULT, etc.)
- **GamePacket.java** - Lightweight serializable data packet containing only essential info
- **NetworkListener.java** - Interface for handling network events
- **NetworkManager.java** - Low-level TCP socket management
- **NetworkGameController.java** - High-level bridge between NetworkManager and GameController
- **NetworkUsageExample.java** - Complete usage examples

## Quick Start

### Hosting a Game

```java
GameController gameController = new GameController();
NetworkGameController networkController = new NetworkGameController(gameController);

// Override event handlers
networkController = new NetworkGameController(gameController) {
    @Override
    protected void onOpponentConnected(String opponentId) {
        // Initialize game and send config
        gameController.initializeGame(10, new int[]{1, 2, 1, 1, 1});
        sendGameInit(10, new int[]{1, 2, 1, 1, 1});
    }
    
    @Override
    protected void onAttackReceived(int row, int col, GameController.AttackResult result) {
        // Handle opponent's attack on your board
        updateUI();
    }
};

networkController.hostGame(8888);
```

### Joining a Game

```java
GameController gameController = new GameController();
NetworkGameController networkController = new NetworkGameController(gameController);

networkController.joinGame("192.168.1.100", 8888);
```

### Making an Attack

```java
if (gameController.isMyTurn()) {
    networkController.sendAttack(row, col);
}
```

### Placement Complete

```java
// After placing all ships
gameController.confirmPlacement();
networkController.sendPlacementReady();
```

## Packet Types

The `PacketType` enum defines all message types:

- **Connection**: `CONNECT_REQUEST`, `CONNECT_ACCEPT`, `DISCONNECT`
- **Setup**: `GAME_INIT`
- **Placement**: `PLACEMENT_READY`
- **Battle**: `ATTACK`, `ATTACK_RESULT`
- **Game State**: `TURN_CHANGE`, `GAME_OVER`
- **Misc**: `SYNC_REQUEST`, `SYNC_RESPONSE`, `CHAT_MESSAGE`

## Data Efficiency

The networking code is designed to be lightweight:

- **No Ship objects** are transmitted - only ship length when a ship is sunk
- **Attack packets** only contain row and column coordinates
- **Attack result packets** contain: hit (boolean), shipSunk (boolean), gameWon (boolean), shipLength (int)
- **Board state** is represented as a simple 2D int array when needed

## Event Handlers

Override these methods in `NetworkGameController` to handle events:

```java
protected void onOpponentConnected(String opponentId)
protected void onOpponentDisconnected(String reason)
protected void onNetworkError(String error)
protected void onGameInitReceived(int boardSize, int[] shipConfig)
protected void onOpponentReady()
protected void onAttackReceived(int row, int col, GameController.AttackResult result)
protected void onAttackResultReceived(boolean hit, boolean shipSunk, boolean gameWon, int shipLength)
protected void onTurnChanged(String newTurnPlayerId)
protected void onGameOverReceived(String message)
protected void onChatMessageReceived(String senderId, String message)
```

## Game Flow

1. **Connection**
   - One player hosts with `hostGame(port)`
   - Other player joins with `joinGame(host, port)`

2. **Initialization**
   - Host sends `GAME_INIT` packet with board size and ship config
   - Both players initialize their game with same settings

3. **Placement Phase**
   - Each player places ships locally using `GameController`
   - When done, send `PLACEMENT_READY` packet
   - When both ready, battle begins

4. **Battle Phase**
   - Players take turns attacking
   - Attacker sends `ATTACK` packet with coordinates
   - Defender processes attack and sends `ATTACK_RESULT` back
   - Turn management handled through `TURN_CHANGE` packets

5. **Game End**
   - When all ships sunk, `GAME_OVER` packet sent
   - Connection can be maintained or closed

## Thread Safety

- NetworkManager uses separate threads for sending and receiving
- Send operations are queued and processed asynchronously
- All callbacks are executed on the receive thread

## Error Handling

Network errors trigger the `onError()` callback. Connection losses trigger `onDisconnected()`.
Both events will automatically clean up the connection.

## Example Integration

See `NetworkUsageExample.java` for complete working examples including:
- Hosting a game
- Joining a game
- Handling all network events
- Making attacks
- Managing game flow

## Technical Details

- Uses Java TCP sockets for reliable connection
- Object serialization for packet transmission
- Blocking queues for thread-safe message sending
- Automatic reconnection not implemented (intentionally simple)
- No encryption (add SSL/TLS wrapper if needed)

## Design Philosophy

This implementation prioritizes:
- **Simplicity** - Easy to understand and modify
- **Efficiency** - Minimal data transmission
- **Type Safety** - Enums instead of string constants
- **Separation** - Network logic separate from game logic
- **Flexibility** - Easy to extend with new packet types

