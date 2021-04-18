package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataBaseAuthService implements AuthService{

    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement psInsert;

    private class UserData {
        String login;
        String password;
        String nickname;

        public UserData(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    private List<UserData> users;

    public DataBaseAuthService() {
        users = new ArrayList<>();

        try {
            connect();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            while (rs.next()){
                users.add(new UserData(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
            rs.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            disconnect();
        }

    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        for (UserData u : users) {
            if(u.login.equals(login) && u.password.equals(password)){
                return u.nickname;
            }
        }

        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        for (UserData u : users) {
            if(u.login.equals(login) || u.nickname.equals(nickname)){
                return false;
            }
        }
        try {
            connect();
            psInsert = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES ( ? , ? , ?);");
            psInsert.setString(1, login);
            psInsert.setString(2, password);
            psInsert.setString(3, nickname);
            psInsert.executeUpdate();

            users.add(new UserData(login, password, nickname));
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        } finally {
            disconnect();
        }

    }

    @Override
    public boolean changeNickName(String login, String newNickName) {
        try {
            connect();
            stmt.executeUpdate("UPDATE users SET nickname = '" + newNickName + "' WHERE login = '" + login + "';");
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        } finally {
            disconnect();
        }

    }

    private static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        stmt = connection.createStatement();
    }

    private static void disconnect() {
        try {
            stmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
