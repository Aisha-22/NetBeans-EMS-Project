/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package planner;

import com.sun.jdi.connect.spi.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 *
 * @author Aisha Hlatshwayo
 */
class DatabaseHelper {

    private static final String URL = "jdbc:mysql://localhost:3306/event_planner_system";
    private static final String USER = "root";  
    private static final String PASSWORD = "SuccessDB@2024";  

    public static java.sql.Connection connect() {
        try {
            java.sql.Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected!");
            return conn;
        } catch (java.sql.SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return null;
        }
    }

    static void close(Connection conn, PreparedStatement pstmt, Object object) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    static void close(java.sql.Connection conn, planner.PreparedStatement pstmt, Object object) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    static void close(java.sql.Connection conn, PreparedStatement pstmt, Object object) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    
    }
    
}
