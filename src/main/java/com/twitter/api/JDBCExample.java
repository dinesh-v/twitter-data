package com.twitter.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.logging.Logger;

/**
 * JDBC connection to connect to MySQL
 */
class JDBCExample {
    private final static Logger LOGGER = Logger.getLogger(JDBCExample.class.getName());
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
        boolean insertStatus = false;
        int insertID = 0;
        try {
            String userName = null;
            if (jsonObject.has("user")) {
                JSONObject userJsonObject = (JSONObject) jsonObject.get("user");
                if (userJsonObject.has("name")) {
                    userName = userJsonObject.getString("name");
                }
            }
            PreparedStatement preparedStmt = conn.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setInt(1, jsonObject.getInt("id"));
            preparedStmt.setString(2, userName);
            preparedStmt.setString(3, jsonObject.getString("text"));
            preparedStmt.setString(4, jsonObject.getString("created_at"));
            preparedStmt.execute();

            ResultSet rs = preparedStmt.getGeneratedKeys();
            if (rs.next()) {
                insertID = rs.getInt(1);
            }

            if (insertID != 0) {
                LOGGER.info("Insert ID : " + insertID + " , Insert Status : " + insertStatus);
                // tweet_id,user_name,tweet_text,created_at
                JSONObject elasticSearchJsonObject = new JSONObject();
                elasticSearchJsonObject.put("id", insertID);
                elasticSearchJsonObject.put("tweet_id", jsonObject.getInt("id"));
                elasticSearchJsonObject.put("user_name", userName);
                elasticSearchJsonObject.put("tweet_text", jsonObject.getString("text"));
                elasticSearchJsonObject.put("created_at", jsonObject.getString("created_at"));
                ElasticSearch elasticSearch = new ElasticSearch();
                elasticSearch.postElastic(elasticSearchJsonObject);
            }
        } catch (SQLException e) {
            LOGGER.severe(e.getMessage());
        }
        return insertStatus;
    }
}

class ElasticSearch {
    private final static Logger LOGGER = Logger.getLogger(ElasticSearch.class.getName());

    void postElastic(JSONObject jsonObject) {
        try {

            StringEntity stringEntity = new StringEntity(jsonObject.toString());
            HttpClient httpClient = new DefaultHttpClient();


            String BASE_URL = "http://127.0.0.1:9200";
            String INDEX = "twitter";
            String TYPE = "tweets";
            String URL = BASE_URL + "/" + INDEX + "/" + TYPE + "/";

            LOGGER.info(String.valueOf(jsonObject.get("id")));
            HttpPost httpPost = new HttpPost(URL + jsonObject.get("id"));

            httpPost.setEntity(stringEntity);

            HttpResponse httpResponse = httpClient.execute(httpPost);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            String response = null;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response += line;
            }
            LOGGER.info("Response is : " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}