# Battleship P2P Networking

This package provides peer-to-peer TCP networking for the Battleship game.

## Architecture

The networking layer uses a **P2P (peer-to-peer)** architecture where one player acts as the **host** (server) and the other connects as a **client**. Once connected, both communicate directly via TCP sockets.

```
Player A (Host)  <---TCP Socket--->  Player B (Client)
```

## Components

### 1. **NetworkManager**
Main class that handles TCP connections and message sending.

**Key Methods:**
- `startHost(int port)` - Start hosting and wait for opponent
- `connectToHost(String ip, int port)` - Connect to a host
- `sendMessage(NetworkMessage msg)` - Send a message
- `sendAttack(int row, int col)` - Send attack to opponent
- `sendReady()` - Signal that ship placement is complete
- `disconnect()` - Close connection

### 2. **NetworkMessage**
Defines the message protocol. Messages are text-based, format: `TYPE:param1:param2:...`

**Message Types:**
- `CONNECT:playerId` - Initial connection handshake
- `READY:playerId` - Player finished ship placement
- `ATTACK:row:col` - Attack coordinate
- `RESULT:hit:sunk:gameOver:shipName` - Attack result
- `TURN:playerId` - Turn update
- `GAME_OVER:winnerId` - Game finished
- `DISCONNECT:playerId` - Player disconnected
- `RESET` - Reset game

### 3. **MessageHandler**
Parses incoming messages and calls appropriate `GameController` methods.

### 4. **NetworkListener**
Background thread that continuously listens for incoming messages.

## How It Works

### Connection Flow

1. **Host starts:**
   ```java
   NetworkManager network = new NetworkManager(gameController);
   network.startHost(12345); // Waits for connection
   ```

2. **Client connects:**
   ```java
   NetworkManager network = new NetworkManager(gameController);
   network.connectToHost("192.168.1.100", 12345);
   ```

3. Both exchange `CONNECT` messages with player IDs

### Game Flow

1. **Setup Phase:**
   - Both players place ships on their boards
   - When done, call `network.sendReady()`
   - When both ready, `MessageHandler` updates game phase to `BATTLE`

2. **Battle Phase:**
   - Host decides who goes first using `gameController.setCurrentTurn(playerId)`
   - On your turn:
     ```java
     if (gameController.isMyTurn()) {
         network.sendAttack(row, col);
     }
     ```
   - Opponent receives attack → `MessageHandler.handleAttack()` called
   - Opponent's `GameController.receiveAttack()` processes the hit
   - Result sent back: `RESULT:true:false:false:Destroyer`
   - Attacker's `MessageHandler.handleAttackResult()` called
   - UI calls `gameController.recordAttackResult(row, col, hit)`
   - Turn switches

3. **Game Over:**
   - When a player loses all ships, `GAME_OVER` message sent
   - Game phase updates to `GAME_OVER`

## Usage Example

### Basic Setup

```java
// Initialize game
GameController controller = new GameController();
controller.setPlayerId("Alice");
controller.initializeGame(10, new int[]{5, 4, 3, 3, 2});

// Setup networking
NetworkManager network = new NetworkManager(controller);

// Host or join
network.startHost(12345);  // OR
network.connectToHost("192.168.1.100", 12345);
```

### Ship Placement

```java
// Place your ships
controller.placeShip(0, 0, 0, true);  // Carrier at (0,0) horizontal
controller.placeShip(1, 2, 0, false); // Battleship at (2,0) vertical
// ... place remaining ships

// Signal ready
network.sendReady();
```

### Attack

```java
// When it's your turn
if (controller.isMyTurn()) {
    network.sendAttack(5, 7);
    
    // Later, when result arrives via MessageHandler,
    // your UI should call:
    controller.recordAttackResult(5, 7, wasHit);
}
```

### Complete Integration Example

See `NetworkExample.java` for a full working example with the `NetworkGameSession` wrapper class.

## Integration with UI

Your UI (console/desktop) should:

1. **Show connection screen:**
   - Button: "Host Game" → calls `network.startHost(port)`
   - Button: "Join Game" → shows IP input, calls `network.connectToHost(ip, port)`

2. **Ship placement:**
   - Let player place ships on board
   - Button: "Ready" → calls `network.sendReady()`
   - Show "Waiting for opponent..." until both ready

3. **Battle:**
   - Display whose turn it is: `controller.isMyTurn()`
   - On click enemy cell:
     ```java
     if (controller.isMyTurn()) {
         network.sendAttack(row, col);
         // Show "attacking..." indicator
     }
     ```
   - When `MessageHandler.handleAttackResult()` fires (via callback/listener):
     ```java
     controller.recordAttackResult(row, col, hit);
     // Update tracking board display
     ```

4. **Game Over:**
   - Listen for `GamePhase.GAME_OVER`
   - Show winner/loser screen

## Testing Locally

Test on the same machine using localhost:

**Terminal 1 (Host):**
```bash
./gradlew :core:run --args="host"
```

**Terminal 2 (Client):**
```bash
./gradlew :core:run --args="join localhost"
```

## Network Requirements

- **LAN:** Works out of the box - just use local IP (e.g., `192.168.1.100`)
- **Internet:** Host needs to port forward (router settings) and share public IP
- **Default Port:** 12345 (customizable)

## Thread Safety Note

The `NetworkListener` runs in a background thread. If your UI is not thread-safe (like Swing/JavaFX), you may need to marshal message handling back to the UI thread.

Example for JavaFX:
```java
Platform.runLater(() -> {
    // Update UI here
});
```

## Error Handling

All connection methods throw `IOException`. Handle appropriately:

```java
try {
    network.startHost(12345);
} catch (IOException e) {
    System.err.println("Failed to start: " + e.getMessage());
    // Show error dialog to user
}
```

## Cleanup

Always disconnect when done:

```java
// On game exit or disconnect button
network.disconnect();
```

