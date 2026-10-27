# Maven Shade Plugin (3.6.2)

## 1. What it is

The Shade plugin builds an **uber jar** (also called fat jar): one `.jar` that contains the project's classes and every library it needs. A jar is a zip file of compiled Java classes. Normally you would have to put the libraries on the class path yourself; the uber jar makes `java -jar file.jar` enough.

## 2. Why this repository uses it

`make run` and the Docker image start the program with `java -jar target/spend.jar`. One file is easy to copy into a container and to run anywhere.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | the plugin, bound to the `package` phase, with its transformers and filters |
| `Dockerfile` | copies the shaded jar into the runtime image |
| `Makefile` | `JAR := target/spend.jar` and every `java -jar $(JAR)` |

## 4. How it works underneath

- The jar plugin first writes a thin jar with only this project's classes.
- The shade plugin then reads the thin jar and the jars of all runtime dependencies, copies their classes and resources into one new jar, applies the transformers (rewrite or merge certain files) and the filters (skip certain files), and replaces the thin jar. The unshaded one is kept as `target/original-spend.jar`.
- At run time the JVM opens one zip, reads `META-INF/MANIFEST.MF`, finds `Main-Class`, and calls its `main` method.

## 5. Walkthrough of this repository's use

```xml
        <artifactId>maven-shade-plugin</artifactId>
        <version>${shade-plugin.version}</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <createDependencyReducedPom>false</createDependencyReducedPom>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>${main.class}</mainClass>
                  <manifestEntries>
                    <Multi-Release>true</Multi-Release>
                  </manifestEntries>
                </transformer>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
              </transformers>
              <filters>
                <filter>
                  <artifact>*:*</artifact>
                  <excludes>
                    <exclude>META-INF/*.SF</exclude>
                    <exclude>META-INF/*.DSA</exclude>
                    <exclude>META-INF/*.RSA</exclude>
                    <exclude>META-INF/MANIFEST.MF</exclude>
                    <exclude>module-info.class</exclude>
                    <exclude>META-INF/versions/*/module-info.class</exclude>
                  </excludes>
                </filter>
              </filters>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

- `<phase>package</phase>` and the goal `shade`: the plugin runs when the `package` phase runs, right after the normal jar plugin has made `target/spend.jar`, and replaces that file.
- `createDependencyReducedPom` false: the plugin would otherwise write an extra `dependency-reduced-pom.xml` into the project folder. The `.gitignore` also lists that name, as a second guard.
- `ManifestResourceTransformer` writes the jar's `META-INF/MANIFEST.MF` file. `<mainClass>${main.class}</mainClass>` becomes the line `Main-Class: ...`, which is what `java -jar` runs. The entry `Multi-Release: true` marks the jar as one that may hold versioned class files for several Java releases (some libraries ship such files; without the flag the JVM would ignore them).
- `ServicesResourceTransformer` merges files under `META-INF/services/`. Java finds plugins by reading these files (the `ServiceLoader` mechanism: SLF4J uses it to find Logback). If two libraries each had one and the plugin kept only one, a plugin would silently go missing.
- `<filters>` leaves out signature files (`*.SF`, `*.DSA`, `*.RSA`), the old manifests of the libraries and `module-info.class`. A signature is only valid for the original jar; copying it into a merged jar would make the JVM refuse to start with a `SecurityException`.

## 6. Try it yourself

- `./mvnw -q -B clean package -DskipTests -Djacoco.skip=true && unzip -p target/spend.jar META-INF/MANIFEST.MF`. Expected: lines including `Main-Class: com.plainround.spend.interfaces.cli.SpendCli` and `Multi-Release: true`.
- `unzip -l target/spend.jar | grep -c logback`. Expected: a number above 100; Logback's classes are inside.
- `unzip -l target/spend.jar | grep META-INF/services`. Expected: the merged service files of the libraries.
- `java -jar target/spend.jar --help` (spending report) or see the README for the command of this project.

## 7. Mistakes people make

- Copying signature files into the merged jar: `Invalid signature file digest`. The filters prevent it.
- Dropping `META-INF/services` entries by keeping only one: a library (here SLF4J finding Logback) stops working with no error. The services transformer merges them.
- Shading when you publish a library for others to depend on: it can duplicate classes. Fine for an application, a design decision for a library.
- Running the unit tests against the fat jar: they run before `package` and use plain class files.

## 8. Check yourself

1. What does the jar contain after shading?
   **Answer:** This project's classes plus the classes and resources of all runtime dependencies.
2. Which manifest line makes `java -jar` work?
   **Answer:** `Main-Class`, written from the `mainClass` setting.
3. Why are `*.SF`, `*.DSA` and `*.RSA` files excluded?
   **Answer:** They are signatures of the original jars and would be wrong for the merged jar.
4. What does `ServicesResourceTransformer` protect?
   **Answer:** The `META-INF/services` files that Java's `ServiceLoader` uses to find implementations, such as SLF4J finding Logback.
5. Where is the unshaded jar?
   **Answer:** `target/original-spend.jar`.

## 9. Further reading

- Maven Shade Plugin 3.6.2: https://maven.apache.org/plugins/maven-shade-plugin/
- Resource transformers: https://maven.apache.org/plugins/maven-shade-plugin/examples/resource-transformers.html
- The JAR file specification: https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html
