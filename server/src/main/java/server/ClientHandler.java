package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket, ExecutorService service) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());



            service.execute(() -> {
                try {
                    // установска таймаута, максимальное время молчания,
                    // после которого будет брошено исключение SocketTimeoutException
                    socket.setSoTimeout(120000);

                    // цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            out.writeUTF("/end");
                            throw new RuntimeException("Клиент решил отключиться");
                        }
                        // Аутентификация
                        if (str.startsWith("/auth")) {
                            String[] token = str.split("\\s+", 3);
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server
                                    .getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            if (newNick != null) {
                                login = token[1];
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg("/auth_ok " + nickname);
                                    server.subscribe(this);
                                    System.out.println("Client authenticated. nick: " + nickname +
                                            " Address: " + socket.getRemoteSocketAddress());
                                    socket.setSoTimeout(0);
                                    break;
                                } else {
                                    sendMsg("С этим логином уже авторизовались");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }
                        // Регистрация
                        if (str.startsWith("/reg")) {
                            String[] token = str.split("\\s+", 4);
                            if (token.length < 4) {
                                continue;
                            }
                            boolean b = server.getAuthService()
                                    .registration(token[1], token[2], token[3]);
                            if (b) {
                                sendMsg("/reg_ok");
                            } else {
                                sendMsg("/reg_no");
                            }
                        }
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                out.writeUTF("/end");
                                break;
                            }
                            if (str.startsWith("/w")) {
                                String[] token = str.split("\\s+", 3);
                                server.privateMsg(this, token[1], token[2]);
                            }
                            // Смена Ника
                            if (str.startsWith("/newNick")) {
                                String[] token = str.split("\\s+", 3);
                                if (token.length < 2) {
                                    continue;
                                }
                                String newNickName = token[1];
                                boolean b = server.getAuthService().changeNickName(getLogin(), newNickName);
                                if (b) {
                                    server.broadcastMsg(this, "Я сменил Ник на " + newNickName);
                                    setNickname(newNickName);
                                    out.writeUTF("/newNick_ok " + newNickName);
                                    server.broadcastClientList();
                                } else {
                                    out.writeUTF("Никнейм занят!");
                                }
                            }
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                    //обработать SocketTimeoutException
                } catch (SocketTimeoutException e) {
                    System.out.println("Client timeuot " + socket.getRemoteSocketAddress());
                    try {
                        out.writeUTF("/end");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("client disconnect " + socket.getRemoteSocketAddress());
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }

    public void setNickname(String newNickName) {
        nickname = newNickName;
    }
}
