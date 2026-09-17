import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Git {
    void main(String[] args) throws IOException {
        if (args.length == 0)
            return;
        switch (args[0]) {
            case "init":
                init();
            case "add":
            case "commit":
        }
    }

    void init() throws IOException {
        Path path = Paths.get("git/");
        String currentPath = System.getProperty("user.dir");
        if (Files.isDirectory(path))
            System.out.println("Reitialized empty Gitty repository in " + currentPath);
        else {
            Files.createDirectories(path);
            System.out.println("Initialized empty Gitty repository in " + currentPath);
            Files.createDirectories(Paths.get("./git/objects/"));
            Files.createDirectories(Paths.get("./git/index/"));
            Files.createFile(Paths.get("./git/HEAD"));
        }
    }
}
