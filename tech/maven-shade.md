# Maven Shade Plugin (3.6.2)

## 1. What it is

A **jar** is a zip file of compiled Java classes. A normal jar holds only your own classes; to run the program you must also hand Java the jars of every library on the class path. An **uber jar** (or fat jar) is one jar that contains your classes *and* the classes of all the libraries, so that `java -jar file.jar` is enough. The **Shade plugin** builds it ("shade" is the plugin's word for repackaging).

## 2. Why this repository uses it

`make run` and the Docker image start the program with `java -jar target/spend.jar`. One file is easy to copy into a container and to run anywhere.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | the plugin, bound to the `package` phase, with its transformers and filters |
| `Dockerfile` | copies the shaded jar into the runtime image |
| `Makefile` | `JAR := target/spend.jar` and every `java -jar $(JAR)` |

## 4. How it works underneath

1. The jar plugin first writes a thin jar with only this project's classes (`target/spend.jar`).
2. The Shade plugin then reads the thin jar and the jars of all runtime dependencies, copies their classes and resources into one new jar, applies the transformers (rewrite or merge certain files) and the filters (skip certain files), and replaces the thin jar. The unshaded one is kept as `target/original-spend.jar`.
3. At run time the JVM opens the one zip, reads `META-INF/MANIFEST.MF`, finds `Main-Class`, and calls that class's `main` method.

## 5. Walkthrough of this repository's use

```xml
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>${shade-plugin.version}</version>
        <executions>
          <execution>
```

The plugin entry: group `org.apache.maven.plugins`, artifact `maven-shade-plugin`, version from the property `shade-plugin.version` (3.6.2).

```xml
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
```

One **execution** without an id (Maven calls it `default`). `<phase>package</phase>` binds it to the `package` phase, after the normal jar plugin has written `target/spend.jar` from the project's own classes; `<goal>shade</goal>` is the job: build the single jar from that jar plus all runtime libraries and replace the file.

> New words:
>
> - **execution**: one configured run of a plugin goal inside a `<plugin>`: an optional `<id>` (its name), the `<phase>` it is bound to, its `<goals>` and its own `<configuration>`.

```xml
              <createDependencyReducedPom>false</createDependencyReducedPom>
```

`false`: the plugin would otherwise write an extra file `dependency-reduced-pom.xml` into the project folder. The `.gitignore` also lists that name as a second guard.

```xml
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
```

A **transformer** changes or merges certain files while the plugin copies everything into the one jar. The first, `ManifestResourceTransformer`, writes the jar's manifest, `META-INF/MANIFEST.MF` (a small text file of `Name: value` lines that describes the jar).

```xml
                  <mainClass>${main.class}</mainClass>
```

`<mainClass>${main.class}</mainClass>` becomes the manifest line `Main-Class: com.plainround.spend.interfaces.cli.SpendCli`. `java -jar` starts exactly that class's `main` method.

```xml
                  <manifestEntries>
                    <Multi-Release>true</Multi-Release>
                  </manifestEntries>
                </transformer>
```

`Multi-Release: true` marks the jar as one that may contain class files for several Java versions under `META-INF/versions/`. Some libraries ship such files; without the flag the JVM ignores them.

```xml
                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
              </transformers>
```

`ServicesResourceTransformer` merges the files in `META-INF/services/`. Java finds implementations through these files (the `ServiceLoader` mechanism; SLF4J uses it to find Logback). If two libraries had a file with the same name and only one were kept, an implementation would silently go missing.

```xml
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

A **filter** says which files of which library are left out. `<artifact>*:*</artifact>` means every library. The `<exclude>` lines drop the signature files (`*.SF`, `*.DSA`, `*.RSA`: a signature is valid only for the original jar and would make the JVM refuse a merged jar with a `SecurityException`), each library's own manifest (replaced by the one written above) and every `module-info.class` (a module descriptor for the Java module system, wrong in a merged jar). The remaining closing tags end the execution and the plugin.

## 6. Try it yourself

- `./mvnw -q -B clean package -DskipTests -Djacoco.skip=true && unzip -p target/spend.jar META-INF/MANIFEST.MF`. Expected: lines including `Main-Class: com.plainround.spend.interfaces.cli.SpendCli` and `Multi-Release: true`.
- `unzip -l target/spend.jar | grep -c logback`. Expected: a number above 100; Logback's classes are inside.
- `unzip -l target/spend.jar | grep META-INF/services`. Expected: the merged service files of the libraries.
- `ls -l target/original-spend.jar target/spend.jar`. Expected: the original is small, the shaded one is several megabytes.

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
