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
    public String createTree(String directoryPath) {
        // checks if pathToAdd is file, if it is then it adds it like a file from before
        Path targetPath = Path.of(directoryPath);
        if (!targetPath.toFile().isDirectory()) {
            try {
                String hash = FileHasher.hashFile(directoryPath);
                FileWriter fw = new FileWriter("./git/objects/" + hash);
                fw.write(Files.readString(targetPath));
                fw.close();
                return hash;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // turns the contents in folder into array
        File directory = new File(directoryPath);
        File[] directoryContents = directory.listFiles();
        StringBuilder treeContents = new StringBuilder();
        if (directoryContents != null) {
            for (int i = 0; i < directoryContents.length; i++) {
                if (!directoryContents[i].isDirectory()) {
                    try {
                        // finds file hash of the file
                        String blobHash =
                                FileHasher.hashFile(directoryContents[i].getPath().toString());
                        // writes into blob
                        FileWriter blobWriter = new FileWriter("./git/objects/" + blobHash);
                        blobWriter.write(Files.readString(directoryContents[i].toPath()));
                        blobWriter.close();
                        // writes line for file
                        treeContents.append(
                                "blob " + blobHash + " " + directoryContents[i].getName() + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // finds file hash of tree
                    String subtreeHash = createTree(directoryContents[i].getPath().toString());
                    treeContents.append(
                            "tree " + subtreeHash + " " + directoryContents[i].getName() + "\n");
                }
            }

        }
        try {
            String treeContentString = treeContents.toString();
            String treeHash = FileHasher.hashString(treeContentString);

            // Writes the tree file
            Files.writeString(Path.of("./git/objects/" + treeHash), treeContentString);

            return treeHash;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String createTreeFromIndex(String workingListPath) throws IOException {
        // builds and sorts working list
        Path path = Path.of(workingListPath);
        File file = path.toFile();

        BufferedReader br = new BufferedReader(new FileReader(file));
        // paths isolates all the paths parts, so we can sort it
        ArrayList<String> workingList = new ArrayList<>();
        while (br.ready()) {
            String newLine = br.readLine();
            // identifies what is path and what is hash and rearranges with path first so
            // easirr to sort later
            String[] splits = newLine.split(" ");
            String filePath = splits[2];
            String hash = splits[1];
            String type = "blob";
            workingList.add(filePath + " " + type + " " + hash);
        }
        br.close();
        Collections.sort(workingList);

        // sees which is deepest by counting slashes
        int maxSlashes = -1;
        ArrayList<String> directoryEntries = new ArrayList<>();

        for (String workingListEntry : workingList) {
            int slashCount = 0;
            for (int i = 0; i < workingListEntry.length(); i++) {
                if (workingListEntry.charAt(i) == '/') {
                    slashCount++;
                }
            }
            if (slashCount > maxSlashes) {
                maxSlashes = slashCount;
                directoryEntries = new ArrayList<>();
                directoryEntries.add(workingListEntry);
            } else if (slashCount == maxSlashes) {
                directoryEntries.add(workingListEntry);

            }
        }

        // see if the deepestLines have same directory
        String deepestDirectoryPath =
                directoryEntries.get(0).substring(0, directoryEntries.get(0).lastIndexOf("/"));
        ArrayList<String> sameDir = new ArrayList<>();
        sameDir.add(directoryEntries.get(0));

        for (String directoryEntry : directoryEntries) {
            String parentDirectoryPath = directoryEntry.substring(0, directoryEntry.lastIndexOf("/"));
            if (deepestDirectoryPath.equals(parentDirectoryPath)) {
                sameDir.add(directoryEntry);
            }
        }

        // finds hash of the directory
        String treeHash = createTree(deepestDirectoryPath);

        // puts lines back in correct order (uses list now)
        ArrayList<String> updatedLines = new ArrayList<>();

        for (String remainingEntry : workingList) {
            if (!remainingEntry.contains(deepestDirectoryPath)) {
                String[] lineParts = remainingEntry.split(" ");
                String remainingFilePath = lineParts[0];
                String fileHash = lineParts[2];
                String type = lineParts[1];

                updatedLines.add(type + " " + fileHash + " " + remainingFilePath);
            }
        }

        return treeHash;
    }

}
