import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CounterfeitInterface extends Remote {

    // Core Verification
    String verifyProduct(String productID, String locationName, double lat, double lon) throws RemoteException;

    // --- CRUD OPERATIONS ---
    String registerProducts(List<String> productIDs) throws RemoteException;

    List<String[]> getAllProducts() throws RemoteException;

    // UPDATED: Now requires location and coordinates for Manual Updates
    String updateProduct(String oldId, String newId, String newStatus, String location, double lat, double lon)
            throws RemoteException;

    String deleteProduct(String productID) throws RemoteException;

    // Dashboard Data
    int[] getDashboardStats() throws RemoteException;
}