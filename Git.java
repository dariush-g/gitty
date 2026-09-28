import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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
        System.out.println();
    }

    void add(String path_to_add) throws IOException {
        var hash = FileHasher.hashFile(path_to_add);
        var path = Path.of("./git/objects/" + hash);

        var hashes_files = Files.readAllLines(Path.of("./git/index")).stream().map(line -> {
            var split = line.split(" ");
            if (split.length != 2) {
                System.err.println("error parsing index");
                System.exit(1);
            }
            return split;
        }).toList();

        boolean exists = false;

        for (var hash_file : hashes_files) {
            var file = hash_file[1];
            if (path_to_add.equals(file)) {
                hash_file[0] = hash;
                exists = true;
            }
        }

        if (!exists) {
            Files.write(path, Files.readAllBytes(Paths.get(path_to_add)));
            Files.writeString(Path.of("./git/INDEX"), hash + " " + path_to_add + "\n",
                    StandardOpenOption.APPEND);
        } else {
            Files.write(path, Files.readAllBytes(Paths.get(path_to_add)));
            var builder = new StringBuilder();
            for (var hash_file : hashes_files)
                builder.append(hash_file[0] + " " + hash_file[1] + "\n");
            Files.writeString(Path.of("./git/INDEX"), builder.toString());
        }
    }

    void init() throws IOException {
        Path path = Paths.get("git/");
        Path objects = Paths.get("./git/objects/");
        Path index = Paths.get("./git/INDEX");
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
