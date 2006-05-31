/*
 * Title:   DBInfo.java
 * Project: NASTY
 *
 * @author Thomas Schurtz
 * @version %I% %G%
 */

package de.japes.servlets.nasty;

/**
 * This class in bean-format holds the information needed to talk to a database via
 * the command line tools mysql and mysqldump (directly or via SSH).
 * These tools are needed in <code>FillTempTableAlt</code> to copy data from
 * remote databases into one (preferably local) temporary table. 
 */
public class DBInfo {
    /** Name of the host of the database (in format <code>hostname[:port]</code>). */
    private String host = "";
    /** Name of the user of the database. */
    private String user = "";
    /** Password for the user of the database. */
    private String password = "";
    /** Name of the database. */
    private String name = "";
    /** Connect via SSH (true) or directly (false). */
    private boolean useSSH = false;
    
    /** Creates a new empty instance of DBInfo. */
    public DBInfo() {
    }
    
    /**
     * Creates a new instance of DBInfo.
     * 
     * @param host   Name of the host of the database in format <code>hostname[:port]</code>.
     * @param user   Name of the user of the database.
     * @param name   Name of the database.
     * @param useSSH Connect via SSH (true) or directly (false).
     */
    public DBInfo(String host, String user, String password, String name, boolean useSSH) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.name = name;
        this.useSSH = useSSH;
    }
    
    /**
     * Sets host name.
     * 
     * @param host  Name of the host of the database in format <code>hostname[:port]</code>.
     */
    public void setHost(String host) { if (host!=null) this.host = host; }
    
    /**
     * Sets user name.
     * 
     * @param user  Name of the user of the database.
     */
    public void setUser(String user) { if (user!=null) this.user = user; }
    
    /**
     * Sets password.
     * 
     * @param password  Password of the user of the database.
     */
    public void setPassword(String password) { if (password!=null) this.password = password; }

    /**
     * Sets database name.
     * 
     * @param name  Name of the database.
     */
    public void setName(String name) { if (name!=null) this.name = name; }
    
    /**
     * Sets way to connect to database.
     * 
     * @param useSSH Connect via SSH (true) or directly (false).
     */
    public void setUseSSH(boolean useSSH) { this.useSSH = useSSH; }

    /**
     * Returns host name of the database.
     *
     * @return Host name of the database.
     */
    public String getHost() { return host; }
    
    /**
     * Returns user of the database.
     *
     * @return Name of the user of the database.
     */
    public String getUser() { return user; }
    
    /**
     * Returns password for the user of the database.
     *
     * @return Password of the user of the database.
     */
    public String getPassword() { return password; }

    /**
     * Returns name of the database.
     *
     * @return Name of the database.
     */
    public String getName() { return name; }
    
    /**
     * Returns way to connect to the database.
     *
     * @return True if SSH must be used to connect to the database, false otherwise.
     */
    public boolean isUseSSH() { return useSSH; }
}
