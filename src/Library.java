import com.sun.jdi.InterfaceType;

import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Library {
    private static Connection conn = null;
    private static Scanner in;

    public static int printMainMenu() {
        int command = 0;
        while (command < 1 || command > 3) {
            System.out.println("1) Register\n2) Login\n3) Exit");
            command = in.nextInt();
        }

        return command;
    }

    public static String[] getRegisterInputs() {
        String[] inputs = new String[6];

        System.out.println("please enter username, password, first name, last name, address, user type:");
        in.nextLine();
        inputs[0] = in.nextLine().trim();
        inputs[1] = in.nextLine().trim();
        inputs[2] = in.nextLine().trim();
        inputs[3] = in.nextLine().trim();
        inputs[4] = in.nextLine().trim();
        inputs[5] = in.nextLine().trim();

        return inputs;
    }

    public static int register(String[] inputs) {
        int result = -1;
        try (CallableStatement properCase = conn.prepareCall("{ ? = call user_register( ?, ?, ?, ?, ?, ? ) }")) {
            properCase.registerOutParameter(1, Types.INTEGER);

            properCase.setString(2, inputs[0]);
            properCase.setString(3, inputs[1]);
            properCase.setString(4, inputs[2]);
            properCase.setString(5, inputs[3]);
            properCase.setString(6, inputs[4]);
            properCase.setString(7, inputs[5]);
            properCase.execute();
            result = (properCase.getInt(1));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }


    public static String[] getLoginInputs() {
        String[] inputs = new String[2];

        System.out.println("please enter username and password:");
        in.nextLine();
        inputs[0] = in.nextLine().trim();
        inputs[1] = in.nextLine().trim();
        return inputs;
    }

    public static ArrayList<Object> login(String[] inputs) {
        ArrayList<Object> result = new ArrayList<>();
        try (CallableStatement properCase = conn.prepareCall("{ call user_login( ?, ?, ?, ?, ? ) }")) {
            properCase.registerOutParameter(1, Types.INTEGER);
            properCase.registerOutParameter(2, Types.VARCHAR);
            properCase.registerOutParameter(3, Types.VARCHAR);
            properCase.setString(4, inputs[0]);
            properCase.setString(5, inputs[1]);
            properCase.execute();
            result.add(properCase.getInt(1));
            result.add(properCase.getString(2));
            result.add(properCase.getString(3));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    private static void handleRegisterResponse(int code) {
        switch (code) {
            case 1 -> System.out.println("please enter all info");
            case 2 -> System.out.println("username must be more than 5 characters");
            case 3 -> System.out.println("password must be more than 7 characters");
            case 4 -> System.out.println("username exists");
            case 5 -> System.out.println("Only characters and numerics are allowed in username");
            default -> System.out.println("Successfully registered");
        }
    }

    private static void handleLoginResponse(int code) {
        if (code == 1) {
            System.out.println("user not found");
        } else {
            System.out.println("incorrect password");
        }
    }

    public static void main(String[] args) {
        try {
            String url = "jdbc:postgresql://localhost:5432/library";
            conn = DriverManager.getConnection(url);
            in = new Scanner(System.in);

            while (true) {
                int command = printMainMenu();
                switch (command) {
                    case 1:
                        String[] inputs = getRegisterInputs();
                        int res = register(inputs);

                        handleRegisterResponse(res);
                        break;
                    case 2:
                        String[] loginInputs = getLoginInputs();
                        ArrayList<Object> loginResult = login(loginInputs);

                        if ((int) loginResult.get(0) == 0)
                            new User(loginInputs[0], (String) loginResult.get(1), (String) loginResult.get(2), in, conn)
                                    .enterLibrary();
                        else
                            handleLoginResponse((int) loginResult.get(0));
                        break;
                    default:
                        return;
                }
            }


        } catch (SQLException e) {
            throw new Error("Problem", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
