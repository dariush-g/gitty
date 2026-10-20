import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
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
                        String blobHash = FileHasher.hashFile(directoryContents[i].getPath().toString());
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

    // recursive version of createTree
    // public static String createTree(ArrayList<String> workingList, String
    // directoryPath)
    // throws IOException {
    // StringBuilder treeContents = new StringBuilder();

    // for (String line : workingList) {
    // // decomposing each entry into its type, hash, and file path
    // String[] parts = line.split(" ", 3);

    // String type = parts[0];
    // String hash = parts[1];
    // String filePath = parts[2];

    // File file = new File(filePath);
    // String parentDirectory = file.getParent();

    // // checking if the file belongs directly inside the directory we're creating
    // the tree
    // // for
    // if (directoryPath.equals(parentDirectory)) {
    // String fileName = file.getName();
    // treeContents.append(type + " " + hash + " " + fileName + "\n");
    // }
    // }

    // String treeHash = FileHasher.hashFile(treeContents.toString());
    // FileWriter fw = new FileWriter("git/objects/" + treeHash);
    // fw.write(treeContents.toString());
    // fw.close();
    // return treeHash;
    // }

    public static ArrayList<String> workingList() throws IOException {
        ArrayList<String> workingList = new ArrayList<>();
        ArrayList<String> workingListFiles = new ArrayList<>();
        HashMap<String, String> fileMap = new HashMap<>();

        // reading the existing index file (going line by line), prefix "blob"
        try (BufferedReader br = new BufferedReader(new FileReader("git/INDEX"))) {
            String line = br.readLine();

            while (line != null) {
                // add same filePath, but with diff hash
                if (!line.isBlank()) {
                    workingList.add("blob " + line);
                }

                line = br.readLine();
            }
        }

        // get file paths and store original entries
        for (String line : workingList) {
            String[] parts = line.split(" ", 3);
            String filePath = parts[2];

            workingListFiles.add(filePath);
            fileMap.put(filePath, line);
        }

        // sort alphabetically
        Collections.sort(workingListFiles);

        // rebuild working list in sorted order
        ArrayList<String> sortedWorkingList = new ArrayList<>();

        for (String filePath : workingListFiles) {
            sortedWorkingList.add(fileMap.get(filePath));
        }

        return sortedWorkingList;
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
            // easier to sort later
            String[] splits = newLine.split(" ");
            String filePath = splits[1];
            String hash = splits[0];
            String type = "blob";
            workingList.add(filePath + " " + type + " " + hash);
        }
        br.close();
        Collections.sort(workingList);

        while (workingList.size() > 0) {
            // sees which is deepest by counting slashes
            int maxSlashes = -1;
            ArrayList<String> directoryEntries = new ArrayList<>();

            for (String workingListEntry : workingList) {

                String filePath = workingListEntry.split(" ", 3)[0];
                int slashCount = filePath.split("/", -1).length - 1;

                if (slashCount > maxSlashes) {
                    maxSlashes = slashCount;
                    directoryEntries = new ArrayList<>();
                    directoryEntries.add(workingListEntry);
                } else if (slashCount == maxSlashes) {
                    directoryEntries.add(workingListEntry);
                }
            }

            // if all remaining files are in the root directory
            if (maxSlashes == 0) {
                StringBuilder rootContents = new StringBuilder();

                for (String entry : workingList) {
                    String[] parts = entry.split(" ", 3);

                    String filePath = parts[0];
                    String type = parts[1];
                    String hash = parts[2];

                    rootContents.append(type + " " + hash + " " + filePath + "\n");
                }

                String rootHash = FileHasher.hashString(rootContents.toString());

                Files.writeString(
                        Path.of("./git/objects/" + rootHash),
                        rootContents.toString());

                return rootHash;
            }

            // see if the deepestLines have same directory
            String deepestDirectoryPath = directoryEntries.get(0).substring(0,
                    directoryEntries.get(0).lastIndexOf("/"));
            ArrayList<String> sameDir = new ArrayList<>();

            for (String directoryEntry : workingList) {

                String filePath = directoryEntry.split(" ", 3)[0];
                String parentDirectoryPath = "";

                if (filePath.contains("/")) {
                    parentDirectoryPath = filePath.substring(0, filePath.lastIndexOf("/"));
                }

                if (deepestDirectoryPath.equals(parentDirectoryPath)) {
                    sameDir.add(directoryEntry);
                }
            }

            // building tree
            StringBuilder treeContents = new StringBuilder();

            for (String entry : sameDir) {
                String[] parts = entry.split(" ", 3);

                String filePath = parts[0];
                String type = parts[1];
                String hash = parts[2];

                String fileName = Path.of(filePath).getFileName().toString();

                treeContents.append(type + " " + hash + " " + fileName + "\n");
            }

            String treeHash = FileHasher.hashString(treeContents.toString());

            Files.writeString(
                    Path.of("./git/objects/" + treeHash),
                    treeContents.toString());

            // puts lines back in correct order (uses list now)
            ArrayList<String> updatedLines = new ArrayList<>();

            for (String remainingEntry : workingList) {

                String[] parts = remainingEntry.split(" ", 3);
                String filePath = parts[0];
                String parentPath = "";

                if (Path.of(filePath).getParent() != null) {
                    parentPath = Path.of(filePath).getParent().toString();
                }

                if (!parentPath.equals(deepestDirectoryPath)) {
                    updatedLines.add(remainingEntry);
                }
            }

            updatedLines.add(deepestDirectoryPath + " tree " + treeHash);
            Collections.sort(updatedLines);
            workingList = updatedLines;
        }

        // if index is empty
        String treeContents = "";
        String treeHash = FileHasher.hashString(treeContents);

        Files.writeString(Path.of("./git/objects/" + treeHash), treeContents);

        return treeHash;
    }

    public String commit(String author, String message) {
        try {
            String sha1 = createTreeFromIndex("git/INDEX");
            String date = LocalDate.now().toString();
            StringBuilder fileContents = new StringBuilder();

            BufferedReader br = new BufferedReader(new FileReader("git/HEAD"));
            String head = br.readAllAsString();
            String parent;

            if (!head.equals("")) {
                parent = head;
            } else {
                parent = null;
            }
            br.close();

            if (parent == null) {
                fileContents.append("tree: " + sha1 + "\nauthor: " + author + "\ndate: " + date
                        + "\nmessage: " + message);
            } else {
                fileContents.append("tree: " + sha1 + "\nparent: " + parent + "\nauthor: " + author + "\ndate: " + date
                        + "\nmessage: " + message);
            }

            // hash commit contents
            String commitHash = FileHasher.hashString(fileContents.toString());

            // create commit file
            File commitFile = new File("git/objects/" + commitHash);
            FileWriter fw = new FileWriter(commitFile);
            fw.write(fileContents.toString());
            fw.close();

            // update head after saving the commit
            Files.writeString(Path.of("git/HEAD"), commitHash);

            return commitHash;

        } catch (Exception e) {
            System.out.println("An error occurred while creating the file.");
            e.printStackTrace();
            return null;
        }
    }

}
