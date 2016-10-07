package com.twitter.api;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Twitter to MySQL and Elastic search
 *
 * @see <a href="https://github.com/twitter/hbc">Github Twitter HBC</a>
 */
public class App {
    private final static Logger LOGGER = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        LOGGER.info("Twitter program");
        String propertyFile = "configuration.properties";
        Properties properties = new Properties();
        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream(propertyFile)) {
            properties.load(inputStream);
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
            LOGGER.severe("Without loading property files, there is no reason to move forward!");
            System.exit(-1);
        }

        JDBCExample jdbcExample = new JDBCExample(properties.getProperty("username"), properties.getProperty("password"));

        //TODO: Move twitter label to new class to make the code clean
        twitter:
        {
            /* Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
            BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
            BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);

            /* Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
            Hosts hosts = new HttpHosts(Constants.STREAM_HOST);
            StatusesFilterEndpoint statusesFilterEndpoint = new StatusesFilterEndpoint();
            // Optional: set up some followings and track terms
            List<Long> followings = Lists.newArrayList(1234L, 566788L);
            List<String> terms = Lists.newArrayList("twitter", "api");
            statusesFilterEndpoint.followings(followings);
            statusesFilterEndpoint.trackTerms(terms);
            /*statusesFilterEndpoint.languages(Arrays.asList("en"));*/

            Authentication authentication = new OAuth1(properties.getProperty("consumer_key"), properties.getProperty("consumer_secret"), properties.getProperty("access_token"), properties.getProperty("access_token_secret"));

            ClientBuilder builder = new ClientBuilder()
                    .name("Client-01")                              // optional: mainly for the logs
                    .hosts(hosts)
                    .authentication(authentication)
                    .endpoint(statusesFilterEndpoint)
                    .processor(new StringDelimitedProcessor(msgQueue))
                    .eventMessageQueue(eventQueue);                          // optional: use this if you want to process client events

            Client client = builder.build();
            // Attempts to establish a connection.
            client.connect();

            // on a different thread, or multiple different threads....
            while (!client.isDone()) {
                String msg;
                try {
                    msg = msgQueue.take();
                    JSONObject jsonObj = new JSONObject(msg);
                    if (jsonObj.has("id") && jsonObj.has("created_at") && jsonObj.has("text")) {
                        jdbcExample.insert(jsonObj);
                    }
                } catch (InterruptedException e) {
                    LOGGER.severe(e.getMessage());
                }
            }
        }
    }
}
