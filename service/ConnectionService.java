package service;

import db.DBConnection;
import exception.UCMSException;

/** Lets the UI test and change database settings without touching the db package. */
public class ConnectionService {

    /** Returns null if MySQL is reachable, otherwise a readable error. */
    public String test() {
        return DBConnection.testConnection();
    }

    public String getHost()   { return DBConnection.getHost(); }
    public String getPort()   { return DBConnection.getPort(); }
    public String getDbName() { return DBConnection.getDbName(); }
    public String getUser()   { return DBConnection.getUser(); }
    public String getPassword() { return DBConnection.get("db.password", ""); }

    public void save(String host, String port, String dbName, String user, String password) throws UCMSException {
        Validator.positiveInt(port, "Port");
        DBConnection.saveConfig(Validator.required(host, "Host"), port.trim(),
                Validator.required(dbName, "Database name"), Validator.required(user, "User"),
                password == null ? "" : password);
    }

    public String describe() {
        return getUser() + "@" + getHost() + ":" + getPort() + "/" + getDbName();
    }
}
