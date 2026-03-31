import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CounterfeitServiceImpl extends UnicastRemoteObject implements CounterfeitInterface {

    private Connection conn;
    private final Consumer<String[]> guiLogger;

    public CounterfeitServiceImpl(Consumer<String[]> guiLogger) throws RemoteException {
        super();
        this.guiLogger = guiLogger;
        initializeDatabase();

        try {
            List<String> initialSeed = Arrays.asList("PROD1", "PROD2", "PROD3", "PROD4", "PROD5");
            registerProducts(initialSeed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:counterfeit.db");
            Statement stmt = conn.createStatement();

            String sql = "CREATE TABLE IF NOT EXISTS products (" +
                    "id TEXT PRIMARY KEY, " +
                    "status TEXT, " +
                    "last_location TEXT, " +
                    "last_lat REAL, " +
                    "last_lon REAL, " +
                    "last_scan_time INTEGER)";
            stmt.execute(sql);
            System.out.println("SQL Database Initialized.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized String verifyProduct(String productID, String locationName, double lat, double lon)
            throws RemoteException {
        try {
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM products WHERE id = ?");
            checkStmt.setString(1, productID);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                logToTable(productID, locationName, "INVALID ID", "CRITICAL");
                return "FRAUD: INVALID ID";
            }

            String status = rs.getString("status");
            String lastLocation = rs.getString("last_location");
            double lastLat = rs.getDouble("last_lat");
            double lastLon = rs.getDouble("last_lon");
            long lastScanTime = rs.getLong("last_scan_time");
            long currentTime = System.currentTimeMillis();

            if ("NEW".equals(status)) {
                updateProductState(productID, "SCANNED", locationName, lat, lon, currentTime);
                logToTable(productID, locationName, "AUTHENTIC", "SUCCESS");
                return "AUTHENTIC";
            }

            if ("SCANNED".equals(status) || "FLAGGED".equals(status)) {
                double distanceKm = calculateDistance(lastLat, lastLon, lat, lon);
                double timeHours = (currentTime - lastScanTime) / 3600000.0;

                if (timeHours < 0.001)
                    timeHours = 0.001;

                double speed = distanceKm / timeHours;

                if (speed > 900 && distanceKm > 50) {
                    updateProductState(productID, "FLAGGED", locationName, lat, lon, currentTime);
                    String msg = String.format("CLONED TAG (Impossible Travel: %.0f km in %.2f hrs)", distanceKm,
                            timeHours);
                    logToTable(productID, locationName, msg, "CRITICAL");
                    return "FRAUD: " + msg;
                } else {
                    logToTable(productID, locationName, "DOUBLE SCAN", "WARNING");
                    return "WARNING: ALREADY SCANNED";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR: DATABASE FAILURE";
        }
        return "ERROR: UNKNOWN STATE";
    }

    private void updateProductState(String id, String status, String loc, double lat, double lon, long time)
            throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE products SET status=?, last_location=?, last_lat=?, last_lon=?, last_scan_time=? WHERE id=?");
        pstmt.setString(1, status);
        pstmt.setString(2, loc);
        pstmt.setDouble(3, lat);
        pstmt.setDouble(4, lon);
        pstmt.setLong(5, time);
        pstmt.setString(6, id);
        pstmt.executeUpdate();
    }

    @Override
    public synchronized String registerProducts(List<String> productIDs) throws RemoteException {
        int successCount = 0;
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT OR IGNORE INTO products (id, status, last_location, last_lat, last_lon, last_scan_time) VALUES (?, 'NEW', 'Factory', 0.0, 0.0, 0)");
            for (String id : productIDs) {
                pstmt.setString(1, id.toUpperCase());
                if (pstmt.executeUpdate() > 0)
                    successCount++;
            }
            String msg = "Registered " + successCount + " new custom product codes.";
            SystemLogger.log("ADMIN", msg);
            return msg;
        } catch (SQLException e) {
            return "Error registering products: " + e.getMessage();
        }
    }

    @Override
    public List<String[]> getAllProducts() throws RemoteException {
        List<String[]> list = new ArrayList<>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, status, last_location FROM products ORDER BY id ASC");
            while (rs.next()) {
                list.add(new String[] { rs.getString("id"), rs.getString("status"), rs.getString("last_location") });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // --- UPDATED CRUD LOGIC ---
    @Override
    public synchronized String updateProduct(String oldId, String newId, String newStatus, String location, double lat,
            double lon) throws RemoteException {
        try {
            newStatus = newStatus.toUpperCase();
            PreparedStatement pstmt;

            if ("NEW".equals(newStatus)) {
                // If Admin selects NEW, reset everything to 0
                pstmt = conn.prepareStatement(
                        "UPDATE products SET id=?, status=?, last_location='Factory', last_lat=0.0, last_lon=0.0, last_scan_time=0 WHERE id=?");
                pstmt.setString(1, newId.toUpperCase());
                pstmt.setString(2, newStatus);
                pstmt.setString(3, oldId);
            } else {
                // If Admin selects SCANNED or FLAGGED, update coordinates and set scan_time to
                // right now
                pstmt = conn.prepareStatement(
                        "UPDATE products SET id=?, status=?, last_location=?, last_lat=?, last_lon=?, last_scan_time=? WHERE id=?");
                pstmt.setString(1, newId.toUpperCase());
                pstmt.setString(2, newStatus);
                pstmt.setString(3, location);
                pstmt.setDouble(4, lat);
                pstmt.setDouble(5, lon);
                pstmt.setLong(6, System.currentTimeMillis());
                pstmt.setString(7, oldId);
            }

            if (pstmt.executeUpdate() > 0) {
                SystemLogger.log("ADMIN", "Updated Product: " + oldId + " -> " + newId + " (" + newStatus + ")");
                return "Success: Product updated.";
            }
            return "Error: Product not found.";
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public synchronized String deleteProduct(String productID) throws RemoteException {
        try {
            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM products WHERE id=?");
            pstmt.setString(1, productID);
            if (pstmt.executeUpdate() > 0) {
                SystemLogger.log("ADMIN", "Deleted Product: " + productID);
                return "Success: Product deleted.";
            }
            return "Error: Product not found.";
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public int[] getDashboardStats() throws RemoteException {
        int[] stats = { 0, 0, 0, 0 };
        try {
            Statement stmt = conn.createStatement();
            stats[0] = stmt.executeQuery("SELECT count(*) FROM products").getInt(1);
            stats[1] = stmt.executeQuery("SELECT count(*) FROM products WHERE status='SCANNED'").getInt(1);
            stats[3] = stmt.executeQuery("SELECT count(*) FROM products WHERE status='FLAGGED'").getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void logToTable(String id, String loc, String status, String type) {
        String time = java.time.LocalTime.now().toString().substring(0, 8);
        guiLogger.accept(new String[] { time, id, loc, status, type });
        SystemLogger.log(type, status + " - " + id + " @ " + loc);
    }
}