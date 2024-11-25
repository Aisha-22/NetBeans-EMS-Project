/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package planner;

//import com.sun.jdi.connect.spi.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.sql.Statement;

/**
 *
 * @author Aisha Hlatshwayo
 */
public class Home extends javax.swing.JFrame {

    int xx, xy;
//      private javax.swing.JTable tableEvent;
//    private JScrollPane scrollPane; // Instance variable for the scroll pane
    private DefaultTableModel model; // Instance variable for the table model

    public Home() {
        initComponents(); // Initialize GUI components
//        init(); // Perform additional custom setup
        setVisible(true); // Make the frame visible
        initTableMouseListener(); // Add the MouseListener here
        loadVenueData(); // Load venues into the table
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
        return email != null && email.matches(emailRegex);
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        String phoneRegex = "\\d{10}"; // Exactly 10 digits
        return phoneNumber != null && phoneNumber.matches(phoneRegex);
    }

    private boolean isValidDate(Date eventDate) {
        if (eventDate == null) {
            return false; // Null date is invalid
        }
        Date currentDate = new Date();
        return eventDate.after(currentDate); // Ensure the date is in the future
    }

    private void addEvent() {
        String eventName = tfEventName.getText();
//    String eventDateStr = ((JTextField) tfEventDate.getDateEditor().getUiComponent()).getText();
        String eventTime = tfEventTime.getText();
        String description = tfDesription.getText();
        String organiserName = tfOrganiserName.getText();
        String organiserPhone = tfOrganiserPhone.getText();
        String organiserEmail = tfOrganiserEmail.getText();

        // Validate inputs
        if (eventName.isEmpty() || eventTime.isEmpty() || organiserName.isEmpty()
                || organiserPhone.isEmpty() || organiserEmail.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        if (!isValidEmail(organiserEmail)) {
            JOptionPane.showMessageDialog(this, "Invalid email format.");
            return;
        }

        if (!isValidPhoneNumber(organiserPhone)) {
            JOptionPane.showMessageDialog(this, "Invalid phone number. It must be 10 digits.");
            return;
        }

        String eventDateStr;
        try {
            // Place the date validation and formatting code here
            Date eventDate = tfEventDate.getDate(); // Get Date object from JDateChooser
            if (!isValidDate(eventDate)) {
                JOptionPane.showMessageDialog(this, "Event date must be in the future.");
                return;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            eventDateStr = dateFormat.format(eventDate); // Format as "YYYY-MM-DD"
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format.");
            return;
        }

        // Database insertion logic
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = (Connection) DatabaseHelper.connect();
            String sql = "INSERT INTO event (event_name, event_date, event_time, event_description, organiser_name, phone, email) VALUES (?, ?, ?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, eventName);
            pstmt.setString(2, eventDateStr);
            pstmt.setString(3, eventTime);
            pstmt.setString(4, description);
            pstmt.setString(5, organiserName);
            pstmt.setString(6, organiserPhone);
            pstmt.setString(7, organiserEmail);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                JOptionPane.showMessageDialog(this, "Event created successfully!");
                loadEvents(); // Reload table data
            }
        } catch (SQLException ex) {
            ex.printStackTrace();

            // Check for duplicate event name constraint
            if (ex.getSQLState().equals("23000")) { // 23000 is the SQL state for integrity constraint violation
                JOptionPane.showMessageDialog(this, "Error: Event name must be unique. The event name already exists.",
                        "Duplicate Event Name", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error adding event. Please check the input data and try again.",
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        } finally {
            DatabaseHelper.close(conn, pstmt, null);
        }

    }

    private void loadAttendeeData() {
        DefaultTableModel model = (DefaultTableModel) tableAttendee.getModel();
        model.setRowCount(0); // Clear existing rows

        try ( Connection conn = DatabaseHelper.connect()) {
            if (conn != null) {
                String sql = "SELECT attendee_name, attendee_email, attendee_phone, event_selection FROM attendee";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()) {
                    String attendeeName = rs.getString("attendee_name");
                    String attendeeEmail = rs.getString("attendee_email");
                    String attendeePhone = rs.getString("attendee_phone");
                    String eventSelection = rs.getString("event_selection");

                    model.addRow(new Object[]{attendeeName, attendeeEmail, attendeePhone, eventSelection});
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading attendees: " + e.getMessage());
        }
    }

    private void registerVenue() {
        // Collect data from GUI components
        String venueName = tfVenueName.getText();
        String venueAddress = tfVenueAddress.getText();
        int venueCapacity = (int) eventCapacity.getValue();
        String venueAvailability = checkBoxAvailable.isSelected() ? "Available" : "Not Available";

        // Validate inputs
        if (venueName.isEmpty() || venueAddress.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        if (venueCapacity <= 0) {
            JOptionPane.showMessageDialog(this, "Venue capacity must be greater than 0.");
            return;
        }

        // Create VenueManagement object and save to database
        VenueManagement venue = new VenueManagement(venueName, venueAddress, venueCapacity, venueAvailability);
        try {
            if (venue.saveToDatabase()) {
                JOptionPane.showMessageDialog(this, "Venue registered successfully!");
                loadVenueData(); // Refresh venue table
            } else {
                JOptionPane.showMessageDialog(this, "Error registering venue.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadVenueData() {
        DefaultTableModel model = (DefaultTableModel) tableVenue.getModel();
        model.setRowCount(0); // Clear existing rows

        try ( Connection conn = DatabaseHelper.connect()) {
            if (conn != null) {
                String sql = "SELECT venue_name, venue_address, venue_capacity, venue_availability FROM venue";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()) {
                    String venueName = rs.getString("venue_name");
                    String venueAddress = rs.getString("venue_address");
                    int venueCapacity = rs.getInt("venue_capacity");
                    String venueAvailability = rs.getString("venue_availability");

                    model.addRow(new Object[]{venueName, venueAddress, venueCapacity, venueAvailability});
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading venues: " + e.getMessage());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JTabbedPane jTabbedPane = new javax.swing.JTabbedPane();
        javax.swing.JPanel jPanel3 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel4 = new javax.swing.JPanel();
        tfEventName = new javax.swing.JTextField();
        tfEventDate = new com.toedter.calendar.JDateChooser();
        tfEventTime = new javax.swing.JTextField();
        tfOrganiserName = new javax.swing.JTextField();
        tfOrganiserPhone = new javax.swing.JTextField();
        tfOrganiserEmail = new javax.swing.JTextField();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel9 = new javax.swing.JLabel();
        javax.swing.JPanel jPanel6 = new javax.swing.JPanel();
        javax.swing.JButton btnCreate = new javax.swing.JButton();
        tfDesription = new javax.swing.JTextField();
        javax.swing.JPanel jPanel5 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel7 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel10 = new javax.swing.JLabel();
        eventSearchField = new javax.swing.JTextField();
        javax.swing.JButton btnEventSearch = new javax.swing.JButton();
        javax.swing.JPanel jPanel8 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableEvent = new javax.swing.JTable();
        javax.swing.JPanel jPanel9 = new javax.swing.JPanel();
        javax.swing.JButton btnEventView = new javax.swing.JButton();
        javax.swing.JButton btnEventLogout = new javax.swing.JButton();
        javax.swing.JButton btnEventEdit = new javax.swing.JButton();
        javax.swing.JButton btnEventDelete = new javax.swing.JButton();
        javax.swing.JButton btnSave = new javax.swing.JButton();
        javax.swing.JPanel jPanel10 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel11 = new javax.swing.JPanel();
        tfAttendeeName = new javax.swing.JTextField();
        tfAttendeePhone = new javax.swing.JTextField();
        tfAttendeeEmail = new javax.swing.JTextField();
        javax.swing.JLabel jLabel11 = new javax.swing.JLabel();
        comboBoxEventSelection = new javax.swing.JComboBox<>();
        javax.swing.JLabel jLabel12 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel15 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel17 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel18 = new javax.swing.JLabel();
        javax.swing.JPanel jPanel12 = new javax.swing.JPanel();
        javax.swing.JButton btnSignUp = new javax.swing.JButton();
        javax.swing.JPanel jPanel13 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel15 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableAttendee = new javax.swing.JTable();
        javax.swing.JPanel jPanel16 = new javax.swing.JPanel();
        javax.swing.JButton btnAttendeeView = new javax.swing.JButton();
        javax.swing.JButton btnEventLogout1 = new javax.swing.JButton();
        javax.swing.JPanel jPanel17 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel18 = new javax.swing.JPanel();
        tfVenueName = new javax.swing.JTextField();
        tfVenueAddress = new javax.swing.JTextField();
        javax.swing.JLabel jLabel20 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel24 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel25 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel26 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel27 = new javax.swing.JLabel();
        javax.swing.JPanel jPanel19 = new javax.swing.JPanel();
        javax.swing.JButton btnAddVenue = new javax.swing.JButton();
        eventCapacity = new javax.swing.JSpinner();
        checkBoxAvailable = new javax.swing.JCheckBox();
        checkBoxNotAvailable = new javax.swing.JCheckBox();
        javax.swing.JPanel jPanel20 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel21 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel28 = new javax.swing.JLabel();
        venueSearchField = new javax.swing.JTextField();
        javax.swing.JButton btnSearchVenue = new javax.swing.JButton();
        javax.swing.JPanel jPanel22 = new javax.swing.JPanel();
        javax.swing.JScrollPane jScrollPane3 = new javax.swing.JScrollPane();
        tableVenue = new javax.swing.JTable();
        javax.swing.JPanel jPanel23 = new javax.swing.JPanel();
        javax.swing.JButton btnViewVenue = new javax.swing.JButton();
        javax.swing.JButton btnDeleteVenue = new javax.swing.JButton();
        javax.swing.JButton btnEditVenue = new javax.swing.JButton();
        javax.swing.JButton btnEventLogout2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(102, 204, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jPanel2.setBackground(new java.awt.Color(153, 255, 255));
        jPanel2.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jPanel2MouseDragged(evt);
            }
        });
        jPanel2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jPanel2MousePressed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 40)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 0, 0));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("EVENT MANAGEMENT SYSTEM");
        jLabel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        jTabbedPane.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N

        jPanel3.setBackground(new java.awt.Color(153, 255, 255));

        jPanel4.setBackground(new java.awt.Color(102, 204, 255));
        jPanel4.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        tfEventName.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        tfEventDate.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        tfEventTime.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        tfOrganiserName.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        tfOrganiserPhone.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        tfOrganiserEmail.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        jLabel2.setFont(new java.awt.Font("Times New Roman", 1, 24)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(0, 0, 0));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Organiser Details");

        jLabel3.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 0, 0));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Event Name");

        jLabel4.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(0, 0, 0));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Event Date");

        jLabel5.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(0, 0, 0));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Event Time");

        jLabel6.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(0, 0, 0));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Event Description");

        jLabel7.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(0, 0, 0));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Name");

        jLabel8.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(0, 0, 0));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Phone Number");

        jLabel9.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(0, 0, 0));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("Email");

        jPanel6.setBackground(new java.awt.Color(102, 204, 255));
        jPanel6.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        btnCreate.setBackground(new java.awt.Color(153, 255, 255));
        btnCreate.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnCreate.setForeground(new java.awt.Color(0, 0, 0));
        btnCreate.setText("Create");
        btnCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(197, 197, 197)
                .addComponent(btnCreate, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(64, 64, 64)
                .addComponent(btnCreate, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tfDesription.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tfOrganiserName, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tfEventName)
                            .addComponent(tfEventDate, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
                            .addComponent(tfEventTime)
                            .addComponent(tfOrganiserPhone)
                            .addComponent(tfOrganiserEmail)
                            .addComponent(tfDesription, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfEventName, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tfEventDate, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfEventTime, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(tfDesription, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addComponent(jLabel2)
                .addGap(30, 30, 30)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfOrganiserName, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfOrganiserPhone, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfOrganiserEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addGap(18, 18, 18)
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBackground(new java.awt.Color(102, 204, 255));
        jPanel5.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        jPanel7.setBackground(new java.awt.Color(102, 204, 255));
        jPanel7.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        jLabel10.setFont(new java.awt.Font("Times New Roman", 1, 24)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(0, 0, 0));
        jLabel10.setText("Search Event");

        eventSearchField.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N

        btnEventSearch.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnEventSearch.setText("Search");
        btnEventSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEventSearchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(eventSearchField, javax.swing.GroupLayout.PREFERRED_SIZE, 402, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE)
                .addComponent(btnEventSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(41, 41, 41))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)
                    .addComponent(eventSearchField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEventSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel8.setBackground(new java.awt.Color(102, 204, 255));
        jPanel8.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        tableEvent.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Event Name", "Event Date", "Event Time", "Event Description", "Organiser Name", "Organiser Phone Number", "Organiser Email"
            }
        ));
        jScrollPane1.setViewportView(tableEvent);

        jPanel9.setBackground(new java.awt.Color(102, 204, 255));
        jPanel9.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        btnEventView.setBackground(new java.awt.Color(153, 255, 255));
        btnEventView.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnEventView.setForeground(new java.awt.Color(0, 0, 0));
        btnEventView.setText("View");
        btnEventView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEventViewActionPerformed(evt);
            }
        });

        btnEventLogout.setBackground(new java.awt.Color(153, 255, 255));
        btnEventLogout.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnEventLogout.setForeground(new java.awt.Color(0, 0, 0));
        btnEventLogout.setText("Logout");
        btnEventLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEventLogoutActionPerformed(evt);
            }
        });

        btnEventEdit.setBackground(new java.awt.Color(153, 255, 255));
        btnEventEdit.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnEventEdit.setForeground(new java.awt.Color(0, 0, 0));
        btnEventEdit.setText("Edit");
        btnEventEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEventEditActionPerformed(evt);
            }
        });

        btnEventDelete.setBackground(new java.awt.Color(153, 255, 255));
        btnEventDelete.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnEventDelete.setForeground(new java.awt.Color(0, 0, 0));
        btnEventDelete.setText("Delete");
        btnEventDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEventDeleteActionPerformed(evt);
            }
        });

        btnSave.setBackground(new java.awt.Color(153, 255, 255));
        btnSave.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnSave.setForeground(new java.awt.Color(0, 0, 0));
        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(btnEventEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(68, 68, 68)
                .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(58, 58, 58)
                .addComponent(btnEventView, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnEventDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(60, 60, 60)
                .addComponent(btnEventLogout, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap(31, Short.MAX_VALUE)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEventView, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEventLogout, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEventEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEventDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(29, 29, 29))
        );

        jPanel9Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {btnEventEdit, btnEventLogout, btnEventView});

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane.addTab("Event Planning", jPanel3);

        jPanel10.setBackground(new java.awt.Color(153, 255, 255));

        jPanel11.setBackground(new java.awt.Color(102, 204, 255));
        jPanel11.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        tfAttendeeName.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        tfAttendeePhone.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        tfAttendeeEmail.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        jLabel11.setFont(new java.awt.Font("Times New Roman", 1, 24)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(0, 0, 0));
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("Attendee Registration");

        comboBoxEventSelection.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N
        comboBoxEventSelection.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Wedding", "Conference", "Music Festival", "Birthday Celebration" }));

        jLabel12.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(0, 0, 0));
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("Attendee Name");

        jLabel15.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(0, 0, 0));
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setText("Event Selection");

        jLabel17.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(0, 0, 0));
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel17.setText("Phone Number");

        jLabel18.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(0, 0, 0));
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel18.setText("Attendee Email");

        jPanel12.setBackground(new java.awt.Color(102, 204, 255));
        jPanel12.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        btnSignUp.setBackground(new java.awt.Color(153, 255, 255));
        btnSignUp.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnSignUp.setForeground(new java.awt.Color(0, 0, 0));
        btnSignUp.setText("Sign Up");
        btnSignUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSignUpActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addGap(193, 193, 193)
                .addComponent(btnSignUp, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addGap(68, 68, 68)
                .addComponent(btnSignUp, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(80, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tfAttendeeName)
                            .addComponent(tfAttendeePhone)
                            .addComponent(tfAttendeeEmail)
                            .addComponent(comboBoxEventSelection, javax.swing.GroupLayout.Alignment.TRAILING, 0, 421, Short.MAX_VALUE)))
                    .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11)
                .addGap(18, 18, 18)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfAttendeeName, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addGap(50, 50, 50)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfAttendeeEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18))
                .addGap(51, 51, 51)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfAttendeePhone, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17))
                .addGap(58, 58, 58)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboBoxEventSelection, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addGap(73, 73, 73)
                .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel13.setBackground(new java.awt.Color(102, 204, 255));
        jPanel13.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        jPanel15.setBackground(new java.awt.Color(102, 204, 255));
        jPanel15.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        tableAttendee.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Attendee Name", "Attendee Email", "Phone Number", "Event Selection"
            }
        ));
        jScrollPane2.setViewportView(tableAttendee);
        if (tableAttendee.getColumnModel().getColumnCount() > 0) {
            tableAttendee.getColumnModel().getColumn(2).setHeaderValue("Organiser Name");
            tableAttendee.getColumnModel().getColumn(3).setHeaderValue("Organiser Phone Number");
        }

        jPanel16.setBackground(new java.awt.Color(102, 204, 255));
        jPanel16.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        btnAttendeeView.setBackground(new java.awt.Color(153, 255, 255));
        btnAttendeeView.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnAttendeeView.setForeground(new java.awt.Color(0, 0, 0));
        btnAttendeeView.setText("View");
        btnAttendeeView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAttendeeViewActionPerformed(evt);
            }
        });

        btnEventLogout1.setBackground(new java.awt.Color(153, 255, 255));
        btnEventLogout1.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnEventLogout1.setForeground(new java.awt.Color(0, 0, 0));
        btnEventLogout1.setText("Logout");
        btnEventLogout1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEventLogout1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(90, 90, 90)
                .addComponent(btnAttendeeView, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnEventLogout1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(116, 116, 116))
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEventLogout1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAttendeeView, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(27, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 790, Short.MAX_VALUE)
                    .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addGap(18, 18, 18)
                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane.addTab("Attendee Registration", jPanel10);

        jPanel17.setBackground(new java.awt.Color(153, 255, 255));

        jPanel18.setBackground(new java.awt.Color(102, 204, 255));
        jPanel18.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        tfVenueName.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        tfVenueAddress.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        jLabel20.setFont(new java.awt.Font("Times New Roman", 1, 24)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(0, 0, 0));
        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel20.setText("Venue Management");

        jLabel24.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(0, 0, 0));
        jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel24.setText("Availability");

        jLabel25.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel25.setForeground(new java.awt.Color(0, 0, 0));
        jLabel25.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel25.setText("Venue Name");

        jLabel26.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel26.setForeground(new java.awt.Color(0, 0, 0));
        jLabel26.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel26.setText("Capacity");

        jLabel27.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        jLabel27.setForeground(new java.awt.Color(0, 0, 0));
        jLabel27.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel27.setText("Venue Address");

        jPanel19.setBackground(new java.awt.Color(102, 204, 255));
        jPanel19.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        btnAddVenue.setBackground(new java.awt.Color(153, 255, 255));
        btnAddVenue.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnAddVenue.setForeground(new java.awt.Color(0, 0, 0));
        btnAddVenue.setText("Add Venue");
        btnAddVenue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddVenueActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addGap(173, 173, 173)
                .addComponent(btnAddVenue)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addComponent(btnAddVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        eventCapacity.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N

        checkBoxAvailable.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        checkBoxAvailable.setForeground(new java.awt.Color(0, 0, 0));
        checkBoxAvailable.setText("Available");

        checkBoxNotAvailable.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        checkBoxNotAvailable.setForeground(new java.awt.Color(0, 0, 0));
        checkBoxNotAvailable.setText("Not Available");

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel18Layout.createSequentialGroup()
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel20, javax.swing.GroupLayout.DEFAULT_SIZE, 534, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel18Layout.createSequentialGroup()
                        .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkBoxAvailable)
                        .addGap(138, 138, 138)
                        .addComponent(checkBoxNotAvailable)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel18Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel18Layout.createSequentialGroup()
                                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel25, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel27, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(tfVenueName, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(tfVenueAddress)
                                    .addGroup(jPanel18Layout.createSequentialGroup()
                                        .addComponent(eventCapacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE)))))))
                .addContainerGap())
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel20)
                .addGap(35, 35, 35)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfVenueName, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel25))
                .addGap(47, 47, 47)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfVenueAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27))
                .addGap(61, 61, 61)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(eventCapacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26))
                .addGap(55, 55, 55)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(checkBoxAvailable)
                    .addComponent(checkBoxNotAvailable))
                .addGap(66, 66, 66)
                .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel20.setBackground(new java.awt.Color(102, 204, 255));
        jPanel20.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        jPanel21.setBackground(new java.awt.Color(102, 204, 255));
        jPanel21.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        jLabel28.setFont(new java.awt.Font("Times New Roman", 1, 24)); // NOI18N
        jLabel28.setForeground(new java.awt.Color(0, 0, 0));
        jLabel28.setText("Search Venue");

        venueSearchField.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N

        btnSearchVenue.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnSearchVenue.setText("Search");
        btnSearchVenue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchVenueActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel21Layout = new javax.swing.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel21Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(venueSearchField, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 43, Short.MAX_VALUE)
                .addComponent(btnSearchVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(38, 38, 38))
        );
        jPanel21Layout.setVerticalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel21Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel28, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)
                    .addComponent(venueSearchField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearchVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel22.setBackground(new java.awt.Color(102, 204, 255));
        jPanel22.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        tableVenue.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Venue Name", "Venue Address", "Capacity", "Availability"
            }
        ));
        jScrollPane3.setViewportView(tableVenue);

        jPanel23.setBackground(new java.awt.Color(102, 204, 255));
        jPanel23.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 255, 255), 4, true));

        btnViewVenue.setBackground(new java.awt.Color(153, 255, 255));
        btnViewVenue.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnViewVenue.setForeground(new java.awt.Color(0, 0, 0));
        btnViewVenue.setText("View");
        btnViewVenue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewVenueActionPerformed(evt);
            }
        });

        btnDeleteVenue.setBackground(new java.awt.Color(153, 255, 255));
        btnDeleteVenue.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnDeleteVenue.setForeground(new java.awt.Color(0, 0, 0));
        btnDeleteVenue.setText("Delete");
        btnDeleteVenue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteVenueActionPerformed(evt);
            }
        });

        btnEditVenue.setBackground(new java.awt.Color(153, 255, 255));
        btnEditVenue.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnEditVenue.setForeground(new java.awt.Color(0, 0, 0));
        btnEditVenue.setText("Edit");
        btnEditVenue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditVenueActionPerformed(evt);
            }
        });

        btnEventLogout2.setBackground(new java.awt.Color(153, 255, 255));
        btnEventLogout2.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        btnEventLogout2.setForeground(new java.awt.Color(0, 0, 0));
        btnEventLogout2.setText("Logout");
        btnEventLogout2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEventLogout2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel23Layout = new javax.swing.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel23Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(btnEditVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(78, 78, 78)
                .addComponent(btnViewVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnDeleteVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(67, 67, 67)
                .addComponent(btnEventLogout2, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel23Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btnDeleteVenue, btnEditVenue, btnViewVenue});

        jPanel23Layout.setVerticalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel23Layout.createSequentialGroup()
                .addContainerGap(31, Short.MAX_VALUE)
                .addGroup(jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnViewVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDeleteVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEditVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEventLogout2, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(29, 29, 29))
        );

        jPanel23Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {btnDeleteVenue, btnEditVenue, btnViewVenue});

        javax.swing.GroupLayout jPanel22Layout = new javax.swing.GroupLayout(jPanel22);
        jPanel22.setLayout(jPanel22Layout);
        jPanel22Layout.setHorizontalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel22Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addComponent(jPanel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel22Layout.setVerticalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel22Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel20Layout = new javax.swing.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel20Layout.setVerticalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane.addTab("Venue Management", jPanel17);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTabbedPane))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents


    private void btnEventLogout2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEventLogout2ActionPerformed
        int a = JOptionPane.showConfirmDialog(this, "Do you want to logout now?", "Select", JOptionPane.YES_NO_OPTION);
        if (a == 0) {

            this.dispose();
        }

    }//GEN-LAST:event_btnEventLogout2ActionPerformed

    private void btnEventLogout1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEventLogout1ActionPerformed
        int a = JOptionPane.showConfirmDialog(this, "Do you want to logout now?", "Select", JOptionPane.YES_NO_OPTION);
        if (a == 0) {

            this.dispose();
        }
    }//GEN-LAST:event_btnEventLogout1ActionPerformed

    private void btnEventLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEventLogoutActionPerformed
        int a = JOptionPane.showConfirmDialog(this, "Do you want to logout now?", "Select", JOptionPane.YES_NO_OPTION);
        if (a == 0) {

            this.dispose();
        }
    }//GEN-LAST:event_btnEventLogoutActionPerformed

    private void jPanel2MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel2MouseDragged
        int x = evt.getXOnScreen();
        int y = evt.getYOnScreen();
        this.setLocation(x - xx, y - xy);
    }//GEN-LAST:event_jPanel2MouseDragged

    private void jPanel2MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel2MousePressed
        xx = evt.getX();
        xy = evt.getY();
    }//GEN-LAST:event_jPanel2MousePressed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        for (double i = 0.1; i <= 1.0; i += 0.1) {
            String s = i + "";
            float f = Float.valueOf(s);
            this.setOpacity(f);
            try {
                Thread.sleep(40);
            } catch (InterruptedException ex) {
                Logger.getLogger(Home.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }//GEN-LAST:event_formWindowOpened

    private void btnCreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateActionPerformed
        addEvent();
    }//GEN-LAST:event_btnCreateActionPerformed

    private void btnEventViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEventViewActionPerformed
        // Get the table's model
        DefaultTableModel model = (DefaultTableModel) tableEvent.getModel();
        model.setRowCount(0);  // Clear existing rows

        // Establish database connection and retrieve data
        try ( Connection conn = DatabaseHelper.connect()) {
            if (conn != null) {
                String sql = "SELECT event_name, event_date, event_time, event_description, organiser_name, phone, email FROM event";
                Statement stmt = conn.createStatement(); // Correct Statement usage
                ResultSet rs = stmt.executeQuery(sql);

                // Process the result set
                while (rs.next()) {
                    String eventName = rs.getString("event_name");
                    String eventDate = rs.getString("event_date");
                    String eventTime = rs.getString("event_time");
                    String description = rs.getString("event_description");
                    String organiserName = rs.getString("organiser_name");
                    String phone = rs.getString("phone");
                    String email = rs.getString("email");

                    // Add the data to the table model
                    model.addRow(new Object[]{eventName, eventDate, eventTime, description, organiserName, phone, email});
                }

                JOptionPane.showMessageDialog(this, "Data loaded successfully!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error retrieving data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }

    }//GEN-LAST:event_btnEventViewActionPerformed

    // Attach a MouseListener to tableEvent
    private void initTableMouseListener() {
        tableEvent.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int selectedRow = tableEvent.getSelectedRow();
                if (selectedRow != -1) { // Check if a row is selected
                    String eventName = tableEvent.getValueAt(selectedRow, 0).toString(); // First column (event_name)
                    String eventDate = tableEvent.getValueAt(selectedRow, 1).toString(); // Second column (event_date)
                    // Display the selected event details
                    JOptionPane.showMessageDialog(null, "Selected Event: " + eventName + ", Date: " + eventDate);
                }
            }
        });
    }

    private void btnSignUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSignUpActionPerformed
        try {
            registerAttendee();
        } catch (SQLException ex) {
            Logger.getLogger(Home.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btnSignUpActionPerformed

    private void btnAttendeeViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAttendeeViewActionPerformed
        // Get the table's model
        DefaultTableModel model = (DefaultTableModel) tableAttendee.getModel();
        model.setRowCount(0);  // Clear existing rows

        // Establish database connection and retrieve data
        try ( Connection conn = DatabaseHelper.connect()) {
            if (conn != null) {
                String sql = "SELECT attendee_name, attendee_email, attendee_phone, event_selection FROM attendee";
                Statement stmt = conn.createStatement(); // Create a Statement object
                ResultSet rs = stmt.executeQuery(sql);

                // Process the result set
                while (rs.next()) {
                    String attendeeName = rs.getString("attendee_name");
                    String attendeeEmail = rs.getString("attendee_email");
                    String attendeePhone = rs.getString("attendee_phone");
                    String eventSelection = rs.getString("event_selection");

                    // Add the data to the table model
                    model.addRow(new Object[]{attendeeName, attendeeEmail, attendeePhone, eventSelection});
                }

                JOptionPane.showMessageDialog(this, "Data loaded successfully!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error retrieving data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnAttendeeViewActionPerformed

    private void btnEventDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEventDeleteActionPerformed
        int selectedRow = tableEvent.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.");
            return;
        }

        // Get the unique identifier (e.g., event_name) of the selected row
        String eventName = tableEvent.getValueAt(selectedRow, 0).toString(); // First column (event_name)

        // Confirm deletion
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the selected event?",
                "Delete Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            // Perform deletion in the database
            try ( Connection conn = DatabaseHelper.connect()) {
                String sql = "DELETE FROM event WHERE event_name = ?";
                try ( PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, eventName);
                    int rowsDeleted = pstmt.executeUpdate();
                    if (rowsDeleted > 0) {
                        JOptionPane.showMessageDialog(this, "Event deleted successfully!");
                        ((DefaultTableModel) tableEvent.getModel()).removeRow(selectedRow); // Remove from table
                    } else {
                        JOptionPane.showMessageDialog(this, "Error: Event not found in database.");
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting event: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

    }//GEN-LAST:event_btnEventDeleteActionPerformed

    private void btnAddVenueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddVenueActionPerformed
        registerVenue();
    }//GEN-LAST:event_btnAddVenueActionPerformed

    private void btnDeleteVenueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteVenueActionPerformed
        int selectedRow = tableVenue.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.");
            return;
        }

        // Get the unique identifier (e.g., venue_name) of the selected row
        String venueName = tableVenue.getValueAt(selectedRow, 0).toString(); // First column (venue_name)

        // Confirm deletion
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the selected venue?",
                "Delete Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            // Perform deletion in the database
            try ( Connection conn = DatabaseHelper.connect()) {
                String sql = "DELETE FROM venue WHERE venue_name = ?";
                try ( PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, venueName);
                    int rowsDeleted = pstmt.executeUpdate();
                    if (rowsDeleted > 0) {
                        JOptionPane.showMessageDialog(this, "Venue deleted successfully!");
                        ((DefaultTableModel) tableVenue.getModel()).removeRow(selectedRow); // Remove from table
                    } else {
                        JOptionPane.showMessageDialog(this, "Error: Venue not found in database.");
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting venue: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnDeleteVenueActionPerformed

    private void btnViewVenueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewVenueActionPerformed
        // Get the table's model
        DefaultTableModel model = (DefaultTableModel) tableVenue.getModel();
        model.setRowCount(0);  // Clear existing rows

        // Establish database connection and retrieve data
        try ( Connection conn = DatabaseHelper.connect()) {
            if (conn != null) {
                String sql = "SELECT venue_name, venue_address, venue_capacity, venue_availability FROM venue";
                Statement stmt = conn.createStatement(); // Create a Statement object
                ResultSet rs = stmt.executeQuery(sql);

                // Process the result set
                while (rs.next()) {
                    String venueName = rs.getString("venue_name");
                    String venueAddress = rs.getString("venue_address");
                    int venueCapacity = rs.getInt("venue_capacity");
                    String venueAvailability = rs.getString("venue_availability");

                    // Add the data to the table model
                    model.addRow(new Object[]{venueName, venueAddress, venueCapacity, venueAvailability});
                }

                JOptionPane.showMessageDialog(this, "Data loaded successfully!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error retrieving data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnViewVenueActionPerformed

    private void btnEditVenueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditVenueActionPerformed
        // Get the selected row from tableVenue
    int selectedRow = tableVenue.getSelectedRow();
    if (selectedRow == -1) {  // If no row is selected
        JOptionPane.showMessageDialog(this, "Please select a venue to edit.");
        return;
    }

    // Enable editing for the selected row
    tableVenue.editCellAt(selectedRow, 0); // Allow editing the first column as an example
    JOptionPane.showMessageDialog(this, "You can edit the data directly in the table. After making changes, click Save.");

    // Assuming a "Save" button is used to trigger saving changes
    btnSave.addActionListener(e -> {
        try {
            // Retrieve updated data from the table
            String venueName = tableVenue.getValueAt(selectedRow, 0).toString();
            String venueAddress = tableVenue.getValueAt(selectedRow, 1).toString();
            int venueCapacity = Integer.parseInt(tableVenue.getValueAt(selectedRow, 2).toString());
            String venueAvailability = tableVenue.getValueAt(selectedRow, 3).toString();

            // Database update query
            String updateQuery = "UPDATE Venue SET venueName = ?, venueAddress = ?, venueCapacity = ?, venueAvailability = ? WHERE venueName = ?";
            try (PreparedStatement pstmt = dbConnection.prepareStatement(updateQuery)) {
                pstmt.setString(1, venueName);
                pstmt.setString(2, venueAddress);
                pstmt.setInt(3, venueCapacity);
                pstmt.setString(4, venueAvailability);
                pstmt.setString(5, venueName); // Use the original venue name as the identifier
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Venue details updated successfully.");
                } else {
                    JOptionPane.showMessageDialog(this, "Error updating venue details. Ensure the venue exists.");
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving changes: " + ex.getMessage());
        }
    });
    }//GEN-LAST:event_btnEditVenueActionPerformed

    private void btnEventEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEventEditActionPerformed
        // Get the selected row from the table
    int selectedRow = tableEvent.getSelectedRow();

    // Check if a row is selected
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select an event to edit.", "No Event Selected", JOptionPane.WARNING_MESSAGE);
        return;
    }

    // Make the table editable for the selected row
    tableEvent.setCellSelectionEnabled(true); // Enable cell selection
    tableEvent.editCellAt(selectedRow, 0);    // Start editing at the first column of the selected row

    // Add a listener to capture when editing stops
    tableEvent.getCellEditor().addCellEditorListener(new javax.swing.event.CellEditorListener() {
        @Override
        public void editingStopped(javax.swing.event.ChangeEvent e) {
            // Fetch the updated row data after editing
            String eventName = tableEvent.getValueAt(selectedRow, 0).toString(); // Event Name
            String eventDate = tableEvent.getValueAt(selectedRow, 1).toString(); // Event Date
            String eventTime = tableEvent.getValueAt(selectedRow, 2).toString(); // Event Time
            String eventDescription = tableEvent.getValueAt(selectedRow, 3).toString(); // Event Description
            String organiserName = tableEvent.getValueAt(selectedRow, 4).toString(); // Organiser Name
            String organiserPhone = tableEvent.getValueAt(selectedRow, 5).toString(); // Organiser Phone
            String organiserEmail = tableEvent.getValueAt(selectedRow, 6).toString(); // Organiser Email

            // Save the updated row data to the database
            saveToDatabase(eventName, eventDate, eventTime, eventDescription, organiserName, organiserPhone, organiserEmail);

            // Notify the user of success
            JOptionPane.showMessageDialog(null, "Event details updated successfully in the database!", "Update Success", JOptionPane.INFORMATION_MESSAGE);
        }

        @Override
        public void editingCanceled(javax.swing.event.ChangeEvent e) {
            JOptionPane.showMessageDialog(null, "Editing cancelled.", "Edit Cancelled", JOptionPane.WARNING_MESSAGE);
        }
    });
}

// Method to save the updated data to the database
private void saveToDatabase(String eventName, String eventDate, String eventTime, String eventDescription, 
                            String organiserName, String organiserPhone, String organiserEmail) {
    try {
        // Assuming you have a connection object 'conn' to the database
        String sql = "UPDATE events SET event_date = ?, event_time = ?, event_description = ?, organiser_name = ?, organiser_phone = ?, organiser_email = ? WHERE event_name = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, eventDate);
        stmt.setString(2, eventTime);
        stmt.setString(3, eventDescription);
        stmt.setString(4, organiserName);
        stmt.setString(5, organiserPhone);
        stmt.setString(6, organiserEmail);
        stmt.setString(7, eventName); // Assuming event_name is a unique identifier in the table

        int rowsUpdated = stmt.executeUpdate();
        if (rowsUpdated > 0) {
            System.out.println("Database updated successfully for event: " + eventName);
        }
        stmt.close();
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Error updating the database: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_btnEventEditActionPerformed

    
    
    private void btnEventSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEventSearchActionPerformed
        // Get the search query from the search field
        String searchQuery = eventSearchField.getText().trim();

        if (searchQuery.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get the table's model
        DefaultTableModel model = (DefaultTableModel) tableEvent.getModel();
        model.setRowCount(0);  // Clear existing rows

        // Establish database connection and retrieve data based on the search query
        try ( Connection conn = DatabaseHelper.connect()) {
            if (conn != null) {
                String sql = "SELECT event_name, event_date, event_time, event_description, organiser_name, phone, email "
                        + "FROM event WHERE event_name LIKE ? OR organiser_name LIKE ? OR event_description LIKE ?";

                try ( PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    // Set the parameters for the query
                    pstmt.setString(1, "%" + searchQuery + "%"); // Search for event name
                    pstmt.setString(2, "%" + searchQuery + "%"); // Search for organiser name
                    pstmt.setString(3, "%" + searchQuery + "%"); // Search for event description

                    ResultSet rs = pstmt.executeQuery();

                    // Process the result set and update the table
                    while (rs.next()) {
                        String eventName = rs.getString("event_name");
                        String eventDate = rs.getString("event_date");
                        String eventTime = rs.getString("event_time");
                        String description = rs.getString("event_description");
                        String organiserName = rs.getString("organiser_name");
                        String phone = rs.getString("phone");
                        String email = rs.getString("email");

                        // Add the data to the table model
                        model.addRow(new Object[]{eventName, eventDate, eventTime, description, organiserName, phone, email});
                    }

                    if (model.getRowCount() == 0) {
                        JOptionPane.showMessageDialog(this, "No events found matching the search criteria.", "Search Results", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Data loaded successfully!");
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error retrieving data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnEventSearchActionPerformed

    private void btnSearchVenueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchVenueActionPerformed
        String searchQuery = venueSearchField.getText().trim(); // Get text from search field

        // If the search field is empty, show all venues
        if (searchQuery.isEmpty()) {
            loadVenueData(); // Reload all venues
            return;
        }

        // Get the table's model
        DefaultTableModel model = (DefaultTableModel) tableVenue.getModel();
        model.setRowCount(0);  // Clear existing rows

        // Establish database connection and retrieve data based on search query
        try ( Connection conn = DatabaseHelper.connect()) {
            if (conn != null) {
                // SQL query with a wildcard to match venue name or address with the search input
                String sql = "SELECT venue_name, venue_address, venue_capacity, venue_availability FROM venue "
                        + "WHERE venue_name LIKE ? OR venue_address LIKE ?";
                try ( PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    // Use the searchQuery with wildcard (%) for partial matching
                    pstmt.setString(1, "%" + searchQuery + "%");
                    pstmt.setString(2, "%" + searchQuery + "%");

                    ResultSet rs = pstmt.executeQuery();

                    // Process the result set
                    while (rs.next()) {
                        String venueName = rs.getString("venue_name");
                        String venueAddress = rs.getString("venue_address");
                        int venueCapacity = rs.getInt("venue_capacity");
                        String venueAvailability = rs.getString("venue_availability");

                        // Add the data to the table model
                        model.addRow(new Object[]{venueName, venueAddress, venueCapacity, venueAvailability});
                    }

                    JOptionPane.showMessageDialog(this, "Data loaded successfully!");

                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error retrieving data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error connecting to database: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSearchVenueActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSaveActionPerformed

    private void registerAttendee() throws SQLException {
        // Collect data from GUI components
        String attendeeName = tfAttendeeName.getText();
        String attendeeEmail = tfAttendeeEmail.getText();
        String attendeePhone = tfAttendeePhone.getText();
        String eventSelection = (String) comboBoxEventSelection.getSelectedItem();

        // Validate inputs
        if (attendeeName.isEmpty() || attendeeEmail.isEmpty() || attendeePhone.isEmpty() || eventSelection.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        if (!isValidEmail(attendeeEmail)) {
            JOptionPane.showMessageDialog(this, "Invalid email format.");
            return;
        }

        if (!isValidPhoneNumber(attendeePhone)) {
            JOptionPane.showMessageDialog(this, "Invalid phone number. It must be between 10 and 15 digits.");
            return;
        }

        // Create AttendeeRegistration object and save to database
        AttendeeRegistration attendee = new AttendeeRegistration(attendeeName, attendeeEmail, attendeePhone, eventSelection);
        if (attendee.saveToDatabase()) {
            JOptionPane.showMessageDialog(this, "Attendee registered successfully!");
            loadAttendeeData(); // Refresh attendee table
        } else {
            JOptionPane.showMessageDialog(this, "Error registering attendee.");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
//        java.awt.EventQueue.invokeLater(() -> {
//            // Show Login screen first
//            Login login = new Login();
//            login.setVisible(true);
////        });

        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Home().setVisible(true);
            }
        });

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox checkBoxAvailable;
    private javax.swing.JCheckBox checkBoxNotAvailable;
    private javax.swing.JComboBox<String> comboBoxEventSelection;
    private javax.swing.JSpinner eventCapacity;
    private javax.swing.JTextField eventSearchField;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tableAttendee;
    private javax.swing.JTable tableEvent;
    private javax.swing.JTable tableVenue;
    private javax.swing.JTextField tfAttendeeEmail;
    private javax.swing.JTextField tfAttendeeName;
    private javax.swing.JTextField tfAttendeePhone;
    private javax.swing.JTextField tfDesription;
    private com.toedter.calendar.JDateChooser tfEventDate;
    private javax.swing.JTextField tfEventName;
    private javax.swing.JTextField tfEventTime;
    private javax.swing.JTextField tfOrganiserEmail;
    private javax.swing.JTextField tfOrganiserName;
    private javax.swing.JTextField tfOrganiserPhone;
    private javax.swing.JTextField tfVenueAddress;
    private javax.swing.JTextField tfVenueName;
    private javax.swing.JTextField venueSearchField;
    // End of variables declaration//GEN-END:variables

    private JTable findTableEventFromGeneratedCode() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void loadEvents() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static class conn {

        private static PreparedStatement prepareStatement(String sql) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        public conn() {
        }
    }

    private static class btnSave {

        public btnSave() {
        }
    }

    private static class dbConnection {

        private static PreparedStatement prepareStatement(String updateQuery) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        public dbConnection() {
        }
    }

}
