package librairies.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server implements Runnable {
    private final int serverPort;
    private final String serverName;
    private final ServerSocket listenSocket;
    private final Class<? extends Service> serviceClass;
    private Service currentServiceClass;
    private volatile boolean isRunning = false;
    private Thread serverThread;

    public Server(Class<? extends Service> serviceClass, int serverPort, String serverName) throws IOException {
        this.serverPort = serverPort;
        this.serverName = serverName;
        this.listenSocket = new ServerSocket(serverPort);
        this.serviceClass = serviceClass;
        this.currentServiceClass = null;
    }

    public Server(Class<? extends Service> serviceClass, int serverPort) throws IOException {
        this(serviceClass, serverPort, "Not defined");
    }

    public String getName() {
        return this.serverName;
    }

    public String getIp() {
        return "127.0.0.1:" + this.serverPort;
    }

    public ServerSocket getListenSocket() {
        return listenSocket;
    }

    public void run() {
        isRunning = true;
        try {
            while (isRunning) {
                Socket clientSocket = this.listenSocket.accept();
                currentServiceClass = this.serviceClass.getConstructor(Socket.class).newInstance(clientSocket);
                currentServiceClass.start();
            }
        } catch (NoSuchMethodException e) {
            try {
                this.listenSocket.close();
            } catch (IOException ignored) {}
            System.err.println("Problem on the listening serverPort: " + e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        serverThread = new Thread(this);
        serverThread.start();
    }

    public void stop() throws IOException {
        isRunning = false;
        serverThread.interrupt();
        if (this.currentServiceClass != null) {
            this.currentServiceClass.close();
        }
        this.listenSocket.close();
    }
}
