package at.gl1tchxd.battleship.network;

/**
 * Enum representing different types of network packets in the P2P battleship game.
 */
public enum PacketType {
    // Connection & Setup
    CONNECT_REQUEST,
    CONNECT_ACCEPT,
    DISCONNECT,

    // Game Initialization
    GAME_INIT,

    // Placement Phase
    PLACEMENT_READY,

    // Battle Phase
    ATTACK,
    ATTACK_RESULT,

    // Game State
    TURN_CHANGE,
    GAME_OVER,

    // Sync & Misc
    SYNC_REQUEST,
    SYNC_RESPONSE,
    CHAT_MESSAGE
}

