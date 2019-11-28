package lift;

import lift.net.ConnectionReceiver;
import main.java.lift.events.MessageListener;
import main.java.lift.net.MessageReceiver;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import lift.events.SocketListener;

import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Class server provides two-way communication between clients (elevator users) and elevator
 */

public class Server implements SocketListener, MessageListener, Closeable, PropertyChangeListener {

    private ConnectionReceiver receiver;

    private MessageReceiver messageReceiver;

    private Set<Socket> clients;

    private Server() throws IOException {
        receiver = new ConnectionReceiver(8080);
        receiver.addListener(this);
        clients = new CopyOnWriteArraySet<>();
        messageReceiver = new MessageReceiver(clients);
        messageReceiver.addListener(this);
    }

    private void send(Socket socket, String message) {
        byte[] data = message.getBytes();
        try {
            OutputStream stream = socket.getOutputStream();
            stream.write(data);
            stream.flush();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    @Override
    public void onMessangeReceived(Socket sender, String message) {
        System.out.println("message received: " + message);
        message = message.replace("\n", "");
       try(BufferedReader reader = new BufferedReader(new FileReader(message + ".txt"))) {
            String c;
            while((c = reader.readLine())!= null){ //блокирующий - фигово
                send(sender, c);
            }
        }catch (FileNotFoundException e) {
            send(sender, "File not found " + e.getMessage());
        } catch (IOException e) {
            send(sender, e.getMessage());
        }
    }

    @Override
    public void onSocketConnected(Socket socket) {
        clients.add(socket);
    }

    public void start() {
        Thread receiverThread = new Thread(receiver);
        receiverThread.setDaemon(true);
        receiverThread.start();

        receiverThread = new Thread(messageReceiver);
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    @Override
    public void close() throws IOException {
        if (receiver != null) {
            receiver.close();
            receiver = null;
        }

        if (messageReceiver != null) {
            messageReceiver.close();
            messageReceiver = null;
        }

        if (clients != null) {
            for (Socket client : clients) {
                client.close();
            }
            clients.clear();
            clients = null;
        }
    }

    public static void main(String[] args) {
        try (Reader consoleReader = new InputStreamReader(System.in);
             BufferedReader in = new BufferedReader(consoleReader);
             Server server = new Server()) {

            server.start();

            System.out.println("Server start on port 8080! to exit press Enter >> ");
            in.readLine();

        } catch (IOException ex) {
            System.err.println("Server close unexpected!");
            ex.printStackTrace(System.err);

        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        System.out.println(evt.getNewValue());
        for (Socket client : clients) {
            send(client, evt.getNewValue().toString() + " ");
        }

    }
}