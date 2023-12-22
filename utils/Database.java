package utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database implements AutoCloseable {
    String[] values;
    private Connection connection;

    public Database(String[] values) throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            this.connection = DriverManager.getConnection(values[0], values[1], values[2]);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MariaDB JDBC driver not found", e);
        } catch (SQLException e) {
            throw new SQLException("SQL Error:", e);
        }
    }

    public int executeUpdate(String query, String... parameters) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setString(i + 1, parameters[i]);
            }

            return preparedStatement.executeUpdate();
        }
    }

    public ResultSet executeQuery(String query, String... parameters) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setString(i + 1, parameters[i]);
            }

            return preparedStatement.executeQuery();
        }
    }
    
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Connection is not open.");
        }
        return connection.prepareStatement(sql);
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
