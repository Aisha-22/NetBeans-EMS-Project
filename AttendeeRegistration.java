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
public class AttendeeRegistration {
    private final String attendeeName;
    private final String attendeeEmail;
    private final String attendeePhone;
    private final String eventSelection;

    // Constructor
    public AttendeeRegistration(String attendeeName, String attendeeEmail, String attendeePhone, String eventSelection) {
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.attendeePhone = attendeePhone;
        this.eventSelection = eventSelection;
    }

    // Getters
    public String getAttendeeName() {
        return attendeeName;
    }

    public String getAttendeeEmail() {
        return attendeeEmail;
    }

    public String getAttendeePhone() {
        return attendeePhone;
    }

    public String getEventSelection() {
        return eventSelection;
    }

    // Database Insertion Logic
    public boolean saveToDatabase() throws java.sql.SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DatabaseHelper.connect();
            String sql = "INSERT INTO attendee (attendee_name, attendee_email, attendee_phone, event_selection) VALUES (?, ?, ?, ?)";
            pstmt = (PreparedStatement) conn.prepareStatement(sql);
            pstmt.setString(1, attendeeName);
            pstmt.setString(2, attendeeEmail);
            pstmt.setString(3, attendeePhone);
            pstmt.setString(4, eventSelection);

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
