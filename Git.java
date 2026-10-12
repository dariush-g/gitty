import java.io.*;
import java.nio.file.*;
import java.util.*;

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

        var hashes_files = Files.readAllLines(Path.of("./git/INDEX")).stream().map(line -> {
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
            System.out.println("Git Repository Already Exists");
        else {
            Files.createDirectories(path);
            System.out.println("Git Repository Created");
            Files.createDirectories(objects);
            Files.createFile(index);
            Files.createFile(head);
        }
    }

    // generates a tree based upon the Path of a directory
    public String createTree(String pathToAdd) {
        // checks if pathToAdd is file, if it is then it adds it like a file from before
        Path pathToUse = Path.of(pathToAdd);
        if (!pathToUse.toFile().isDirectory()) {
            try {
                String hash = FileHasher.hashFile(pathToAdd);
                FileWriter fw = new FileWriter("./git/objects/" + hash);
                fw.write(Files.readString(pathToUse));
                fw.close();
                return hash;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // turns the contents in folder into array
        File folder = new File(pathToAdd);
        File[] filesInFolder = folder.listFiles();
        StringBuilder contentsOfTree = new StringBuilder();
        if (filesInFolder != null) {
            for (int i = 0; i < filesInFolder.length; i++) {
                if (!filesInFolder[i].isDirectory()) {
                    try {
                        // finds file hash of the file
                        String fileHash =
                                FileHasher.hashFile(filesInFolder[i].getPath().toString());
                        // writes into blob
                        FileWriter blobWriter = new FileWriter("./git/objects/" + fileHash);
                        blobWriter.write(Files.readString(filesInFolder[i].toPath()));
                        blobWriter.close();
                        // writes line for file
                        contentsOfTree.append(
                                "blob " + fileHash + " " + filesInFolder[i].getName() + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // finds file hash of tree
                    String hashOfSmallerTree = createTree(filesInFolder[i].getPath().toString());
                    contentsOfTree.append(
                            "tree " + hashOfSmallerTree + " " + filesInFolder[i].getName() + "\n");
                }
            }

        }
        try {
            String stringContents = contentsOfTree.toString();
            String hash = FileHasher.hashString(stringContents);

            // Writes the tree file
            Files.writeString(Path.of("./git/objects/" + hash), stringContents);

            return hash;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public String createTreeFromIndex(String pathToWorkingList) throws IOException {
        // builds and sorts working list
        Path path = Path.of(pathToWorkingList);
        File file = path.toFile();

        BufferedReader br = new BufferedReader(new FileReader(file));
        // paths isolates all the paths parts, so we can sort it
        ArrayList<String> lines = new ArrayList<>();
        while (br.ready()) {
            String newLine = br.readLine();
            // identifies what is path and what is hash and rearranges with path first so
            // easirr to sort later
            String[] splits = newLine.split(" ");
            String path1 = splits[2];
            String hash = splits[1];
            String type = "blob";
            lines.add(path1 + " " + type + " " + hash);
        }
        br.close();
        Collections.sort(lines);

        // sees which is deepest by counting slashes
        int maxSlashes = -1;
        ArrayList<String> deepestLines = new ArrayList<>();

        for (String line : lines) {
            int slashCount = 0;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == '/') {
                    slashCount++;
                }
            }
            if (slashCount > maxSlashes) {
                maxSlashes = slashCount;
                deepestLines = new ArrayList<>();
                deepestLines.add(line);
            } else if (slashCount == maxSlashes) {
                deepestLines.add(line);

            }
        }

        // see if the deepestLines have same directory
        String firstDirectory =
                deepestLines.get(0).substring(0, deepestLines.get(0).lastIndexOf("/"));
        ArrayList<String> sameDir = new ArrayList<>();
        sameDir.add(deepestLines.get(0));

        for (String line1 : deepestLines) {
            String dir = line1.substring(0, line1.lastIndexOf("/"));
            if (firstDirectory.equals(dir)) {
                sameDir.add(line1);
            }
        }

        // finds hash of the directory
        String hashOfTree = createTree(firstDirectory);

        // puts lines back in correct order (uses list now)
        ArrayList<String> updatedLines = new ArrayList<>();

        for (String line2 : lines) {
            if (!line2.contains(firstDirectory)) {
                String[] splits = line2.split(" ");
                String path2 = splits[0];
                String hash = splits[2];
                String type = splits[1];

                updatedLines.add(type + " " + hash + " " + path2);
            }
        }

        return hashOfTree;
    }

}
