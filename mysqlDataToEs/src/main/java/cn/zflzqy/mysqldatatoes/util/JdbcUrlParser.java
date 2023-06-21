package cn.zflzqy.mysqldatatoes.util;

import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * @author zflzqy
 * @Description 解析jdbcUrl
 */
public class JdbcUrlParser {
    public static JdbcConnectionInfo parseJdbcUrl(String jdbcUrl) {
        if (!StringUtils.hasText(jdbcUrl)) {
            return new JdbcConnectionInfo();
        }
        JdbcConnectionInfo connectionInfo = new JdbcConnectionInfo();

        String host = jdbcUrl.substring(jdbcUrl.indexOf("//") + 2, jdbcUrl.lastIndexOf(":"));
        connectionInfo.setHost(host);

        int portStartIndex = jdbcUrl.lastIndexOf(":") + 1;
        int portEndIndex = jdbcUrl.indexOf("/", portStartIndex);
        int port = Integer.parseInt(jdbcUrl.substring(portStartIndex, portEndIndex));
        connectionInfo.setPort(port);

        int databaseStartIndex = jdbcUrl.indexOf(String.valueOf(port))+String.valueOf(port).length() + 1;
        int databaseEndIndex = jdbcUrl.indexOf("?", databaseStartIndex);
        String database = jdbcUrl.substring(databaseStartIndex, databaseEndIndex);
        connectionInfo.setDatabase(database);

        String query = jdbcUrl.substring(jdbcUrl.indexOf("?") + 1);
        Properties properties = new Properties();
        String[] keyValuePairs = query.split("&");
        for (String keyValue : keyValuePairs) {
            String[] parts = keyValue.split("=");
            if (parts.length == 2) {
                properties.setProperty(parts[0], parts[1]);
            }
        }
        connectionInfo.setProperties(properties);

        return connectionInfo;
    }

    public static class JdbcConnectionInfo {
        private String host;
        private int port;
        private String database;
        private Properties properties;

        // Getter and Setter methods

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public Properties getProperties() {
            return properties;
        }

        public void setProperties(Properties properties) {
            this.properties = properties;
        }
    }
}
