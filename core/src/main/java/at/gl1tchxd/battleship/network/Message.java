package at.gl1tchxd.battleship.network;

import java.util.Map;

public class Message {
    MessageType messageType;
    Map<String, Object> data;
    public Message(MessageType messageType, Map<String, Object> data) {
        this.messageType = messageType;
        this.data = data;
    }
}
