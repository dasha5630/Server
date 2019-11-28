package lift.net;

import lift.events.SocketListener;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;


public class ConnectionReceiver implements Runnable, Closeable {

    private ServerSocket socket;

    private Set<SocketListener> listeners;

    public ConnectionReceiver(int port) throws IOException {
        socket = new ServerSocket(port);
        listeners = new CopyOnWriteArraySet<>();
    }

    public void addListener(SocketListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SocketListener listener) {
        listeners.remove(listener);
    }

    private void notifySocketListener(Socket socket) {
        for (SocketListener item : listeners) {
            item.onSocketConnected(socket);
        }
    }

    @Override
    public void run() {
        while (true) {
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
                break;
            }
            try {
                Socket client = socket.accept();
                notifySocketListener(client);
            } catch (IOException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        try {
            close();
        } catch (IOException ignore) {
        }
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
            socket = null;
        }

        if (listeners != null) {
            listeners.clear();
            listeners = null;
        }
    }
}