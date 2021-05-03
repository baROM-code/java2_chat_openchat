package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Server {
    public static final Logger logger = Logger.getLogger("");

    private static ServerSocket server;
    private static Socket socket;
    private static ExecutorService executorService;

    private static final int PORT = 8189;
    private List<ClientHandler> clients;
    private AuthService authService;

    public Server() {
        clients = new CopyOnWriteArrayList<>();
        authService = new DataBaseAuthService();

        LogManager manager = LogManager.getLogManager();
        try {
            manager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            server = new ServerSocket(PORT);
            logger.info("Server started");
            // System.out.println("Server started");

            while (true) {
                socket = server.accept();
                logger.fine("Client connect: " + socket.getRemoteSocketAddress());

                // ExecutorService
                // ExecutorService executorServiceexecutorService = Executors.newFixedThreadPool(10);
                executorService = Executors.newSingleThreadExecutor();
                new ClientHandler(this, socket, executorService);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("%s : %s", sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void privateMsg(ClientHandler sender, String toNickName, String msg){
        String message = String.format("Only %s from %s : %s", toNickName, sender.getNickname(), msg);

        if (sender.getNickname().equals(toNickName)) {
            sender.sendMsg("Вы разговаривайте сами с собой?!");
            return;
        }
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(toNickName)) {
                c.sendMsg(message);
                sender.sendMsg(message);
                return;
            }
        }
        sender.sendMsg("Сообщение не отправлено! " + toNickName + " не в сети");
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }

        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientlist");
        for (ClientHandler c : clients) {
            sb.append(" ").append(c.getNickname());
        }

        String msg = sb.toString();

        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }
}
