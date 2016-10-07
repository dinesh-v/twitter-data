package com.twitter.api;

import org.json.JSONObject;

import java.sql.*;
import java.util.logging.Logger;

/**
 * JDBC connection to connect to MySQL
 */
class JDBCExample {
    private final static Logger LOGGER = Logger.getLogger(App.class.getName());
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost/twitter?useUnicode=true&characterEncoding=UTF-8";

    private Connection conn = null;
    private static final String INSERT_QUERY = "INSERT INTO tweets (tweet_id,user_name,tweet_text,created_at) values (?,?,?,?)";

    JDBCExample(String username, String password) {
        try {
            Class.forName(JDBC_DRIVER);
            LOGGER.info("Connecting to database!");
            conn = DriverManager.getConnection(DB_URL, username, password);
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Insert twitter data into database
     *
     * @param jsonObject JsonObject which was retrieved from twitter
     * @return status of insert performed in myqsl
     */
    boolean insert(JSONObject jsonObject) {
        try {
            String userName = null;
            if (jsonObject.has("user")) {
                JSONObject userJsonObject = (JSONObject) jsonObject.get("user");
                if (userJsonObject.has("name")) {
                    userName = userJsonObject.getString("name");
                }
            }
            PreparedStatement preparedStmt = conn.prepareStatement(INSERT_QUERY);
            preparedStmt.setInt(1, jsonObject.getInt("id"));
            preparedStmt.setString(2, userName);
            preparedStmt.setString(3, jsonObject.getString("text"));
            preparedStmt.setString(4, jsonObject.getString("created_at"));
            return preparedStmt.execute();
            // TODO: If successfully inserted, make a REST call to insert into elastic search
        } catch (SQLException e) {
            LOGGER.severe(e.getMessage());
        }
        return false;
    }
}