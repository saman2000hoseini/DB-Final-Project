import java.sql.*;
import java.util.Scanner;

public class User {
    private String username;
    private String token;
    private final String userType;
    private final Scanner in;
    private final Connection conn;

    private int printAdminMenu() {
        System.out.println("""
                1) Get info
                2) Search book
                3) Insert book
                4) Insert publisher
                5) Remove user
                6) Search user
                7) Update storage
                8) Borrow book
                9) Return book
                10) Search borrow
                11) Increase credit
                12) Get borrow history
                13) Get delayed books
                14) Family search""");

        return in.nextInt();
    }

    private int printLibrarianMenu() {
        System.out.println("""
                1) Get info
                2) Search book
                3) Insert book
                4) Insert publisher
                5) Increase credit
                6) Search user
                7) Update storage
                8) Borrow book
                9) Return book
                10) Search borrow
                11) Get borrow history
                12) Get delayed books
                13) Family search""");

        return in.nextInt();
    }

    private int printUserMenu() {
        System.out.println("""
                1) Get info
                2) Search book
                3) Increase credit
                4) Borrow book
                5) Return book""");

        return in.nextInt();
    }

    private void getInfo() {
        try (CallableStatement call = conn.prepareCall("{ call user_get_info( ?, ?, ?, ?, ?, ?, ?, ? ) }")) {
            call.registerOutParameter(1, Types.VARCHAR);
            call.registerOutParameter(2, Types.VARCHAR);
            call.registerOutParameter(3, Types.VARCHAR);
            call.registerOutParameter(4, Types.VARCHAR);
            call.registerOutParameter(5, Types.VARCHAR);
            call.registerOutParameter(6, Types.INTEGER);
            call.registerOutParameter(7, Types.TIMESTAMP);
            call.setString(8, token);
            call.execute();

            System.out.println("-----------------------------------------");
            System.out.println("Username: " + call.getString(1));
            System.out.println("Firstname: " + call.getString(2));
            System.out.println("Lastname: " + call.getString(3));
            System.out.println("Address: " + call.getString(4));
            System.out.println("Usertype: " + call.getString(5));
            System.out.println("Credit: " + call.getInt(6));
            System.out.println("Registration: " + call.getObject(7).toString());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String[] getInsertPublisherInputs() {
        String[] inputs = new String[3];

        System.out.println("please enter publisher name, publisher address and publisher website:");
        in.nextLine();
        inputs[0] = in.nextLine().trim();
        inputs[1] = in.nextLine().trim();
        inputs[2] = in.nextLine().trim();

        return inputs;
    }

    private void insertPublisher(String[] inputs) {
        try (CallableStatement call = conn.prepareCall("{ call publisher_insert( ?, ?, ?, ?, ? ) }")) {
            call.registerOutParameter(1, Types.INTEGER);
            call.setString(2, token);
            call.setString(3, inputs[0]);
            call.setString(4, inputs[1]);
            call.setString(5, inputs[2]);
            call.execute();

            switch (call.getInt(1)) {
                case 1 -> System.out.println("you should fill all fields");
                case 2 -> System.out.println("unauthorized");
                case 3 -> System.out.println("publisher already exists");
                default -> System.out.println("publisher successfully added");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String[] getInsertBookInputs() {
        String[] inputs = new String[9];

        System.out.println("please enter title, subject, price," +
                " pages, type, publish date, publisher, edition and volume:");
        in.nextLine();
        for (int i = 0; i < 9; i++) {
            inputs[i] = in.nextLine().trim();
        }

        return inputs;
    }

    private void insertBook(String[] inputs) {
        try (CallableStatement call = conn.prepareCall("{ call book_insert( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) }")) {
            call.registerOutParameter(1, Types.INTEGER);
            call.setString(2, token);
            call.setString(3, inputs[0]);
            call.setString(4, inputs[1]);
            call.setInt(5, Integer.parseInt(inputs[3]));
            call.setInt(6, Integer.parseInt(inputs[4]));
            call.setString(7, inputs[5]);
            call.setDate(8, Date.valueOf(inputs[6]));
            call.setInt(9, Integer.parseInt(inputs[7]));
            call.setInt(10, Integer.parseInt(inputs[8]));
            call.setInt(11, Integer.parseInt(inputs[9]));
            call.execute();

            switch (call.getInt(1)) {
                case 1 -> System.out.println("you should fill all fields");
                case 2 -> System.out.println("unauthorized");
                case 3 -> System.out.println("book already exists");
                default -> System.out.println("book successfully added");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String[] getSearchBookInputs() {
        String[] inputs = new String[5];

        System.out.println("please enter title, author, edition, publisher and publish date:");
        in.nextLine();
        inputs[0] = in.nextLine().trim();
        inputs[1] = in.nextLine().trim();
        inputs[2] = in.nextLine().trim();
        inputs[3] = in.nextLine().trim();
        inputs[4] = in.nextLine().trim();

        return inputs;
    }

    private void searchBook(String[] inputs) {
        try (CallableStatement call = conn.prepareCall("{ call search_book( ?, ?, ?, ?, ? ) }")) {
            call.setString(1, inputs[0]);
            call.setString(2, inputs[1]);
            if (inputs[2].equals(""))
                call.setObject(3, null);
            else
                call.setInt(3, Integer.parseInt(inputs[2]));

            call.setString(4, inputs[3]);

            if (inputs[4].equals(""))
                call.setObject(5, null);
            else
                call.setDate(5, Date.valueOf(inputs[4]));
            ResultSet res = call.executeQuery();

            while (res.next())
                System.out.println("title: " + res.getString("b_title") +
                        ",  author: " + res.getString("b_author") +
                        ",  volume: " + res.getString("b_volume") +
                        ",  edition: " + res.getString("b_edition") +
                        ",  publisher: " + res.getString("b_publisher") +
                        ",  published: " + res.getString("b_published") +
                        ",  subject: " + res.getString("b_subject") +
                        ",  pages: " + res.getString("b_pages") +
                        ",  price: " + res.getString("b_price"));
            System.out.println("-------------------------------------------------------------------------------------");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void removeUser() {
        System.out.println("please enter a username: ");
        in.nextLine();
        String uname = in.nextLine().trim();

        try (CallableStatement call = conn.prepareCall("{ call user_remove( ?, ?, ?) }")) {
            call.registerOutParameter(1, Types.INTEGER);
            call.setString(2, token);
            call.setString(3, uname);
            call.execute();

            int res = call.getInt(1);
            switch (res) {
                case 1 -> System.out.println("you must enter a username");
                case 2 -> System.out.println("unauthorized");
                default -> System.out.println("removed successfully");
            }
        } catch (
                SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    private void getUserInfo() {
        System.out.println("please enter a username: ");
        in.nextLine();
        String uname = in.nextLine().trim();

        try (CallableStatement call = conn.prepareCall("{ call user_search( ?, ?, ?, ?, ?, ?, ?, ?) }")) {
            call.registerOutParameter(1, Types.VARCHAR);
            call.registerOutParameter(2, Types.VARCHAR);
            call.registerOutParameter(3, Types.VARCHAR);
            call.registerOutParameter(4, Types.INTEGER);
            call.registerOutParameter(5, Types.VARCHAR);
            call.registerOutParameter(6, Types.TIMESTAMP);
            call.setString(7, token);
            call.setString(8, uname);
            call.execute();

            System.out.println("-----------------------------------------");
            System.out.println("Firstname: " + call.getString(1));
            System.out.println("Lastname: " + call.getString(2));
            System.out.println("Address: " + call.getString(3));
            System.out.println("Credit: " + call.getInt(4));
            System.out.println("Usertype: " + call.getString(5));
            System.out.println("Registration: " + call.getObject(6).toString());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String[] getAddBookInputs() {
        String[] inputs = new String[4];

        System.out.println("please enter amount, title, edition and volume:");
        in.nextLine();
        for (int i = 0; i < 4; i++) {
            inputs[i] = in.nextLine().trim();
        }

        return inputs;
    }

    private void addBooks(String[] inputs) {
        try (CallableStatement call = conn.prepareCall("{ call book_add( ?, ?, ?, ?, ?, ? ) }")) {
            call.registerOutParameter(1, Types.INTEGER);
            call.setString(2, token);
            call.setInt(3, Integer.parseInt(inputs[0]));
            call.setString(4, inputs[1]);
            call.setInt(5, Integer.parseInt(inputs[2]));
            call.setInt(6, Integer.parseInt(inputs[3]));
            call.execute();

            switch (call.getInt(1)) {
                case 1 -> System.out.println("you should fill all fields");
                case 2 -> System.out.println("unauthorized");
                case 3 -> System.out.println("book does not exist");
                default -> System.out.println("book successfully added");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String[] getBorrowBookInputs() {
        String[] inputs = new String[3];

        System.out.println("please enter title, edition and volume:");
        in.nextLine();
        for (int i = 0; i < 3; i++) {
            inputs[i] = in.nextLine().trim();
        }

        return inputs;
    }

    private void borrowBook(String[] inputs) {
        try (CallableStatement call = conn.prepareCall("{ call book_borrow( ?, ?, ?, ?, ? ) }")) {
            call.registerOutParameter(1, Types.INTEGER);
            call.setString(2, token);
            call.setString(3, inputs[0]);
            call.setInt(4, Integer.parseInt(inputs[1]));
            call.setInt(5, Integer.parseInt(inputs[2]));
            call.execute();

            switch (call.getInt(1)) {
                case 1 -> System.out.println("you should fill all fields");
                case 2 -> System.out.println("unauthorized");
                case 3 -> System.out.println("you are suspended");
                case 4 -> System.out.println("book does not exist");
                case 5 -> System.out.println("book is currently unavailable");
                case 6 -> System.out.println("access denied");
                case 7 -> System.out.println("your credit is not enough");
                default -> System.out.println("book successfully borrowed");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void returnBook(String[] inputs) {
        try (CallableStatement call = conn.prepareCall("{ call book_return( ?, ?, ?, ?, ? ) }")) {
            call.registerOutParameter(1, Types.INTEGER);
            call.setString(2, token);
            call.setString(3, inputs[0]);
            call.setInt(4, Integer.parseInt(inputs[1]));
            call.setInt(5, Integer.parseInt(inputs[2]));
            call.execute();

            switch (call.getInt(1)) {
                case 1 -> System.out.println("you should fill all fields");
                case 2 -> System.out.println("unauthorized");
                case 3 -> System.out.println("you dont have this book");
                default -> System.out.println("book successfully returned");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void searchBorrows(String[] inputs) {
        try (CallableStatement call = conn.prepareCall("{ call borrow_search( ?, ?, ?, ? ) }")) {
            call.setString(1, token);
            call.setString(2, inputs[0]);
            call.setInt(3, Integer.parseInt(inputs[1]));
            call.setInt(4, Integer.parseInt(inputs[2]));
            ResultSet resultSet = call.executeQuery();
            while (resultSet.next()) {
                System.out.println("-----------------------------------------");
                System.out.println("Username: " + resultSet.getString(1));
                System.out.println("Firstname: " + resultSet.getString(2));
                System.out.println("Lastname: " + resultSet.getString(3));
                System.out.println("Usertype: " + resultSet.getString(4));
                System.out.println("Borrow date: " + resultSet.getObject(5).toString());
                Object returnDate = resultSet.getObject(6);
                if (returnDate != null)
                    System.out.println("Return date: " + returnDate.toString());
                else
                    System.out.println("Return date: -");
            }
            System.out.println();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void addCredit() {
        System.out.println("please enter amount");
        int amount = in.nextInt();
        try (CallableStatement call = conn.prepareCall("{ call user_add_credit( ?, ?, ? ) }")) {
            call.registerOutParameter(1, Types.INTEGER);
            call.setString(2, token);
            call.setInt(3, amount);
            call.execute();

            switch (call.getInt(1)) {
                case 1 -> System.out.println("you should fill all fields");
                case 2 -> System.out.println("unauthorized");
                case 3 -> System.out.println("amount should be positive");
                default -> System.out.println("your credit successfully increased");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String[] getBorrowHistoryInputs() {
        String[] inputs = new String[4];

        System.out.println("please enter username, title, edition and volume:");
        in.nextLine();
        for (int i = 0; i < 4; i++) {
            inputs[i] = in.nextLine().trim();
        }

        return inputs;
    }

    private void getBorrowHistory(String[] inputs) {
        try (CallableStatement call = conn.prepareCall("{ call borrow_get_history( ?, ?, ?, ?, ? ) }")) {
            call.setString(1, token);
            call.setString(2, inputs[0]);
            call.setString(3, inputs[1]);
            if (inputs[2].equals(""))
                call.setObject(4, null);
            else
                call.setInt(4, Integer.parseInt(inputs[2]));
            if (inputs[3].equals(""))
                call.setObject(5, null);
            else
                call.setInt(5, Integer.parseInt(inputs[3]));

            ResultSet resultSet = call.executeQuery();
            while (resultSet.next()) {
                System.out.println("-----------------------------------------");
                System.out.println("ID: " + resultSet.getInt(1));
                System.out.println("Username: " + resultSet.getString(2));
                System.out.println("Book title: " + resultSet.getString(3));
                System.out.println("Book volume: " + resultSet.getInt(4));
                System.out.println("Book edition: " + resultSet.getString(5));
                System.out.println("Operation: " + resultSet.getString(6));
                System.out.println("Result: " + resultSet.getString(7));
                System.out.println("Operation date: " + resultSet.getObject(8).toString());
            }
            System.out.println();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void getDelayedBooks() {
        try (CallableStatement call = conn.prepareCall("{ call book_delayed( ? ) }")) {
            call.setString(1, token);
            ResultSet resultSet = call.executeQuery();
            while (resultSet.next()) {
                System.out.println("-----------------------------------------");
                System.out.println("Username: " + resultSet.getString(1));
                System.out.println("Firstname: " + resultSet.getString(2));
                System.out.println("Lastname: " + resultSet.getString(3));
                System.out.println("Usertype: " + resultSet.getString(4));
                System.out.println("Borrow date: " + resultSet.getObject(5).toString());
                System.out.println("Expected return date: " + resultSet.getObject(6).toString());
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void getUserByFamily() {
        System.out.println("please enter a family name and page: ");
        in.nextLine();
        String lname = in.nextLine().trim();
        int page = in.nextInt();

        try (CallableStatement call = conn.prepareCall("{ call user_family_search( ?, ?, ? ) }")) {
            call.setString(1, token);
            call.setString(2, lname);
            call.setInt(3, page);
            ResultSet resultSet = call.executeQuery();
            while (resultSet.next()) {
                System.out.println("-----------------------------------------");
                System.out.println("Username: " + resultSet.getString(1));
                System.out.println("Firstname: " + resultSet.getString(2));
                System.out.println("Address: " + resultSet.getString(3));
                System.out.println("Credit: " + resultSet.getInt(4));
                System.out.println("Usertype: " + resultSet.getString(5));
                System.out.println("Registration: " + resultSet.getObject(6).toString());
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public User(String username, String token, String userType, Scanner in, Connection conn) {
        this.username = username;
        this.token = token;
        this.userType = userType;
        this.in = in;
        this.conn = conn;
    }

    public void enterLibrary() {
        switch (userType) {
            case "admin" -> adminLoop();
            case "librarian" -> librarianLoop();
            default -> userLoop();
        }
    }

    private void adminLoop() {
        String[] inputs;

        while (true) {
            int command = printAdminMenu();
            switch (command) {
                case 1:
                    getInfo();
                    break;
                case 2:
                    inputs = getSearchBookInputs();
                    searchBook(inputs);
                    break;
                case 3:
                    inputs = getInsertBookInputs();
                    insertBook(inputs);
                    break;
                case 4:
                    inputs = getInsertPublisherInputs();
                    insertPublisher(inputs);
                    break;
                case 5:
                    removeUser();
                    break;
                case 6:
                    getUserInfo();
                    break;
                case 7:
                    inputs = getAddBookInputs();
                    addBooks(inputs);
                    break;
                case 8:
                    inputs = getBorrowBookInputs();
                    borrowBook(inputs);
                    break;
                case 9:
                    inputs = getBorrowBookInputs();
                    returnBook(inputs);
                    break;
                case 10:
                    inputs = getBorrowBookInputs();
                    searchBorrows(inputs);
                    break;
                case 11:
                    addCredit();
                    break;
                case 12:
                    inputs = getBorrowHistoryInputs();
                    getBorrowHistory(inputs);
                    break;
                case 13:
                    getDelayedBooks();
                    break;
                case 14:
                    getUserByFamily();
                    break;
                default:
                    return;
            }
        }
    }

    private void librarianLoop() {
        String[] inputs;

        while (true) {
            int command = printLibrarianMenu();
            switch (command) {
                case 1:
                    getInfo();
                    break;
                case 2:
                    inputs = getSearchBookInputs();
                    searchBook(inputs);
                    break;
                case 3:
                    inputs = getInsertBookInputs();
                    insertBook(inputs);
                    break;
                case 4:
                    inputs = getInsertPublisherInputs();
                    insertPublisher(inputs);
                    break;
                case 5:
                    addCredit();
                    break;
                case 6:
                    getUserInfo();
                    break;
                case 7:
                    inputs = getAddBookInputs();
                    addBooks(inputs);
                    break;
                case 8:
                    inputs = getBorrowBookInputs();
                    borrowBook(inputs);
                    break;
                case 9:
                    inputs = getBorrowBookInputs();
                    returnBook(inputs);
                    break;
                case 10:
                    inputs = getBorrowBookInputs();
                    searchBorrows(inputs);
                    break;
                case 11:
                    inputs = getBorrowHistoryInputs();
                    getBorrowHistory(inputs);
                    break;
                case 12:
                    getDelayedBooks();
                    break;
                case 13:
                    getUserByFamily();
                    break;
                default:
                    return;
            }
        }
    }

    private void userLoop() {
        String[] inputs;

        while (true) {
            int command = printUserMenu();
            switch (command) {
                case 1:
                    getInfo();
                    break;
                case 2:
                    inputs = getSearchBookInputs();
                    searchBook(inputs);
                    break;
                case 3:
                    addCredit();
                    break;
                case 4:
                    inputs = getBorrowBookInputs();
                    borrowBook(inputs);
                    break;
                case 5:
                    inputs = getBorrowBookInputs();
                    returnBook(inputs);
                    break;
                default:
                    return;
            }
        }
    }
}
