package lift.events;

import java.net.Socket;

public interface SocketListener {
    
    void onSocketConnected(Socket socket);
    
}
