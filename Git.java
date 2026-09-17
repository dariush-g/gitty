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
                if (args.length < 2) {
                    System.out.println("add needs parameter");
                    return;
                }
                add(args[1]);
            case "commit":
        }
    }

    void add(String file_name) throws IOException {
        var hash = FileHasher.hashFile(file_name);
        Files.write(Path.of("./git/objects/" + hash), Files.readAllBytes(Paths.get(file_name)));
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
            Files.createFile(Paths.get("./git/index/"));
            Files.createFile(Paths.get("./git/HEAD"));
        }
    }
}
