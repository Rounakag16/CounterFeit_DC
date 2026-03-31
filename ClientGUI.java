import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.rmi.Naming;
import java.util.Scanner;

public class ClientGUI {

    private static CounterfeitInterface service;
    private static String serverIP = "10.85.228.240";

    // Geolocation
    private static String detectedCity = "Unknown";
    private static double detectedLat = 0.0, detectedLon = 0.0;

    // UI Elements
    private static JFrame frame;
    private static JTextField idField;
    private static JLabel locationLabel, statusIcon, statusText, statusSubText;
    private static RoundedPanel resultPanel;
    private static Timer pulseTimer;
    private static boolean pulseUp = true;

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
        String ip = JOptionPane.showInputDialog("Enter Server IP:", serverIP);
        if (ip != null)
            serverIP = ip;
        fetchGeolocation();
        SwingUtilities.invokeLater(ClientGUI::createGUI);
    }

    private static void fetchGeolocation() {
        try {
            URL url = new URL("http://ip-api.com/csv/?fields=city,lat,lon");
            Scanner sc = new Scanner(url.openStream());
            String[] data = sc.nextLine().split(",");
            sc.close();
            detectedCity = data[0];
            detectedLat = Double.parseDouble(data[1]);
            detectedLon = Double.parseDouble(data[2]);
        } catch (Exception e) {
            System.err.println("Geolocation failed.");
        }
    }

    private static void createGUI() {
        frame = new JFrame();
        frame.setSize(450, 650);
        frame.setUndecorated(true);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(BG_COLOR);

        // --- CUSTOM TITLE BAR ---
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(BG_COLOR);
        titleBar.setBorder(new EmptyBorder(10, 15, 10, 15));
        JLabel title = new JLabel("SECURE SCAN | ROUNAK 23BIT0168");
        title.setForeground(ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // FIXED: Replaced emoji with standard 'X'
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

        // --- MAIN CONTENT ---
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_COLOR);
        content.setBorder(new EmptyBorder(10, 30, 30, 30));

        // Location Card
        RoundedPanel locCard = new RoundedPanel(20, CARD_COLOR);
        locCard.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 15));
        locCard.setMaximumSize(new Dimension(400, 60));
        // FIXED: Replaced emoji with "LOC:"
        locationLabel = new JLabel("LOC: " + detectedCity + " (" + detectedLat + ", " + detectedLon + ")");
        locationLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        locationLabel.setForeground(TEXT_COLOR);
        locCard.add(locationLabel);
        content.add(locCard);
        content.add(Box.createVerticalStrut(20));

        // Input Area
        JLabel inputLbl = new JLabel("Enter or Upload Barcode:");
        inputLbl.setForeground(Color.GRAY);
        inputLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        content.add(inputLbl);
        content.add(Box.createVerticalStrut(5));

        JPanel idPanel = new JPanel(new BorderLayout(10, 0));
        idPanel.setBackground(BG_COLOR);
        idPanel.setMaximumSize(new Dimension(400, 45));

        idField = new JTextField("PROD1");
        idField.setFont(new Font("Consolas", Font.BOLD, 20));
        idField.setHorizontalAlignment(JTextField.CENTER);
        idField.setBackground(CARD_COLOR);
        idField.setForeground(ACCENT_COLOR);
        idField.setCaretColor(ACCENT_COLOR);
        idField.setBorder(BorderFactory.createLineBorder(CARD_COLOR, 5));
        idField.addActionListener(e -> performScan());

        // FIXED: Removed folder emoji
        RoundedButton uploadBtn = new RoundedButton("UPLOAD", CARD_COLOR.brighter(), TEXT_COLOR);
        uploadBtn.addActionListener(e -> processUniversalBarcodeImage());

        idPanel.add(idField, BorderLayout.CENTER);
        idPanel.add(uploadBtn, BorderLayout.EAST);
        content.add(idPanel);
        content.add(Box.createVerticalStrut(25));

        // Verify Button
        RoundedButton verifyBtn = new RoundedButton("VERIFY AUTHENTICITY", ACCENT_COLOR, TEXT_DARK);
        verifyBtn.setPreferredSize(new Dimension(400, 50));
        verifyBtn.addActionListener(e -> performScan());
        content.add(verifyBtn);

        frame.add(content, BorderLayout.CENTER);

        // --- RESULT PANEL ---
        resultPanel = new RoundedPanel(30, CARD_COLOR);
        resultPanel.setPreferredSize(new Dimension(390, 200));
        resultPanel.setLayout(new GridLayout(3, 1));

        // FIXED: Used standard hyphen instead of circle dot
        statusIcon = new JLabel("-", SwingConstants.CENTER);
        statusIcon.setFont(new Font("Segoe UI", Font.BOLD, 60));
        statusIcon.setForeground(Color.GRAY);

        statusText = new JLabel("READY TO SCAN", SwingConstants.CENTER);
        statusText.setFont(new Font("Segoe UI", Font.BOLD, 20));
        statusText.setForeground(TEXT_COLOR);

        statusSubText = new JLabel("Awaiting product input...", SwingConstants.CENTER);
        statusSubText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusSubText.setForeground(Color.LIGHT_GRAY);

        resultPanel.add(statusIcon);
        resultPanel.add(statusText);
        resultPanel.add(statusSubText);

        JPanel footerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerWrapper.setBackground(BG_COLOR);
        footerWrapper.setBorder(new EmptyBorder(0, 0, 20, 0));
        footerWrapper.add(resultPanel);
        frame.add(footerWrapper, BorderLayout.SOUTH);

        pulseTimer = new Timer(50, e -> animatePulse());

        frame.setVisible(true);
    }

    private static void processUniversalBarcodeImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Any Barcode or QR Image");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg"));

        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File fileToRead = fileChooser.getSelectedFile();

            // FIXED: Using "..." instead of gear emoji
            updateUI("...", "DECODING...", CARD_COLOR, ACCENT_COLOR);
            statusSubText.setText("Analyzing barcode via Cloud...");
            pulseTimer.start();

            new Thread(() -> {
                try {
                    String boundary = "===" + System.currentTimeMillis() + "===";
                    URL url = new URL("https://zxing.org/w/decode");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    try (OutputStream os = conn.getOutputStream();
                            PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {

                        writer.append("--" + boundary).append("\r\n");
                        writer.append(
                                "Content-Disposition: form-data; name=\"f\"; filename=\"" + fileToRead.getName() + "\"")
                                .append("\r\n");
                        String contentType = Files.probeContentType(fileToRead.toPath());
                        writer.append("Content-Type: " + (contentType != null ? contentType : "image/png"))
                                .append("\r\n\r\n");
                        writer.flush();

                        Files.copy(fileToRead.toPath(), os);
                        os.flush();

                        writer.append("\r\n").append("--" + boundary + "--").append("\r\n");
                        writer.flush();
                    }

                    Scanner scanner = new Scanner(conn.getInputStream());
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine())
                        response.append(scanner.nextLine());
                    scanner.close();

                    pulseTimer.stop();

                    String resStr = response.toString();
                    String decodedText = null;
                    if (resStr.contains("<pre>")) {
                        int start = resStr.indexOf("<pre>") + 5;
                        int end = resStr.indexOf("</pre>", start);
                        decodedText = resStr.substring(start, end);
                    }

                    if (decodedText != null && !decodedText.isEmpty()) {
                        final String finalId = decodedText;
                        SwingUtilities.invokeLater(() -> {
                            idField.setText(finalId.trim().toUpperCase());
                            performScan();
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> updateUI("!", "NO BARCODE DETECTED", CARD_COLOR, NEON_YELLOW));
                    }

                } catch (Exception e) {
                    pulseTimer.stop();
                    SwingUtilities.invokeLater(() -> updateUI("!", "API TIMEOUT / ERROR", CARD_COLOR, NEON_RED));
                }
            }).start();
        }
    }

    private static void performScan() {
        String pid = idField.getText().trim().toUpperCase();
        if (pid.isEmpty())
            return;

        try {
            if (service == null)
                service = (CounterfeitInterface) Naming.lookup("rmi://" + serverIP + ":1099/CounterfeitService");

            String result = service.verifyProduct(pid, detectedCity, detectedLat, detectedLon);

            // FIXED: Using safe unicode checkmark (✓), exclamation (!), and X
            if (result.contains("AUTHENTIC")) {
                updateUI("✓", "AUTHENTIC", NEON_GREEN, TEXT_DARK);
            } else if (result.contains("WARNING")) {
                updateUI("!", "ALREADY SCANNED", NEON_YELLOW, TEXT_DARK);
            } else {
                updateUI("X", "FRAUD DETECTED", NEON_RED, TEXT_DARK);
            }
            statusSubText.setText(result.replace("FRAUD: ", "").replace("WARNING: ", ""));

        } catch (Exception e) {
            updateUI("?", "SERVER OFFLINE", CARD_COLOR, NEON_RED);
            service = null;
        }
    }

    private static void updateUI(String icon, String text, Color bg, Color textCol) {
        resultPanel.setBackground(bg);
        statusIcon.setText(icon);
        statusIcon.setForeground(textCol);
        statusText.setText(text);
        statusText.setForeground(textCol);
        statusSubText.setForeground(textCol == TEXT_DARK ? new Color(0, 0, 0, 180) : Color.LIGHT_GRAY);
        resultPanel.repaint();
    }

    private static void animatePulse() {
        Color c = statusIcon.getForeground();
        int alpha = c.getAlpha();
        if (pulseUp) {
            alpha += 15;
            if (alpha >= 255) {
                alpha = 255;
                pulseUp = false;
            }
        } else {
            alpha -= 15;
            if (alpha <= 100) {
                alpha = 100;
                pulseUp = true;
            }
        }
        statusIcon.setForeground(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
    }
}

// --- CUSTOM MODERN COMPONENTS ---
class RoundedPanel extends JPanel {
    private int radius;
    private Color bg;

    public RoundedPanel(int radius, Color bg) {
        this.radius = radius;
        this.bg = bg;
        setOpaque(false);
    }

    @Override
    public void setBackground(Color bg) {
        this.bg = bg;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }
}

class RoundedButton extends JButton {
    private Color defaultColor;
    private Color hoverColor;

    public RoundedButton(String text, Color defaultColor, Color textColor) {
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
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        g2.dispose();
        super.paintComponent(g);
    }
}