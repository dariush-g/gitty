import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class GitWrapper {

    /**
     * Initializes a new Git repository.
     * This method creates the necessary directory structure
     * and initial files (index, HEAD) required for a Git repository.
     */
    public void init() {
        Git git = new Git();

        try {
            git.init();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Stages a file for the next commit.
     * This method adds a file to the index file.
     * If the file does not exist, it throws an IOException.
     * If the file is a directory, it throws an IOException.
     * If the file is already in the index, it does nothing.
     * If the file is successfully staged, it creates a blob for the file.
     * 
     * @param filePath The path to the file to be staged.
     */
    public void add(String filePath) {
        Git git = new Git();

        try {
            Path path = Path.of(filePath);

            if (!Files.exists(path)) {
                throw new IOException("File does not exist: " + filePath);
            }

            if (Files.isDirectory(path)) {
                throw new IOException("Can't add a directory: " + filePath);
            }

            git.add(filePath);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a commit with the given author and message.
     * It should capture the current state of the repository by building trees based
     * on the index file,
     * writing the tree to the objects directory,
     * writing the commit to the objects directory,
     * updating the HEAD file,
     * and returning the commit hash.
     *
     * The commit should be formatted as follows:
     * tree: <tree_sha>
     * parent: <parent_sha>
     * author: <author>
     * date: <date>
     * message: <message>
     *
     * @param author  The name of the author making the commit.
     * @param message The commit message describing the changes.
     * @return The SHA1 hash of the new commit.
     */
    public String commit(String author, String message) {
        Git git = new Git();

        return git.commit(author, message);
    }

    /**
     * Checks out a specific commit given its hash.
     * This method should read the HEAD file to determine the "checked out" commit.
     * Then it should update the working directory to match the
     * state of the repository at that commit by tracing through the root tree and
     * all its children.
     *
     * @param commitHash The SHA1 hash of the commit to check out.
     */
    public void checkout(String commitHash) {
        Git git = new Git();

        try {
            ArrayList<String> oldIndexLines = new ArrayList<>(Files.readAllLines(Path.of("git/index")));

            // generate new index (well, whatever's in the commit)
            git.regenerateIndex(commitHash);
            ArrayList<String> newIndexLines = new ArrayList<>(Files.readAllLines(Path.of("git/index")));

            // delete files not tracked
            for (String line : oldIndexLines) {
                String[] parts = line.split(" ", 2);
                String filePath = parts[1];

                boolean found = false;

                for (String newLine : newIndexLines) {
                    String[] newParts = newLine.split(" ", 2);
                    if (filePath.equals(newParts[1])) {
                        found = true;
                    }
                }

                if (!found) {
                    Files.deleteIfExists(Path.of(filePath));

                    // remove empty folders
                    Path folder = Path.of(filePath).getParent();

                    while (folder != null) {
                        try {
                            Files.delete(folder);
                        } catch (Exception e) {
                            break;
                        }

                        folder = folder.getParent();
                    }
                }
            }

            // restore files from commit we're checking out
            for (String line : newIndexLines) {
                String[] parts = line.split(" ", 2);
                String hash = parts[0];
                String filePath = parts[1];

                Path origin = Path.of("git/objects/" + hash);
                Path target = Path.of(filePath);

                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }

                Files.copy(origin, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // update head
            Files.writeString(Path.of("git/HEAD"), commitHash);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
