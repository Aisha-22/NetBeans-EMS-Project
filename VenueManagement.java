/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package planner;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;

/**
 *
 * @author Aisha Hlatshwayo
 */
public class VenueManagement {
    private final String venueName;
    private final String venueAddress;
    private final int venueCapacity;
    private final String venueAvailability;

    // Constructor
    public VenueManagement(String venueName, String venueAddress, int venueCapacity, String venueAvailability) {
        this.venueName = venueName;
        this.venueAddress = venueAddress;
        this.venueCapacity = venueCapacity;
        this.venueAvailability = venueAvailability;
    }

    // Getters
    public String getVenueName() {
        return venueName;
    }

    public String getVenueAddress() {
        return venueAddress;
    }

    public int getVenueCapacity() {
        return venueCapacity;
    }

    public String getVenueAvailability() {
        return venueAvailability;
    }

    // Save venue to database
    public boolean saveToDatabase() throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DatabaseHelper.connect();
            String sql = "INSERT INTO venue (venue_name, venue_address, venue_capacity, venue_availability) VALUES (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, venueName);
            pstmt.setString(2, venueAddress);
            pstmt.setInt(3, venueCapacity);
            pstmt.setString(4, venueAvailability);

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseHelper.close(conn, pstmt, null);
        }
    }
}
