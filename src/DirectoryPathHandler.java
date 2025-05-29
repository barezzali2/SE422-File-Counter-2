import java.io.File;
import java.util.Scanner;

class DirectoryPathHandler {

    static String getValidPath() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Please Enter a valid directory path: ");
            String path = scanner.nextLine().trim();
            File dir = new File(path);
            if (dir.isDirectory()) return dir.getAbsolutePath();
            System.out.println("❌  Not a valid directory. Try again.");
        }
    }
}
