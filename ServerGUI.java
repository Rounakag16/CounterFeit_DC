import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class ServerGUI {

    private static DefaultTableModel tableModel;
    private static JLabel totalLbl, authLbl, fraudLbl;
    private static CounterfeitServiceImpl service;
    private static JFrame frame;

    // --- NEON & MINIMALIST COLOR PALETTE ---
    private static final Color BG_COLOR = new Color(10, 10, 10);
    private static final Color CARD_COLOR = new Color(25, 25, 25);
    private static final Color ACCENT_COLOR = new Color(0, 255, 255);
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final Color TEXT_DARK = new Color(10, 10, 10);

    // Status Colors
    private static final Color NEON_GREEN = new Color(57, 255, 20);
    private static final Color NEON_YELLOW = new Color(255, 222, 33);
    private static final Color NEON_RED = new Color(255, 49, 49);

    public static void main(String[] args) {
        String myIp = JOptionPane.showInputDialog("Enter Server IP:", "10.85.228.240");
        if (myIp == null)
            System.exit(0);
        SwingUtilities.invokeLater(() -> {
            setupGUI(myIp);
            startServer(myIp);
        });
    }

    private static void setupGUI(String ip) {
        frame = new JFrame();
        frame.setSize(950, 650);
        frame.setUndecorated(true);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(BG_COLOR);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(BG_COLOR);
        titleBar.setBorder(new EmptyBorder(10, 15, 10, 15));
        JLabel title = new JLabel("CENTRAL NODE - " + ip + " | ROUNAK 23BIT0168");
        title.setForeground(ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel closeBtn = new JLabel("X");
        closeBtn.setForeground(Color.GRAY);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                System.exit(0);
            }

            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(NEON_RED);
            }

            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(Color.GRAY);
            }
        });

        final Point[] dragPoint = { null };
        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragPoint[0] = e.getPoint();
            }
        });
        titleBar.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point p = frame.getLocation();
                frame.setLocation(p.x + e.getX() - dragPoint[0].x, p.y + e.getY() - dragPoint[0].y);
            }
        });

        titleBar.add(title, BorderLayout.WEST);
        titleBar.add(closeBtn, BorderLayout.EAST);
        frame.add(titleBar, BorderLayout.NORTH);

        JPanel dashPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        dashPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
        dashPanel.setBackground(BG_COLOR);

        totalLbl = createStatCard("TOTAL PRODUCTS", "0", ACCENT_COLOR);
        authLbl = createStatCard("AUTHENTIC SCANS", "0", NEON_GREEN);
        fraudLbl = createStatCard("FRAUD ALERTS", "0", NEON_RED);

        dashPanel.add(totalLbl);
        dashPanel.add(authLbl);
        dashPanel.add(fraudLbl);
        frame.add(dashPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 15));
        bottomPanel.setBackground(BG_COLOR);
        bottomPanel.setBorder(new EmptyBorder(0, 20, 20, 20));

        String[] columns = { "Time", "Product ID", "Location", "Status", "Risk Level" };
        tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        table.setRowHeight(35);
        table.setFont(new Font("Consolas", Font.PLAIN, 14));
        table.setBackground(CARD_COLOR);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(BG_COLOR);
        table.getTableHeader().setBackground(BG_COLOR);
        table.getTableHeader().setForeground(TEXT_COLOR);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.setBorder(null);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setBorder(new EmptyBorder(0, 10, 0, 0));
                String risk = (String) table.getModel().getValueAt(row, 4);

                if ("CRITICAL".equals(risk))
                    c.setForeground(NEON_RED);
                else if ("WARNING".equals(risk))
                    c.setForeground(NEON_YELLOW);
                else
                    c.setForeground(NEON_GREEN);

                if (col == 1)
                    c.setForeground(ACCENT_COLOR);
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(CARD_COLOR);
        scrollPane.setBorder(BorderFactory.createLineBorder(CARD_COLOR, 2));
        scrollPane.setPreferredSize(new Dimension(900, 350));
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        RoundedBtn batchBtn = new RoundedBtn("Add Products", ACCENT_COLOR, TEXT_DARK);
        batchBtn.setPreferredSize(new Dimension(150, 45));
        batchBtn.addActionListener(e -> addCustomProducts());

        RoundedBtn crudBtn = new RoundedBtn("Manage Database", NEON_YELLOW, TEXT_DARK);
        crudBtn.setPreferredSize(new Dimension(170, 45));
        crudBtn.addActionListener(e -> openCRUDWindow());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        btnPanel.setBackground(BG_COLOR);
        btnPanel.add(crudBtn);
        btnPanel.add(batchBtn);
        bottomPanel.add(btnPanel, BorderLayout.SOUTH);

        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static void openCRUDWindow() {
        JDialog crudDialog = new JDialog(frame, "Database Management", true);
        crudDialog.setSize(650, 500);
        crudDialog.setLocationRelativeTo(frame);
        crudDialog.getContentPane().setBackground(BG_COLOR);
        crudDialog.setLayout(new BorderLayout());

        String[] cols = { "Product ID", "Status", "Last Known Location" };
        DefaultTableModel pModel = new DefaultTableModel(cols, 0);
        JTable pTable = new JTable(pModel);
        pTable.setRowHeight(30);
        pTable.setBackground(CARD_COLOR);
        pTable.setForeground(TEXT_COLOR);
        pTable.getTableHeader().setBackground(BG_COLOR);
        pTable.getTableHeader().setForeground(TEXT_COLOR);
        pTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane sp = new JScrollPane(pTable);
        sp.getViewport().setBackground(CARD_COLOR);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        crudDialog.add(sp, BorderLayout.CENTER);

        Runnable loadData = () -> {
            try {
                pModel.setRowCount(0);
                List<String[]> prods = service.getAllProducts();
                for (String[] p : prods)
                    pModel.addRow(p);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };
        loadData.run();

        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        ctrlPanel.setBackground(BG_COLOR);

        RoundedBtn refreshBtn = new RoundedBtn("Refresh Table", ACCENT_COLOR, TEXT_DARK);
        RoundedBtn editBtn = new RoundedBtn("Edit Selected", NEON_YELLOW, TEXT_DARK);
        RoundedBtn delBtn = new RoundedBtn("Delete Selected", NEON_RED, TEXT_DARK);

        refreshBtn.addActionListener(e -> loadData.run());

        editBtn.addActionListener(e -> {
            int row = pTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(crudDialog, "Please select a product to edit.");
                return;
            }
            String oldId = (String) pModel.getValueAt(row, 0);
            String oldStatus = (String) pModel.getValueAt(row, 1);

            String newId = JOptionPane.showInputDialog(crudDialog, "Edit Product ID:", oldId);
            if (newId == null || newId.trim().isEmpty())
                return;

            String[] statuses = { "NEW", "SCANNED", "FLAGGED" };
            String newStatus = (String) JOptionPane.showInputDialog(crudDialog, "Edit Status:", "Status",
                    JOptionPane.QUESTION_MESSAGE, null, statuses, oldStatus);
            if (newStatus == null)
                return;

            // --- LOCATION UPDATE LOGIC ---
            String newLoc = "Factory";
            double lat = 0.0;
            double lon = 0.0;

            if (!"NEW".equals(newStatus)) {
                JTextField locField = new JTextField("Admin Override");
                JTextField latField = new JTextField("12.9202"); // Approx Vellore lat
                JTextField lonField = new JTextField("79.1306"); // Approx Vellore lon

                Object[] message = {
                        "Since status is " + newStatus + ", update the coordinates:",
                        "Location Name:", locField,
                        "Latitude:", latField,
                        "Longitude:", lonField
                };

                int option = JOptionPane.showConfirmDialog(crudDialog, message, "Update Location Data",
                        JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION) {
                    newLoc = locField.getText();
                    try {
                        lat = Double.parseDouble(latField.getText());
                        lon = Double.parseDouble(lonField.getText());
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(crudDialog, "Invalid coordinates! Defaulting to 0.0");
                    }
                } else {
                    return; // Admin cancelled update
                }
            }

            try {
                JOptionPane.showMessageDialog(crudDialog,
                        service.updateProduct(oldId, newId.trim(), newStatus, newLoc, lat, lon));
                loadData.run();
                updateDashboard(service.getDashboardStats());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        delBtn.addActionListener(e -> {
            int row = pTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(crudDialog, "Please select a product to delete.");
                return;
            }
            String id = (String) pModel.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(crudDialog,
                    "Are you sure you want to permanently delete " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    JOptionPane.showMessageDialog(crudDialog, service.deleteProduct(id));
                    loadData.run();
                    updateDashboard(service.getDashboardStats());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        ctrlPanel.add(refreshBtn);
        ctrlPanel.add(editBtn);
        ctrlPanel.add(delBtn);
        crudDialog.add(ctrlPanel, BorderLayout.SOUTH);

        crudDialog.setVisible(true);
    }

    private static JLabel createStatCard(String title, String val, Color c) {
        JLabel lbl = new JLabel("<html><center><font color='white'>" + title + "</font><br/><font size='7' color='"
                + hex(c) + "'>" + val + "</font></center></html>", SwingConstants.CENTER);
        lbl.setOpaque(true);
        lbl.setBackground(CARD_COLOR);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(c, 2),
                new EmptyBorder(20, 20, 20, 20)));
        return lbl;
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static void updateDashboard(int[] stats) {
        totalLbl.setText("<html><center><font color='white'>TOTAL PRODUCTS</font><br/><font size='7' color='"
                + hex(ACCENT_COLOR) + "'>" + stats[0] + "</font></center></html>");
        authLbl.setText("<html><center><font color='white'>AUTHENTIC SCANS</font><br/><font size='7' color='"
                + hex(NEON_GREEN) + "'>" + stats[1] + "</font></center></html>");
        fraudLbl.setText("<html><center><font color='white'>FRAUD ALERTS</font><br/><font size='7' color='"
                + hex(NEON_RED) + "'>" + stats[3] + "</font></center></html>");
    }

    private static void addCustomProducts() {
        String countStr = JOptionPane.showInputDialog(frame, "Enter the number of products to add:");
        if (countStr != null && !countStr.trim().isEmpty()) {
            try {
                int count = Integer.parseInt(countStr.trim());
                if (count <= 0)
                    return;
                List<String> newProductIds = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String id = JOptionPane.showInputDialog(frame, "Name Product " + (i + 1) + " of " + count + ":");
                    if (id == null || id.trim().isEmpty())
                        break;
                    newProductIds.add(id.trim());
                }
                if (!newProductIds.isEmpty()) {
                    String res = service.registerProducts(newProductIds);
                    JOptionPane.showMessageDialog(frame, res);
                    updateDashboard(service.getDashboardStats());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error processing input.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void startServer(String myIp) {
        try {
            System.setProperty("java.rmi.server.hostname", myIp);
            service = new CounterfeitServiceImpl(
                    row -> SwingUtilities.invokeLater(() -> {
                        tableModel.insertRow(0, row);
                        try {
                            updateDashboard(service.getDashboardStats());
                        } catch (Exception e) {
                        }
                    }));
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("CounterfeitService", service);
            updateDashboard(service.getDashboardStats());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Server Error: " + e.getMessage());
        }
    }
}

// Custom Neon Button Class
class RoundedBtn extends JButton {
    private Color defaultColor, hoverColor;

    public RoundedBtn(String text, Color defaultColor, Color textColor) {
        super(text);
        this.defaultColor = defaultColor;
        this.hoverColor = defaultColor.brighter();
        setBackground(defaultColor);
        setForeground(textColor);
        setFont(new Font("Segoe UI", Font.BOLD, 14));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                setBackground(hoverColor);
            }

            public void mouseExited(MouseEvent e) {
                setBackground(defaultColor);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g2.dispose();
        super.paintComponent(g);
    }
}