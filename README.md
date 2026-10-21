## init
Initializes a git folder in the directory and puts an empty objects/ directory as well as git/HEAD and git/INDEX files. If a git folder already exists, nothing happens

## add <filename>
Adds file to objects folder with the name of the file being the hash of the contents. Then, in git/INDEX, it puts an entry <filehash> <filename>

# GITWRAPPER USAGE

## initialize repository
GitWrapper git = new GitWrapper();
git.init();

## stage files
git.add("test");
git.add("src/Test.java");

## commit
String hash = git.commit("John", "Initial commit");

## checkout

# changes 10/20/26
Fixed all of createTreeFromIndex()
- builds trees from staged files instead of all current working files
- root-level files during tree creation work
other
- commit() added 