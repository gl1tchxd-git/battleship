package at.gl1tchxd.battleship.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;

public class ServerSocket implements Socket {
    Server server;
    Kryo kryo;

    public ServerSocket(int port) throws IOException {
        this.server = new Server();
        this.server.start();
        this.server.bind(port);

        kryo = this.server.getKryo();
        kryo.register(Message.class);
    }

    @Override
    public Server getSocket() {
        return server;
    }
}
