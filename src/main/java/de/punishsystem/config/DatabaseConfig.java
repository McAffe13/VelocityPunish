package de.punishsystem.config;

public class DatabaseConfig {

    private boolean enabled = false;
    private String host = "localhost";
    private int port = 3306;
    private String database = "punishsystem";
    private String username = "root";
    private String password = "";
    private int maxPoolSize = 10;
    private String tablePrefix = "";

    public String getJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC&characterEncoding=UTF-8"
                + "&autoReconnect=true";
    }

    public boolean isEnabled() { return enabled; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public String getTablePrefix() { return tablePrefix; }
}
