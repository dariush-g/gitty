import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Verify {
    public static void main(String[] args) {

        Git git = new Git();

        try {
            cleanDirectory(Paths.get("git"));
            // Test 1: Initialize repository
            git.init();

            // 1.5 testing add
            Files.createDirectories(Path.of("tree-test"));
            Files.writeString(Path.of("tree-test/hello.txt"), "hello");
            git.add("tree-test/hello.txt");
            System.out.println(Files.readString(Path.of("git/INDEX")));

            // 1.5.1 testing duplicates
            Files.writeString(Path.of("tree-test/hello.txt"), "hello again");
            git.add("tree-test/hello.txt");
            System.out.println(Files.readString(Path.of("git/INDEX")));

            // // Test 2: Verify required files/directories exist
            Path repository = Path.of("git");
            Path objects = repository.resolve("objects");
            Path index = repository.resolve("index");
            Path head = repository.resolve("HEAD");

            if (Files.isDirectory(repository) && Files.isDirectory(objects)
                    && Files.isRegularFile(index) && Files.isRegularFile(head)) {

                System.out.println("Test Passed: Git repository initialized");

            } else {
                System.out.println("Test failed: repository is missing files/directories");
            }

        } catch (IOException e) {
            System.out.println("Test Failed: " + e.getMessage());
        }
        Path testPath = Path.of("test.txt");
        try {
            git.add(testPath.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Path testPath1 = Path.of("test1.txt");
        try {
            git.add(testPath1.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // test 3: createTree()
        try {
            String hash = git.createTree("tree-test");
            System.out.println("treeHash: " + hash);
            System.out.println(Files.readString(Path.of("git/objects/" + hash)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // test 4: createTreeFromIndex()
        try {
            Files.writeString(Path.of("scratch.txt"), "not staged");

            String hash = git.createTreeFromIndex("git/INDEX");
            System.out.println("rootTreeHash: " + hash);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // test 5: commit()
        try {
            git.commit("jessie", "test message");
            git.commit("jessie2", "test message 2, should have parent");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // test 6: regenerateIndex()
        try {
            String oldIndex = Files.readString(Path.of("git/index"));
            String head = Files.readString(Path.of("git/HEAD")).trim();

            Files.writeString(Path.of("git/index"), "");

            git.regenerateIndex(head);

            System.out.println("\n\nOriginal index:" + oldIndex);

            System.out.println("Regenerated index: " + Files.readString(Path.of("git/index")));

            String newHash = git.commit("person", "commit2");

            System.out.println("First commit:" + Files.readString(Path.of("git/objects/" + head)));

            System.out.println("\nSecond commit:" + Files.readString(Path.of("git/objects/" + newHash)));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cleanDirectory(Path path) {
        if (!Files.exists(path))
            return;
        try {
            Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            System.err.println("Failed to cleanup: " + path);
        }
    }
}
