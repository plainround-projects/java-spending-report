# Maven Wrapper (3.3.4, `only-script` type)

## 1. What it is

To build a Java project with Maven you normally have to install Maven first, and different people install different versions. The **Maven Wrapper** removes that problem. It is a small script (`mvnw`) that lives in the repository. When you run it, it checks whether the right Maven is already on your computer; if not, it downloads that exact version, unpacks it in a hidden folder in your home directory, and starts it with your arguments. After the first run, `./mvnw` is as fast as `mvn` would be.

## 2. Why this repository uses it

The reader of this repository needs only a JDK. `./mvnw` then gives the same Maven (3.9.16) on the author's Mac, in CI and in the Docker build, which is part of the promise that the build repeats exactly.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `mvnw` | the shell script that downloads and starts Maven (Linux, macOS, Git Bash) |
| `mvnw.cmd` | the same job for Windows `cmd.exe` |
| `.mvn/wrapper/maven-wrapper.properties` | which Maven to download |
| `.mvn/jvm.config` | JVM options for the JVM that runs Maven (opens JDK internals for the formatter) |
| `Makefile` | every Maven call is `./mvnw`, never `mvn` (the variable `MVN := ./mvnw`) |
| `Dockerfile` | copies `mvnw` and `.mvn` into the build stage and runs `./mvnw` there |
| `.github/workflows/ci.yml` | runs `make` targets, which call `./mvnw` |

## 4. How it works underneath

1. **First run.** `mvnw` finds no folder `~/.m2/wrapper/dists/apache-maven-3.9.16/<hash>`. It reads `distributionUrl`, downloads the zip to a temporary folder (with `wget`, else `curl`, else Java), unpacks it, moves the unpacked Maven to that folder and deletes the temporary one.
2. **Every run.** `mvnw` replaces itself with `<that folder>/bin/mvn`, passing on every argument. From then on you are running ordinary Maven.
3. **The hash in the folder name** is a fingerprint of the download address, so two projects that download different Mavens never mix.
4. **Permissions.** `mvnw` must be an executable file (git stores the executable bit, mode 755). If the bit is lost, `./mvnw` says `Permission denied`; `sh mvnw` still works.
5. **`.mvn/jvm.config`** is read by Maven's start-up script, so its options apply to every Maven command in this folder.

## 5. Walkthrough of this repository's use

### `.mvn/wrapper/maven-wrapper.properties`

```properties
wrapperVersion=3.3.4
```

The version of the wrapper machinery itself (3.3.4). It is informational: it records which wrapper generated these files.

```properties
distributionType=only-script
```

`only-script` means the repository holds only the script `mvnw`, not a helper jar. The script downloads Maven using `wget`, `curl`, or (last resort) a tiny Java program it writes and compiles on the fly.

```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
```

The exact file to download: Maven 3.9.16, the binary zip, from **Maven Central**'s address `repo.maven.apache.org`. Because the version is part of the address, every machine runs the same Maven. A `.properties` file is a list of `name=value` lines.

> New words:
>
> - **Maven Central**: the big public warehouse of Java libraries on the internet (`repo.maven.apache.org`). Maven downloads the files named by your coordinates from it, and keeps a copy in the folder `~/.m2/repository` on your computer, so each file is downloaded only once.

### `.mvn/jvm.config`

```properties
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
```

`.mvn/jvm.config` is read by Maven's own start-up script, which adds each option to the command line of the JVM that runs Maven. Each line here is a JVM option of the form `--add-exports MODULE/PACKAGE=TARGET`. Since Java 17 the JDK **hides** the internal packages of its own modules; `--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED` makes the package `com.sun.tools.javac.api` of the module `jdk.compiler` visible to every piece of code that is not in a module (`ALL-UNNAMED`, such as plugins loaded from jars). The first line opens the compiler's public-ish API package.

```properties
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
```

Opens `com.sun.tools.javac.file`, the compiler's file handling.

```properties
--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
```

Opens `com.sun.tools.javac.parser`, the compiler's parser (the part that reads source text).

```properties
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
```

Opens `com.sun.tools.javac.tree`, the compiler's syntax tree classes.

```properties
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

Opens `com.sun.tools.javac.util`, helper classes the parser uses.

```properties
--add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
```

Opens `com.sun.tools.javac.code`, the compiler's symbols and types. All six are needed because google-java-format (run by Spotless) calls the compiler's own parser to understand the source before it reformats it, and Java 25 refuses that access unless it is opened. Without these lines, `make lint` fails with an access error.

### `mvnw`, line by line (Linux, macOS and Git Bash)

```bash
#!/bin/sh
# ----------------------------------------------------------------------------
```

The first line, `#!/bin/sh`, is the **shebang**: when you run `./mvnw`, the system starts the program `/bin/sh` (the basic Unix command interpreter, see **terminal and shell**) and gives it this file to read. The file is therefore a **shell script**. It is written for plain `sh`, the lowest common denominator, so it runs on macOS, Linux and in Git Bash on Windows.

> New words:
>
> - **terminal and shell**: a terminal is a window in which you type commands. The program that reads what you type and runs it is the **shell** (here `sh`, `bash` or `zsh`). A command is a program name followed by arguments, separated by spaces: `ls -l target` runs the program `ls` with the arguments `-l` and `target`.
> - **shell script**: a text file of shell commands, run from top to bottom as if you had typed them. The first line `#!/bin/sh` (the "shebang") says which program should run the file. To run it you make it executable (`chmod +x file`) and type `./file`.

```bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
```

Lines 2 to 19 are a **comment (#)** block with the Apache License, Version 2.0, the legal permission under which the Apache Software Foundation lets anyone use and copy this script. The shell ignores it.

> New words:
>
> - **comment (#)**: a line (or the end of a line) starting with `#` is ignored by the shell; it is a note for the reader. The first line `#!...` is the only `#` line that has a meaning.

```bash
# Apache Maven Wrapper startup batch script, version 3.3.4
#
# Optional ENV vars
# -----------------
#   JAVA_HOME - location of a JDK home dir, required when download maven via java source
#   MVNW_REPOURL - repo url base for downloading maven distribution
#   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
#   MVNW_VERBOSE - true: enable verbose log; debug: trace the mvnw script; others: silence the output
# ----------------------------------------------------------------------------
```

Another comment block. It names the script (the Maven Wrapper startup script, version 3.3.4, the same as `wrapperVersion` in the properties file) and lists the **environment variable**s that change its behavior: `JAVA_HOME` (where a JDK lives; needed only for the Java-based download), `MVNW_REPOURL` (a different place to download Maven from, for example a company mirror), `MVNW_USERNAME` and `MVNW_PASSWORD` (login for that mirror) and `MVNW_VERBOSE` (`true` prints what the script does; `debug` shows every command).

> New words:
>
> - **environment variable**: a named text setting that a program inherits from the process that started it, for example `LOG_LEVEL=DEBUG`. Programs read them to be configured without changing code.

```bash
set -euf
[ "${MVNW_VERBOSE-}" != debug ] || set -x
```

`set -euf` switches on three strict options (see **set -e -u -f -o pipefail**): stop at the first failing command (`-e`), treat an unset variable as an error (`-u`) and do not expand `*` in file names (`-f`). The next line is a **test ([ ... ])** combined with `||` (**&& and ||**): unless the variable `MVNW_VERBOSE` equals `debug`, do nothing; when it does equal `debug`, run `set -x`, which prints every command before running it. `${MVNW_VERBOSE-}` is the safe way to read a variable that may not be set (**${name-} and friends**).

> New words:
>
> - **set -e -u -f -o pipefail**: options that make a script strict. `-e` stop at the first failing command; `-u` treat an unset variable as an error; `-f` switch off filename patterns like `*` (so a stray `*` is not expanded); `-o pipefail` makes a pipe fail if any command in it fails, not only the last. `set +e` switches one off again; `set -x` prints every command before it runs (for debugging).
> - **test ([ ... ])**: `[ -n "$x" ]` is true if the text is not empty, `[ -z "$x" ]` if it is empty, `[ -d dir ]` if `dir` is a folder, `[ -f file ]` if it is a file, `[ -x file ]` if it is executable, `[ a = b ]` if two texts are equal, `[ ! ... ]` negates. It is really a command named `[` whose last argument is `]`, which is why the spaces inside are required.
> - **&& and ||**: `a && b` runs `b` only if `a` succeeded. `a || b` runs `b` only if `a` failed. A command *succeeds* when it exits with status 0 and fails with any other status. `a || :` means "run a, and if it fails do nothing" (`:` is a command that does nothing and succeeds).
> - **${name-} and friends**: `${NAME-}` means "the value of NAME, or nothing if it is not set". It exists because a script run with `set -u` would otherwise stop with an error for a variable that is not set. `${NAME:-text}` gives `text` when NAME is unset or empty. `${NAME:+text}` gives `text` only when NAME is set and not empty (otherwise nothing).

```bash
# OS specific support.
native_path() { printf %s\\n "$1"; }
case "$(uname)" in
CYGWIN* | MINGW*)
  [ -z "${JAVA_HOME-}" ] || JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  native_path() { cygpath --path --windows "$1"; }
  ;;
esac
```

Operating-system support. `native_path` is a **shell function** that prints its first argument (`printf %s\\n "$1"`: print the text and a line break); the doubled backslash in the script reaches `printf` as a single `\n`, which `printf` prints as a line break; it is the default for Unix systems, where no conversion is needed. The **case** statement looks at the output of `uname` (**uname**, inside **command substitution**). If the system is Cygwin or MinGW (Unix-like layers on Windows, whose names begin `CYGWIN` or `MINGW`), two things change: `JAVA_HOME` is converted to a Unix-style path with `cygpath --unix` (only if it is set and not empty), and `native_path` is redefined to convert in the other direction (`cygpath --path --windows`). The Windows tools later need Windows-style paths.

> New words:
>
> - **shell function**: `name() { commands; }` defines a command of your own. Inside it, `$1`, `$2` are its arguments. `return 1` ends it with failure.
> - **case**: `case WORD in pattern) commands ;; ... esac` compares a word with patterns in turn and runs the commands of the first match. `*` matches anything and is used as the last "otherwise"; `|` separates alternatives; `;;` ends a branch; `esac` closes the statement.
> - **uname**: prints the name of the operating system (`Darwin` for macOS, `Linux`, or something starting `MINGW`/`CYGWIN` for Windows shells).
> - **command substitution**: `$(command)` runs the command and is replaced by what it printed. `"$(dirname "$0")"` is the folder of the script.

```bash
# set JAVACMD and JAVACCMD
set_java_home() {
  # For Cygwin and MinGW, ensure paths are in Unix format before anything is touched
  if [ -n "${JAVA_HOME-}" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
      JAVACCMD="$JAVA_HOME/jre/sh/javac"
    else
      JAVACMD="$JAVA_HOME/bin/java"
      JAVACCMD="$JAVA_HOME/bin/javac"

      if [ ! -x "$JAVACMD" ] || [ ! -x "$JAVACCMD" ]; then
        echo "The JAVA_HOME environment variable is not defined correctly, so mvnw cannot run." >&2
        echo "JAVA_HOME is set to \"$JAVA_HOME\", but \"\$JAVA_HOME/bin/java\" or \"\$JAVA_HOME/bin/javac\" does not exist." >&2
        return 1
      fi
    fi
  else
```

The function `set_java_home` finds the Java program (`JAVACMD`) and the Java compiler (`JAVACCMD`). It is only used by the fallback that downloads Maven with Java. If `JAVA_HOME` is set (`[ -n ... ]`), then: if the file `$JAVA_HOME/jre/sh/java` is executable (`-x`) it is an IBM JDK on AIX with an unusual layout, and those paths are used; otherwise the usual `bin/java` and `bin/javac` are used, and if either is missing the function prints two error lines to the error stream (`>&2`, see **redirection**) and fails with `return 1`.

> New words:
>
> - **redirection**: `>file` sends what a command prints to a file; `>>file` appends; `<file` feeds a file to a command; `2>&1` sends the error stream to the same place as the normal output; `>&2` sends a message to the error stream (standard error); `>/dev/null` throws output away; `2>/dev/null` throws away only errors.

```bash
    JAVACMD="$(
      'set' +e
      'unset' -f command 2>/dev/null
      'command' -v java
    )" || :
    JAVACCMD="$(
      'set' +e
      'unset' -f command 2>/dev/null
      'command' -v javac
    )" || :

    if [ ! -x "${JAVACMD-}" ] || [ ! -x "${JAVACCMD-}" ]; then
      echo "The java/javac command does not exist in PATH nor is JAVA_HOME set, so mvnw cannot run." >&2
      return 1
    fi
  fi
}
```

If `JAVA_HOME` is not set, the function searches the **PATH**. `command -v java` prints the location of `java` (nothing if there is none). The odd lines around it, `'set' +e` and `'unset' -f command`, are defensive: quoting the word `set` stops any alias of the same name from interfering, `set +e` switches off "stop at the first error" inside this substitution so that a missing program does not end the script, and `unset -f command` removes any user-defined function that replaces `command`. The `|| :` (&& and ||) means "if that failed, do nothing". Finally, if either program is not executable, the function prints an error and fails.

> New words:
>
> - **PATH**: the list of folders in which the shell looks for a program when you type its bare name. `command -v java` prints where `java` was found, or nothing.

```bash
# hash string like Java String::hashCode
hash_string() {
  str="${1:-}" h=0
  while [ -n "$str" ]; do
    char="${str%"${str#?}"}"
    h=$(((h * 31 + $(LC_CTYPE=C printf %d "'$char")) % 4294967296))
    str="${str#?}"
  done
  printf %x\\n $h
}
```

`hash_string` computes a short hexadecimal fingerprint of a text, the same number that Java's `String.hashCode()` would give. It is used to give each download address its own folder. `str="${1:-}" h=0` sets two **shell variable**s: the text (first argument, empty if none) and the running number `h`. A `while` loop (`while [ -n "$str" ]`) repeats while text is left. `char="${str%"${str#?}"}"` takes the first character (`${str#?}` is the text without its first character, and removing that from the end leaves the first character; see **${name#...} ${name%...}**). The line `h=$(((h * 31 + code) % 4294967296))` is **$(( ))**: multiply the number by 31, add the character's code, and keep it within 32 bits (the remainder after dividing by 2 to the power 32). The code comes from `printf %d "'$char"`, where the leading single quote asks `printf` for the character code, and `LC_CTYPE=C` makes that independent of the user's language settings. `str="${str#?}"` drops the first character. After the loop `printf %x` prints the number in hexadecimal.

> New words:
>
> - **shell variable**: a named piece of text. `name=value` sets it (no spaces around `=`); `$name` or `${name}` reads it. Variables set in the environment (see environment variable) are passed on to the programs the script starts.
> - **${name#...} ${name%...}**: cut a piece off a variable's text. `${x##*/}` removes the longest start that ends in `/` (leaving the last part of a path); `${x%.*}` removes the shortest end that starts with `.` (an extension); `${x%/*}` removes the last `/` and what follows. `#` works from the front, `%` from the back; doubling takes the longest match.
> - **$(( ))**: arithmetic: `$(( (h * 31 + 7) % 4294967296 ))` computes with whole numbers; `%` is the remainder.

```bash
verbose() { :; }
[ "${MVNW_VERBOSE-}" != true ] || verbose() { printf %s\\n "${1-}"; }
```

Logging helpers. `verbose` is first defined as a function that does nothing (`:` is the do-nothing command). The second line is `[ "${MVNW_VERBOSE-}" != true ] || verbose() {...}`: the test is true unless `MVNW_VERBOSE` equals `true`, and when the test is true the `||` skips what follows. So the second definition, which prints its argument, replaces the first one only when `MVNW_VERBOSE` is exactly `true`.

```bash
die() {
  printf %s\\n "$1" >&2
  exit 1
}
```

`die` prints its first argument to the error stream and ends the script with status 1 (failure). Throughout the script `|| die "message"` means "if the previous command failed, stop with this message".

```bash
trim() {
  # MWRAPPER-139:
  #   Trims trailing and leading whitespace, carriage returns, tabs, and linefeeds.
  #   Needed for removing poorly interpreted newline sequences when running in more
  #   exotic environments such as mingw bash on Windows.
  printf "%s" "${1}" | tr -d '[:space:]'
}
```

`trim` removes all white space from its first argument. `tr -d '[:space:]'` deletes (`-d`) every character of the class "space" (blank, tab, line break, carriage return), read from the **pipe**. The comment explains why: on Windows, shells may leave a stray carriage-return character at the end of a line read from a file, which would corrupt the web address.

> New words:
>
> - **pipe**: `a | b` connects the output of `a` to the input of `b`, so the two run together. `a | b | c` chains more.

```bash
scriptDir="$(dirname "$0")"
scriptName="$(basename "$0")"
```

Two variables: `scriptDir` is the folder that holds `mvnw` and `scriptName` is its file name. `$0` is the name the script was started with (**$@, $0, $1**); `dirname` and `basename` are programs that cut a path into its folder and its last part.

> New words:
>
> - **$@, $0, $1**: `$0` is the name the script was started with, `$1`, `$2` ... are its arguments, `"$@"` is all of them, each kept as a separate word.

```bash
# parse distributionUrl and optional distributionSha256Sum, requires .mvn/wrapper/maven-wrapper.properties
while IFS="=" read -r key value; do
  case "${key-}" in
  distributionUrl) distributionUrl=$(trim "${value-}") ;;
  distributionSha256Sum) distributionSha256Sum=$(trim "${value-}") ;;
  esac
done <"$scriptDir/.mvn/wrapper/maven-wrapper.properties"
[ -n "${distributionUrl-}" ] || die "cannot read distributionUrl property in $scriptDir/.mvn/wrapper/maven-wrapper.properties"
```

Reads the properties file (**while read**): each line `key=value` is split at the first `=`. The case keeps two keys: `distributionUrl` and `distributionSha256Sum` (an optional checksum, not set in this repository), each passed through `trim`. The input of the loop is the file `.mvn/wrapper/maven-wrapper.properties` next to the script (`done <"..."`). The line after it is a guard: if `distributionUrl` is still empty the script stops with a message.

> New words:
>
> - **while read**: `while IFS='=' read -r key value; do ... done <file` reads the file line by line, splitting each line at the `=` into `key` and `value`. `IFS` is the character at which lines are cut; `-r` stops the shell from treating backslashes specially.

```bash
case "${distributionUrl##*/}" in
maven-mvnd-*bin.*)
  MVN_CMD=mvnd.sh _MVNW_REPO_PATTERN=/maven/mvnd/
  case "${PROCESSOR_ARCHITECTURE-}${PROCESSOR_ARCHITEW6432-}:$(uname -a)" in
  *AMD64:CYGWIN* | *AMD64:MINGW*) distributionPlatform=windows-amd64 ;;
  :Darwin*x86_64) distributionPlatform=darwin-amd64 ;;
  :Darwin*arm64) distributionPlatform=darwin-aarch64 ;;
  :Linux*x86_64*) distributionPlatform=linux-amd64 ;;
  *)
    echo "Cannot detect native platform for mvnd on $(uname)-$(uname -m), use pure java version" >&2
    distributionPlatform=linux-amd64
    ;;
  esac
  distributionUrl="${distributionUrl%-bin.*}-$distributionPlatform.zip"
  ;;
maven-mvnd-*) MVN_CMD=mvnd.sh _MVNW_REPO_PATTERN=/maven/mvnd/ ;;
*) MVN_CMD="mvn${scriptName#mvnw}" _MVNW_REPO_PATTERN=/org/apache/maven/ ;;
esac
```

Decides what kind of Maven it is. `${distributionUrl##*/}` is the last part of the web address, the file name (${name#...} ${name%...}): `apache-maven-3.9.16-bin.zip`. The first pattern, `maven-mvnd-*bin.*`, is for "mvnd", a different product (the Maven daemon). Its branch picks the platform name by examining the processor and system name, changes the file name accordingly and sets the command name to `mvnd.sh`. The second pattern, `maven-mvnd-*`, handles another mvnd file form. The last pattern `*` is the one used here: the command to run is `mvn` (`"mvn${scriptName#mvnw}"`: the script's own name without the leading `mvnw`, which is nothing for `mvnw`), and the repository path pattern is `/org/apache/maven/`. This repository uses only the last branch.

```bash
# apply MVNW_REPOURL and calculate MAVEN_HOME
# maven home pattern: ~/.m2/wrapper/dists/{apache-maven-<version>,maven-mvnd-<version>-<platform>}/<hash>
[ -z "${MVNW_REPOURL-}" ] || distributionUrl="$MVNW_REPOURL$_MVNW_REPO_PATTERN${distributionUrl#*"$_MVNW_REPO_PATTERN"}"
distributionUrlName="${distributionUrl##*/}"
distributionUrlNameMain="${distributionUrlName%.*}"
distributionUrlNameMain="${distributionUrlNameMain%-bin}"
MAVEN_USER_HOME="${MAVEN_USER_HOME:-${HOME}/.m2}"
MAVEN_HOME="${MAVEN_USER_HOME}/wrapper/dists/${distributionUrlNameMain-}/$(hash_string "$distributionUrl")"
```

Calculates where Maven will live. If `MVNW_REPOURL` is set (`[ -z ... ] ||` runs the assignment only when it is not empty), the part of the address before the repository pattern is replaced by that mirror. Then three file-name variables are derived with ${name#...} ${name%...}: `distributionUrlName` is `apache-maven-3.9.16-bin.zip`, `distributionUrlNameMain` is that without `.zip` and then without `-bin`, i.e. `apache-maven-3.9.16`. `MAVEN_USER_HOME` is `~/.m2` unless set. `MAVEN_HOME` becomes `~/.m2/wrapper/dists/apache-maven-3.9.16/<fingerprint>`, where the fingerprint is `hash_string` of the full address, so different download addresses never share a folder.

```bash
exec_maven() {
  unset MVNW_VERBOSE MVNW_USERNAME MVNW_PASSWORD MVNW_REPOURL || :
  exec "$MAVEN_HOME/bin/$MVN_CMD" "$@" || die "cannot exec $MAVEN_HOME/bin/$MVN_CMD"
}
```

`exec_maven` starts the real Maven. It first removes the wrapper's own variables from the environment (`unset`), so Maven does not see them. Then `exec "$MAVEN_HOME/bin/$MVN_CMD" "$@"` (**exec**, $@, $0, $1) replaces this script by Maven and passes on every argument exactly as typed. If `exec` itself fails, `die` reports it.

> New words:
>
> - **exec**: `exec program args` replaces the running script by the program, instead of starting it as a child and waiting. The script ends there; the program takes its place, keeping the same process.

```bash
if [ -d "$MAVEN_HOME" ]; then
  verbose "found existing MAVEN_HOME at $MAVEN_HOME"
  exec_maven "$@"
fi
```

The fast path, used on every run after the first: if the Maven folder already exists, say so (in verbose mode) and start Maven. `exec_maven` never returns, so nothing below this `if` runs.

```bash
case "${distributionUrl-}" in
*?-bin.zip | *?maven-mvnd-?*-?*.zip) ;;
*) die "distributionUrl is not valid, must match *-bin.zip or maven-mvnd-*.zip, but found '${distributionUrl-}'" ;;
esac
```

Safety check before downloading: the address must end in `-bin.zip` (a normal Maven binary distribution) or be an mvnd zip; otherwise the script stops and shows what it found. The pattern `*?-bin.zip` means "at least one character, then -bin.zip".

```bash
# prepare tmp dir
if TMP_DOWNLOAD_DIR="$(mktemp -d)" && [ -d "$TMP_DOWNLOAD_DIR" ]; then
  clean() { rm -rf -- "$TMP_DOWNLOAD_DIR"; }
  trap clean HUP INT TERM EXIT
else
  die "cannot create temp dir"
fi
```

Makes a temporary download folder. `mktemp -d` creates a new, uniquely named folder and prints its name. If that works and the folder exists, the function `clean` is defined to delete it (`rm -rf --`), and **trap** registers `clean` to run on `HUP`, `INT`, `TERM` and `EXIT`, that is, however the script ends. If the folder cannot be made, the script stops.

> New words:
>
> - **trap**: `trap command SIGNALS` registers a command to run when the script gets one of those signals or ends. `EXIT` means "whenever the script ends"; `INT` is Ctrl+C; `TERM` is the polite "please stop" signal; `HUP` is "terminal closed". It is how a script cleans up its temporary files even when it is interrupted.

```bash
mkdir -p -- "${MAVEN_HOME%/*}"
```

Creates the parent folder of the final Maven folder (`${MAVEN_HOME%/*}` removes the last path part, ${name#...} ${name%...}). `-p` creates every missing level and is silent if it exists; `--` marks the end of options.

```bash
# Download and Install Apache Maven
verbose "Couldn't find MAVEN_HOME, downloading and installing it ..."
verbose "Downloading from: $distributionUrl"
verbose "Downloading to: $TMP_DOWNLOAD_DIR/$distributionUrlName"
```

Three `verbose` lines that tell, in verbose mode, what is about to happen: where from and where to.

```bash
# select .zip or .tar.gz
if ! command -v unzip >/dev/null; then
  distributionUrl="${distributionUrl%.zip}.tar.gz"
  distributionUrlName="${distributionUrl##*/}"
fi
```

If the program `unzip` is not installed (`! command -v unzip`; the output of `command -v` is discarded with `>/dev/null`, see redirection), the script asks for the `.tar.gz` version of the same file instead, and updates the file name variable.

```bash
# verbose opt
__MVNW_QUIET_WGET=--quiet __MVNW_QUIET_CURL=--silent __MVNW_QUIET_UNZIP=-q __MVNW_QUIET_TAR=''
[ "${MVNW_VERBOSE-}" != true ] || __MVNW_QUIET_WGET='' __MVNW_QUIET_CURL='' __MVNW_QUIET_UNZIP='' __MVNW_QUIET_TAR=v
```

Chooses how chatty the download and unpack programs are. By default the variables hold the quiet options of `wget` (`--quiet`), `curl` (`--silent`) and `unzip` (`-q`), and `tar` gets none. In verbose mode they are emptied, and `tar` gets `v` (verbose).

```bash
# normalize http auth
case "${MVNW_PASSWORD:+has-password}" in
'') MVNW_USERNAME='' MVNW_PASSWORD='' ;;
has-password) [ -n "${MVNW_USERNAME-}" ] || MVNW_USERNAME='' MVNW_PASSWORD='' ;;
esac
```

Cleans the optional login for a private mirror. `${MVNW_PASSWORD:+has-password}` is empty if there is no password (${name-} and friends). With no password, both username and password are set to empty. With a password but no username, both are emptied too. Only a complete pair is kept.

```bash
if [ -z "${MVNW_USERNAME-}" ] && command -v wget >/dev/null; then
  verbose "Found wget ... using wget"
  wget ${__MVNW_QUIET_WGET:+"$__MVNW_QUIET_WGET"} "$distributionUrl" -O "$TMP_DOWNLOAD_DIR/$distributionUrlName" || die "wget: Failed to fetch $distributionUrl"
elif [ -z "${MVNW_USERNAME-}" ] && command -v curl >/dev/null; then
  verbose "Found curl ... using curl"
  curl ${__MVNW_QUIET_CURL:+"$__MVNW_QUIET_CURL"} -f -L -o "$TMP_DOWNLOAD_DIR/$distributionUrlName" "$distributionUrl" || die "curl: Failed to fetch $distributionUrl"
```

Chooses the download program, in order of preference. If there is no login and `wget` exists, use it: it downloads the address (`"$distributionUrl"`) into the temporary folder (`-O file`); if it fails, `die`. Otherwise, if no login and `curl` exists, use `curl` with `-f` (treat an HTTP error as failure), `-L` (follow redirects) and `-o file`. Note the use of `${VAR:+"$VAR"}` to add the quiet option only when it is non-empty.

```bash
elif set_java_home; then
  verbose "Falling back to use Java to download"
  javaSource="$TMP_DOWNLOAD_DIR/Downloader.java"
  targetZip="$TMP_DOWNLOAD_DIR/$distributionUrlName"
  cat >"$javaSource" <<-END
	public class Downloader extends java.net.Authenticator
	{
	  protected java.net.PasswordAuthentication getPasswordAuthentication()
	  {
	    return new java.net.PasswordAuthentication( System.getenv( "MVNW_USERNAME" ), System.getenv( "MVNW_PASSWORD" ).toCharArray() );
	  }
	  public static void main( String[] args ) throws Exception
	  {
	    setDefault( new Downloader() );
	    java.nio.file.Files.copy( java.net.URI.create( args[0] ).toURL().openStream(), java.nio.file.Paths.get( args[1] ).toAbsolutePath().normalize() );
	  }
	}
	END
  # For Cygwin/MinGW, switch paths to Windows format before running javac and java
  verbose " - Compiling Downloader.java ..."
  "$(native_path "$JAVACCMD")" "$(native_path "$javaSource")" || die "Failed to compile Downloader.java"
  verbose " - Running Downloader.java ..."
  "$(native_path "$JAVACMD")" -cp "$(native_path "$TMP_DOWNLOAD_DIR")" Downloader "$distributionUrl" "$(native_path "$targetZip")"
fi
```

The last resort when neither `wget` nor `curl` is available: use Java itself (this is what happens in a plain JDK container). `set_java_home` must succeed. Then the script writes a small Java program into the temporary folder with a **here document** (`cat >"$javaSource" <<-END`, up to the line `END`): the class `Downloader` has a `main` method that copies the content of the web address (`openStream()`) to the target file (`Files.copy`), and it can answer a login request from the environment variables. The lines inside the here document are indented with tab characters, which `<<-` strips. After the here document, the comment explains the next two commands: `javac` compiles `Downloader.java`, and `java -cp <folder> Downloader <address> <target>` runs it. This path is slow on a starved machine, which is why the Dockerfile in this repository installs `curl` first.

> New words:
>
> - **here document**: `cat >file <<-END ... END` writes the lines between the markers into the file. `<<-` also removes leading tab characters, so the text can be indented in the script.

```bash
# If specified, validate the SHA-256 sum of the Maven distribution zip file
if [ -n "${distributionSha256Sum-}" ]; then
  distributionSha256Result=false
  if [ "$MVN_CMD" = mvnd.sh ]; then
    echo "Checksum validation is not supported for maven-mvnd." >&2
    echo "Please disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties." >&2
    exit 1
  elif command -v sha256sum >/dev/null; then
    if echo "$distributionSha256Sum  $TMP_DOWNLOAD_DIR/$distributionUrlName" | sha256sum -c - >/dev/null 2>&1; then
      distributionSha256Result=true
    fi
  elif command -v shasum >/dev/null; then
    if echo "$distributionSha256Sum  $TMP_DOWNLOAD_DIR/$distributionUrlName" | shasum -a 256 -c >/dev/null 2>&1; then
      distributionSha256Result=true
    fi
  else
    echo "Checksum validation was requested but neither 'sha256sum' or 'shasum' are available." >&2
    echo "Please install either command, or disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties." >&2
    exit 1
  fi
  if [ $distributionSha256Result = false ]; then
    echo "Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised." >&2
    echo "If you updated your Maven version, you need to update the specified distributionSha256Sum property." >&2
    exit 1
  fi
fi
```

Optional integrity check. If `distributionSha256Sum` was in the properties file (it is not here), the script compares the SHA-256 fingerprint of the downloaded file with it. It refuses for mvnd; otherwise it uses `sha256sum` (Linux) or, if that is missing, `shasum -a 256` (macOS). `... | sha256sum -c -` reads a line "checksum  file" from the pipe and checks it; the result is remembered in `distributionSha256Result`. If neither program exists, or the check fails, the script prints an explanation and exits with status 1, because a mismatch means the download may have been tampered with.

```bash
# unzip and move
if command -v unzip >/dev/null; then
  unzip ${__MVNW_QUIET_UNZIP:+"$__MVNW_QUIET_UNZIP"} "$TMP_DOWNLOAD_DIR/$distributionUrlName" -d "$TMP_DOWNLOAD_DIR" || die "failed to unzip"
else
  tar xzf${__MVNW_QUIET_TAR:+"$__MVNW_QUIET_TAR"} "$TMP_DOWNLOAD_DIR/$distributionUrlName" -C "$TMP_DOWNLOAD_DIR" || die "failed to untar"
fi
```

Unpacks the download into the temporary folder: `unzip` (with the quiet option, `-d` names the target folder) if available, otherwise `tar xzf` (extract, gunzip, from file) with `-C` for the target folder. A failure stops the script.

```bash
# Find the actual extracted directory name (handles snapshots where filename != directory name)
actualDistributionDir=""

# First try the expected directory name (for regular distributions)
if [ -d "$TMP_DOWNLOAD_DIR/$distributionUrlNameMain" ]; then
  if [ -f "$TMP_DOWNLOAD_DIR/$distributionUrlNameMain/bin/$MVN_CMD" ]; then
    actualDistributionDir="$distributionUrlNameMain"
  fi
fi
```

Finds the folder that the archive created. `actualDistributionDir` starts empty. First it tries the expected name (`apache-maven-3.9.16`): if that folder exists and contains `bin/mvn`, it is the answer.

```bash
# If not found, search for any directory with the Maven executable (for snapshots)
```

If the expected folder was not found (a snapshot build can use a different folder name), `set +f` switches file-name patterns on again, and a **for loop** loop goes over everything in the temporary folder (`"$TMP_DOWNLOAD_DIR"/*`, a **glob pattern**): the first folder containing `bin/mvn` wins (`break` leaves the loop). `set -f` switches patterns off again.

> New words:
>
> - **for loop**: `for x in a b c; do ...; done` runs the commands once for each word; `for dir in /path/*` goes over the files matching a pattern.
> - **glob pattern**: `*` stands for any text and `?` for one character in a file name pattern: `*.zip` matches every name ending in `.zip`. The shell replaces the pattern by the matching names before the command runs.

```bash
if [ -z "$actualDistributionDir" ]; then
  # enable globbing to iterate over items
  set +f
  for dir in "$TMP_DOWNLOAD_DIR"/*; do
    if [ -d "$dir" ]; then
      if [ -f "$dir/bin/$MVN_CMD" ]; then
        actualDistributionDir="$(basename "$dir")"
        break
      fi
    fi
  done
  set -f
fi

if [ -z "$actualDistributionDir" ]; then
  verbose "Contents of $TMP_DOWNLOAD_DIR:"
  verbose "$(ls -la "$TMP_DOWNLOAD_DIR")"
  die "Could not find Maven distribution directory in extracted archive"
fi
```

If still nothing was found, the script lists the folder (in verbose mode) and stops with a clear message.

```bash
verbose "Found extracted Maven distribution directory: $actualDistributionDir"
printf %s\\n "$distributionUrl" >"$TMP_DOWNLOAD_DIR/$actualDistributionDir/mvnw.url"
mv -- "$TMP_DOWNLOAD_DIR/$actualDistributionDir" "$MAVEN_HOME" || [ -d "$MAVEN_HOME" ] || die "fail to move MAVEN_HOME"

clean || :
exec_maven "$@"
```

Final steps. It records, in a file `mvnw.url` inside the new Maven folder, the address it came from. Then `mv` moves the folder to its final place `$MAVEN_HOME`. The `|| [ -d "$MAVEN_HOME" ] || die` means: if the move failed but the destination now exists (another run of `mvnw` finished first at the same moment), that is fine; otherwise stop. `clean || :` runs the cleanup function now (the temporary folder), ignoring problems, and `exec_maven "$@"` finally starts Maven with the user's arguments. The script's job is done.

### `mvnw.cmd`, line by line (Windows)

```bat
<# : batch portion
@REM ----------------------------------------------------------------------------
```

A Windows file that is two programs in one. Windows `cmd.exe` runs files ending in `.cmd` as **batch files**: a list of commands, one per line. PowerShell is Windows' more powerful scripting language. This file is valid in both: the first line `<# : batch portion` is a trick. To `cmd.exe` it is a harmless line (the characters before the colon are ignored as a label); to PowerShell `<#` opens a block comment. So `cmd.exe` runs the batch part at the top and PowerShell runs the part after the comment closes (`#>`, at line 46). The batch part's only job is to start PowerShell on this same file.

```bat
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
```

`@REM` starts a comment in a batch file (the `@` stops the line from being shown). These lines are the Apache License 2.0 text, the same as at the top of `mvnw`.

```bat
@REM Apache Maven Wrapper startup batch script, version 3.3.4
@REM
@REM Optional ENV vars
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM ----------------------------------------------------------------------------
```

A comment block that names the script (Maven Wrapper startup script version 3.3.4) and lists the environment variables it understands: `MVNW_REPOURL` (a different download location), `MVNW_USERNAME` and `MVNW_PASSWORD` (login for it) and `MVNW_VERBOSE` (`true` prints progress).

```bat
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
```

The batch part begins. `%name%` reads an environment variable in batch files. This line sets `__MVNW_ARG0_NAME__` to the script's own file name (`%~nx0` means name and extension of argument 0) unless it is already set. The next lines clear two helper variables, save the current `PSModulePath` (the places where PowerShell looks for modules) and empty it, so a broken PowerShell setup on the machine cannot interfere.

```bat
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $script='%__MVNW_ARG0_NAME__%'; icm -ScriptBlock ([Scriptblock]::Create((Get-Content -Raw '%~f0'))) -NoNewScope}"`) DO @(
  IF "%%A"=="MVN_CMD" (set __MVNW_CMD__=%%B) ELSE IF "%%B"=="" (echo %%A) ELSE (echo %%A=%%B)
)
```

The central line. It runs a PowerShell command (inside the backticks) and reads what it prints, line by line, splitting each line at the first `=` into `%%A` (before) and `%%B` (after). The PowerShell command reads *this very file* (`Get-Content -Raw '%~f0'`, where `%~f0` is the full path of the script), turns its text into a script block and runs it (`icm` is short for `Invoke-Command`), passing in the script's folder (`%~dp0`) and name. That is the PowerShell half of the file, below. Its output is the line `MVN_CMD=<path to mvn.cmd>`, possibly preceded by other messages. For each line: if the key is `MVN_CMD`, the batch variable `__MVNW_CMD__` is set to the value; otherwise the line is shown (`echo`), restored to `key=value` form if it had one.

```bat
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@SET MVNW_USERNAME=
@SET MVNW_PASSWORD=
```

Cleanup: restore the saved `PSModulePath` and empty the helper variables, including the username and password, so Maven does not receive them.

```bat
@IF NOT "%__MVNW_CMD__%"=="" ("%__MVNW_CMD__%" %*)
@echo Cannot start maven from wrapper >&2 && exit /b 1
@GOTO :EOF
```

If PowerShell found the Maven command, run it (`"%__MVNW_CMD__%" %*`; `%*` stands for all arguments typed by the user). When Maven finishes, the `cmd.exe` script goes on to the next line only if Maven could not be started at all: `@echo Cannot start maven from wrapper >&2 && exit /b 1` prints an error to the error stream and ends with status 1. (A batch file that runs another program continues after it ends, unlike `exec` in the Unix script; the `GOTO :EOF` on the next line jumps to the end of the file so the PowerShell text is never read as batch commands.)

```bat
: end batch / begin powershell #>
```

The line that closes PowerShell's block comment (`#>`) and, for `cmd.exe`, is a label that is never reached. From here on the file is PowerShell code.

```bat
$ErrorActionPreference = "Stop"
if ($env:MVNW_VERBOSE -eq "true") {
  $VerbosePreference = "Continue"
}
```

PowerShell begins. `$ErrorActionPreference = "Stop"` makes any error end the script (like `set -e` in the Unix version). If `MVNW_VERBOSE` is `true`, `$VerbosePreference = "Continue"` turns on the output of `Write-Verbose`.

```bat
# calculate distributionUrl, requires .mvn/wrapper/maven-wrapper.properties
$distributionUrl = (Get-Content -Raw "$scriptDir/.mvn/wrapper/maven-wrapper.properties" | ConvertFrom-StringData).distributionUrl
if (!$distributionUrl) {
  Write-Error "cannot read distributionUrl property in $scriptDir/.mvn/wrapper/maven-wrapper.properties"
}
```

Reads `maven-wrapper.properties` as a block of text (`Get-Content -Raw`) and converts the `key=value` lines to a table (`ConvertFrom-StringData`), then takes the entry `distributionUrl`. If it is empty (`!$distributionUrl`), `Write-Error` stops the script with a message.

```bat
switch -wildcard -casesensitive ( $($distributionUrl -replace '^.*/','') ) {
  "maven-mvnd-*" {
    $USE_MVND = $true
    $distributionUrl = $distributionUrl -replace '-bin\.[^.]*$',"-windows-amd64.zip"
    $MVN_CMD = "mvnd.cmd"
    break
  }
  default {
    $USE_MVND = $false
    $MVN_CMD = $script -replace '^mvnw','mvn'
    break
  }
}
```

A `switch` over the file name part of the address (`-replace '^.*/',''` removes everything up to the last `/`, a regular expression). If the name begins with `maven-mvnd-` it is the Maven daemon (a different product): the address is rewritten for Windows and the command is `mvnd.cmd`. Otherwise (the case here) `$USE_MVND` is false and `$MVN_CMD` is the script's own name with the leading `mvnw` replaced by `mvn`, so `mvn.cmd`.

```bat
# apply MVNW_REPOURL and calculate MAVEN_HOME
# maven home pattern: ~/.m2/wrapper/dists/{apache-maven-<version>,maven-mvnd-<version>-<platform>}/<hash>
if ($env:MVNW_REPOURL) {
  $MVNW_REPO_PATTERN = if ($USE_MVND -eq $False) { "/org/apache/maven/" } else { "/maven/mvnd/" }
  $distributionUrl = "$env:MVNW_REPOURL$MVNW_REPO_PATTERN$($distributionUrl -replace "^.*$MVNW_REPO_PATTERN",'')"
}
$distributionUrlName = $distributionUrl -replace '^.*/',''
$distributionUrlNameMain = $distributionUrlName -replace '\.[^.]*$','' -replace '-bin$',''
```

If `MVNW_REPOURL` is set, the part of the address before the repository path is replaced by it. Then two names are derived by regular-expression replacement: the file name (`apache-maven-3.9.16-bin.zip`) and the main name (`apache-maven-3.9.16`: extension removed, then `-bin` removed).

```bat
$MAVEN_M2_PATH = "$HOME/.m2"
if ($env:MAVEN_USER_HOME) {
  $MAVEN_M2_PATH = "$env:MAVEN_USER_HOME"
}

if (-not (Test-Path -Path $MAVEN_M2_PATH)) {
    New-Item -Path $MAVEN_M2_PATH -ItemType Directory | Out-Null
}

$MAVEN_WRAPPER_DISTS = $null
if ((Get-Item $MAVEN_M2_PATH).Target[0] -eq $null) {
  $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
} else {
  $MAVEN_WRAPPER_DISTS = (Get-Item $MAVEN_M2_PATH).Target[0] + "/wrapper/dists"
}
```

The Maven folder in the user's home, unless `MAVEN_USER_HOME` says otherwise. If it does not exist it is created (`New-Item -ItemType Directory`; `| Out-Null` throws away the output). If it is a link to another folder (`.Target[0]` is not null), the link target is used, so the wrapper folder lives where the link points.

```bat
$MAVEN_HOME_PARENT = "$MAVEN_WRAPPER_DISTS/$distributionUrlNameMain"
$MAVEN_HOME_NAME = ([System.Security.Cryptography.SHA256]::Create().ComputeHash([byte[]][char[]]$distributionUrl) | ForEach-Object {$_.ToString("x2")}) -join ''
$MAVEN_HOME = "$MAVEN_HOME_PARENT/$MAVEN_HOME_NAME"
```

Calculates the final Maven folder: `dists/apache-maven-3.9.16/<hash>`. The hash here is a SHA-256 fingerprint of the address in hexadecimal (`ToString("x2")` prints each byte as two hex digits), whereas the Unix script uses a shorter Java-style hash; the two scripts therefore keep separate folders.

```bat
if (Test-Path -Path "$MAVEN_HOME" -PathType Container) {
  Write-Verbose "found existing MAVEN_HOME at $MAVEN_HOME"
  Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
  exit $?
}
```

The fast path: if the folder already exists, print `MVN_CMD=<path to mvn.cmd>` (read by the batch part) and stop.

```bat
if (! $distributionUrlNameMain -or ($distributionUrlName -eq $distributionUrlNameMain)) {
  Write-Error "distributionUrl is not valid, must end with *-bin.zip, but found $distributionUrl"
}
```

A safety check: the address must end in `-bin.zip`, otherwise the main name equals the full name and the script stops.

```bat
# prepare tmp dir
$TMP_DOWNLOAD_DIR_HOLDER = New-TemporaryFile
$TMP_DOWNLOAD_DIR = New-Item -Itemtype Directory -Path "$TMP_DOWNLOAD_DIR_HOLDER.dir"
$TMP_DOWNLOAD_DIR_HOLDER.Delete() | Out-Null
trap {
  if ($TMP_DOWNLOAD_DIR.Exists) {
    try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null }
    catch { Write-Warning "Cannot remove $TMP_DOWNLOAD_DIR" }
  }
}
```

Creates a temporary folder: `New-TemporaryFile` makes a unique temporary file, a folder with the same name plus `.dir` is created, and the file is deleted. The `trap { ... }` block runs if the script fails: it deletes the temporary folder (`Remove-Item -Recurse -Force`), warning if that fails.

```bat
New-Item -Itemtype Directory -Path "$MAVEN_HOME_PARENT" -Force | Out-Null

# Download and Install Apache Maven
Write-Verbose "Couldn't find MAVEN_HOME, downloading and installing it ..."
Write-Verbose "Downloading from: $distributionUrl"
Write-Verbose "Downloading to: $TMP_DOWNLOAD_DIR/$distributionUrlName"
```

Creates the parent folder of the Maven folder (`-Force` means no error if it exists), then three `Write-Verbose` lines say what is about to happen.

```bat
$webclient = New-Object System.Net.WebClient
if ($env:MVNW_USERNAME -and $env:MVNW_PASSWORD) {
  $webclient.Credentials = New-Object System.Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD)
}
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$webclient.DownloadFile($distributionUrl, "$TMP_DOWNLOAD_DIR/$distributionUrlName") | Out-Null
```

Downloads the zip. A .NET `WebClient` object is created; if a username and password are both set they are used as credentials. `SecurityProtocol = Tls12` forces a modern secure protocol (older Windows would otherwise try a weaker one). `DownloadFile` fetches the address into the temporary folder.

```bat
# If specified, validate the SHA-256 sum of the Maven distribution zip file
$distributionSha256Sum = (Get-Content -Raw "$scriptDir/.mvn/wrapper/maven-wrapper.properties" | ConvertFrom-StringData).distributionSha256Sum
if ($distributionSha256Sum) {
  if ($USE_MVND) {
    Write-Error "Checksum validation is not supported for maven-mvnd. `nPlease disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties."
  }
  Import-Module $PSHOME\Modules\Microsoft.PowerShell.Utility -Function Get-FileHash
  if ((Get-FileHash "$TMP_DOWNLOAD_DIR/$distributionUrlName" -Algorithm SHA256).Hash.ToLower() -ne $distributionSha256Sum) {
    Write-Error "Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised. If you updated your Maven version, you need to update the specified distributionSha256Sum property."
  }
}
```

Optional integrity check, as in the Unix script: if `distributionSha256Sum` is in the properties file (it is not here), the downloaded file's SHA-256 (`Get-FileHash`) must match it, else the script stops with an error saying the download may be compromised. mvnd does not support this.

```bat
# unzip and move
Expand-Archive "$TMP_DOWNLOAD_DIR/$distributionUrlName" -DestinationPath "$TMP_DOWNLOAD_DIR" | Out-Null
```

Unpacks the zip into the temporary folder with `Expand-Archive`.

```bat
# Find the actual extracted directory name (handles snapshots where filename != directory name)
$actualDistributionDir = ""

# First try the expected directory name (for regular distributions)
$expectedPath = Join-Path "$TMP_DOWNLOAD_DIR" "$distributionUrlNameMain"
$expectedMvnPath = Join-Path "$expectedPath" "bin/$MVN_CMD"
if ((Test-Path -Path $expectedPath -PathType Container) -and (Test-Path -Path $expectedMvnPath -PathType Leaf)) {
  $actualDistributionDir = $distributionUrlNameMain
}
```

Looks for the folder that was created. First the expected name (`apache-maven-3.9.16`): `Join-Path` joins path parts, and the folder counts if it contains `bin/mvn.cmd` (`Test-Path -PathType Leaf` tests for a file).

```bat
# If not found, search for any directory with the Maven executable (for snapshots)
if (!$actualDistributionDir) {
  Get-ChildItem -Path "$TMP_DOWNLOAD_DIR" -Directory | ForEach-Object {
    $testPath = Join-Path $_.FullName "bin/$MVN_CMD"
    if (Test-Path -Path $testPath -PathType Leaf) {
      $actualDistributionDir = $_.Name
    }
  }
}

if (!$actualDistributionDir) {
  Write-Error "Could not find Maven distribution directory in extracted archive"
}
```

Otherwise it goes through every folder in the temporary directory (`Get-ChildItem -Directory | ForEach-Object`) and takes one that contains `bin/mvn.cmd`. If none is found, the script stops with a message.

```bat
Write-Verbose "Found extracted Maven distribution directory: $actualDistributionDir"
Rename-Item -Path "$TMP_DOWNLOAD_DIR/$actualDistributionDir" -NewName $MAVEN_HOME_NAME | Out-Null
try {
  Move-Item -Path "$TMP_DOWNLOAD_DIR/$MAVEN_HOME_NAME" -Destination $MAVEN_HOME_PARENT | Out-Null
} catch {
  if (! (Test-Path -Path "$MAVEN_HOME" -PathType Container)) {
    Write-Error "fail to move MAVEN_HOME"
  }
} finally {
  try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null }
  catch { Write-Warning "Cannot remove $TMP_DOWNLOAD_DIR" }
}

Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
```

Final steps. The folder is renamed to the hash name and moved into the parent folder with `Move-Item`. If the move fails but the destination exists (another run finished first), that is fine; otherwise it is an error. The `finally` block always deletes the temporary folder. The last line prints `MVN_CMD=<path>`, which the batch part reads, and the batch part then starts Maven.

## 6. Try it yourself

- `./mvnw -v`. Expected: `Apache Maven 3.9.16`, then the Java version `25.0.x`.
- `cat .mvn/wrapper/maven-wrapper.properties`. Expected: the three lines shown above.
- `ls ~/.m2/wrapper/dists/apache-maven-3.9.16/`. Expected: one folder with a hash name, holding the unpacked Maven.
- `ls -l mvnw`. Expected: a line starting `-rwx`, so it is executable.
- `MVNW_VERBOSE=true ./mvnw -v`. Expected: extra lines starting `found existing MAVEN_HOME at ...` (the fast path) -- the script narrating its steps.

## 7. Mistakes people make

- Running plain `mvn` instead of `./mvnw`. That uses whatever Maven is installed, maybe a different version.
- A Docker image without `curl` or `wget`. The `eclipse-temurin` JDK image has neither, so the `only-script` wrapper falls back to compiling a Java downloader, which is very slow on a starved machine; the Dockerfile here installs `curl` first.
- Committing `target/` or `~/.m2`. Only the wrapper files are committed.
- Forgetting that the first build needs the network.
- Editing `mvnw`. It is generated by `mvn wrapper:wrapper -Dmaven=3.9.16`; change the properties file or regenerate.

## 8. Check yourself

1. Why does the repository contain `mvnw`?
   **Answer:** So the build runs with the pinned Maven 3.9.16 on any machine, without installing Maven.
2. What does `distributionType=only-script` mean?
   **Answer:** Only the shell script is checked in; there is no wrapper jar. The script downloads Maven itself.
3. What are the `--add-exports` lines in `.mvn/jvm.config` for?
   **Answer:** They let the code formatter reach the JDK compiler's internal parser, which Java 25 hides by default.
4. Where is Maven stored after the first run?
   **Answer:** In `~/.m2/wrapper/dists/`.
5. What command prints the Maven version used?
   **Answer:** `./mvnw -v`.

## 9. Further reading

- Maven Wrapper 3.3.x: https://maven.apache.org/tools/wrapper/
- Maven 3.9.16 download page: https://maven.apache.org/download.cgi
