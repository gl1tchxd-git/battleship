package at.gl1tchxd.battleship.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;

import java.io.IOException;

public class ClientSocket implements Socket{
    Client client;
    Kryo kryo;

    public ClientSocket(int port, String host) throws IOException {
        this.client = new Client();
        this.client.start();
        this.client.connect(5000, host, port);

        kryo = this.client.getKryo();
        kryo.register(Message.class);
    }

    @Override
    public Client getSocket() {
        return client;
    }
}
