import java.io.IOException;

void main(String[] args) throws IOException {
    if (args.length == 0)
        return;
    switch (args[0]) {
        case "init":
            Path path = Paths.get(".gitty");
            String currentPath = System.getProperty("user.dir");
            if (Files.isDirectory(path))
                System.out.println("Reitialized empty Gitty repository in " + currentPath);

            Files.createDirectories(path);
            System.out.println("Initialized empty Gitty repository in " + currentPath);
            Files.createDirectories(Paths.get("./.gitty/objects/"));
        case "add":
        case "commit":
    }
}
