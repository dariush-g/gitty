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
                break;
            case "add":
                if (args.length < 2) {
                    System.out.println("add needs parameter");
                    return;
                }
                add(args[1]);
                break;
            case "commit":
                break;
        }
    }

    void add(String file_name) throws IOException {
        var hash = FileHasher.hashFile(file_name);
        Files.write(Path.of("./git/objects/" + hash), Files.readAllBytes(Paths.get(file_name)));
        Files.writeString(Path.of("./git/index"), hash + " " + file_name);
    }

    void init() throws IOException {
        Path path = Paths.get("git/");
        Path objects = Paths.get("./git/objects/");
        Path index = Paths.get("./git/index");
        Path head = Paths.get("./git/HEAD");
        String currentPath = System.getProperty("user.dir");

        if (Files.isDirectory(path) && Files.isDirectory(objects) && Files.exists(index)
                && Files.exists(head))
            System.out.println("Reitialized Gitty repository in " + currentPath);
        else {
            Files.createDirectories(path);
            System.out.println("Initialized empty Gitty repository in " + currentPath);
            Files.createDirectories(objects);
            Files.createFile(index);
            Files.createFile(head);
        }
    }
}
