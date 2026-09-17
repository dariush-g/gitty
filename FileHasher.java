import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHasher {
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String hashFile(String filePath) throws IOException {
        var read = Files.readAllBytes(Path.of(filePath));

        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var encoded = digest.digest(read);
            String hexString = bytesToHex(encoded);
            return hexString;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error hashing file" + filePath);
            e.printStackTrace();
            return filePath;
        }
    }
}
