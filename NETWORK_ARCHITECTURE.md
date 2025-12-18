# Network Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        YOUR UI CODE                             │
│                   (Console/Desktop/etc.)                        │
└────────────┬────────────────────────────────────┬───────────────┘
             │                                    │
             │ calls methods                      │ calls methods
             │                                    │
             ▼                                    ▼
┌────────────────────────┐         ┌────────────────────────────┐
│   GameController       │◄────────│  NetworkGameController     │
│   (game logic)         │         │  (network bridge)          │
│                        │         │                            │
│  - initializeGame()    │         │  + hostGame()              │
│  - placeShip()         │         │  + joinGame()              │
│  - receiveAttack()     │         │  + sendAttack()            │
│  - recordAttackResult()│         │  + sendPlacementReady()    │
│  - isMyTurn()          │         │  + sendGameInit()          │
│  - setCurrentTurn()    │         │                            │
└────────────────────────┘         └────────┬───────────────────┘
                                            │
                                            │ uses
                                            │
                                            ▼
                                   ┌────────────────────────────┐
                                   │    NetworkManager          │
                                   │    (TCP sockets)           │
                                   │                            │
                                   │  + host(port)              │
                                   │  + connect(host, port)     │
                                   │  + send(packet)            │
                                   │  + disconnect()            │
                                   │                            │
                                   │  [Send Thread]             │
                                   │  [Receive Thread]          │
                                   │  [Server Thread]           │
                                   └────────┬───────────────────┘
                                            │
                                            │ sends/receives
                                            │
                                            ▼
                                   ┌────────────────────────────┐
                                   │      GamePacket            │
                                   │      (data)                │
                                   │                            │
                                   │  - type: PacketType        │
                                   │  - row, col: int           │
                                   │  - hit: boolean            │
                                   │  - shipSunk: boolean       │
                                   │  - gameWon: boolean        │
                                   │  - shipLength: int         │
                                   │  - etc.                    │
                                   └────────────────────────────┘
                                            │
                                            │ uses enum
                                            │
                                            ▼
                                   ┌────────────────────────────┐
                                   │      PacketType (enum)     │
                                   │                            │
                                   │  CONNECT_REQUEST           │
                                   │  CONNECT_ACCEPT            │
                                   │  GAME_INIT                 │
                                   │  PLACEMENT_READY           │
                                   │  ATTACK                    │
                                   │  ATTACK_RESULT             │
                                   │  TURN_CHANGE               │
                                   │  GAME_OVER                 │
                                   │  CHAT_MESSAGE              │
                                   └────────────────────────────┘
```

## Data Flow Example: Attack Sequence

```
Player A (Attacker)                              Player B (Defender)
─────────────────                                ────────────────

1. User clicks cell (5, 3)
   │
   ├─► networkController.sendAttack(5, 3)
   │
   ├─► Creates GamePacket:
   │   - type: ATTACK
   │   - row: 5, col: 3
   │
   └─► NetworkManager.send(packet)
         │
         │ ═══════════════════════════════════════════════►
         │            TCP Connection                       │
         │                                                 ▼
         │                              NetworkManager receives packet
         │                                        │
         │                                        ├─► onPacketReceived()
         │                                        │
         │                                        ├─► handleAttack()
         │                                        │
         │                                        ├─► gameController.receiveAttack(5, 3)
         │                                        │   returns AttackResult(hit=true, shipSunk=true)
         │                                        │
         │                                        └─► Creates GamePacket:
         │                                            - type: ATTACK_RESULT
         │                                            - hit: true
         │                                            - shipSunk: true
         │                                            - shipLength: 4
         │                                            - gameWon: false
         │
         │ ◄═══════════════════════════════════════════════
         │            TCP Connection
         ▼
   NetworkManager receives result packet
   │
   ├─► onPacketReceived()
   │
   ├─► handleAttackResult()
   │
   ├─► gameController.recordAttackResult(5, 3, true)
   │
   └─► onAttackResultReceived(true, true, false, 4)
       │
       └─► UI updates to show "HIT! Ship sunk!"
```

## Key Design Decisions

1. **Enums over Strings**: PacketType enum provides compile-time safety
2. **Lightweight Packets**: Only ship length sent, not full Ship objects
3. **Separation of Concerns**: Network logic separate from game logic
4. **Thread Safety**: Separate threads with blocking queues
5. **Simple P2P**: Direct connection, no server infrastructure needed
6. **Event-driven**: Override methods to customize behavior

## Security Considerations

- No ship positions are ever transmitted (prevents cheating)
- Only attack results (hit/miss) are sent back
- Ship length only sent when ship is sunk
- Board state export shows only what opponent should know

