package persistence;

import java.sql.*;

public class SqlConnect {

    public static void main(String[] args) throws ClassNotFoundException {

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        // Create a variable for the connection string.
        String connectionUrl = "jdbc:sqlserver://localhost:1433;databaseName=TestDB;";

        try (Connection con = DriverManager.getConnection(connectionUrl, "sa", "<YourStrong$Passw0rd>"); Statement stmt = con.createStatement()) {
            String SQL = "SELECT * FROM TestDB";
            ResultSet rs = stmt.executeQuery(SQL);

            // Iterate through the data in the result set and display it.
            while (rs.next()) {
                System.out.println(rs.getString("FirstName") + " " + rs.getString("LastName"));
            }
        }
        // Handle any errors that may have occurred.
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
