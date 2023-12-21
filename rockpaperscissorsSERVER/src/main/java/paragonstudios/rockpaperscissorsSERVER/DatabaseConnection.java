package paragonstudios.rockpaperscissorsSERVER;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @Don
 */
public class DatabaseConnection {
    
    public Connection databaseLink;
    
    public boolean valid;
    
    String databaseName = "rps";
    String databaseUser = "root";
    String databasePassword = "password";
    String url = "jdbc:mysql://localhost:3306/"+databaseName+"?autoReconnect=true"; 
    // I have used MySQL for database storing account info. Very simple database, only one schema "UserAccounts"
    // that contains following fields - username, password, totalWins

    
    public Connection getConnection(){

        try{
            databaseLink = DriverManager.getConnection(url,databaseUser,databasePassword);
            System.out.println("Connected to database");
            valid = true;
        } catch(SQLException e) {
            e.printStackTrace();
            valid = false;
        }
        return databaseLink;
    }
    
    public boolean isValid(){
        try{
            databaseLink = DriverManager.getConnection(url,databaseUser,databasePassword);
            valid = true;
        } catch(SQLException e) {
            valid = false;
        }
        return valid;
        
    }
}
