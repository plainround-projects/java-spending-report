# XML

## 1. What it is

XML is a text format for structured data. A file is a tree of **elements**: `<name>content</name>`, which may hold text, attributes (`<module name="Checker">`) or more elements. `<name/>` is an empty element. `<!-- ... -->` is a comment. Many Java tools use XML for configuration.

## 2. Why this repository uses it

Three files in this repository are XML, each read by a different tool: Maven reads `pom.xml`, Checkstyle reads `config/checkstyle.xml`, Logback reads `logback.xml`.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | the Maven build |
| `config/checkstyle.xml` | the Checkstyle rules |
| `src/main/resources/logback.xml`, `src/test/resources/logback-test.xml` | the logging configuration |

## 4. How it works underneath

- Every opening tag needs a closing tag (or be self-closing); elements nest properly; there is exactly one root element.
- Attributes are `name="value"` inside the opening tag; text content is between tags.
- Special characters in text must be escaped: `&lt;` for `<`, `&amp;` for `&`. In `logback.xml` the pattern avoids them.
- The first line `<?xml version="1.0" encoding="UTF-8"?>` declares the version and character encoding.
- A **namespace** (`xmlns="..."`) names the vocabulary. The pom's `xmlns` and `xsi:schemaLocation` point at the schema that defines which elements are allowed; Maven checks the file against it.

## 5. Walkthrough of this repository's use

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
```

The first line is the XML declaration. `<project>` is the root; its `xmlns*` attributes name the schema.

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
<!-- Rules that a formatter cannot enforce: naming, Javadoc on the public surface, dead code and
     leftovers. Layout is owned by Spotless (google-java-format). -->
<module name="Checker">
```

`<!DOCTYPE ...>` is an older way to name the rules a file must follow (a *DTD*); Checkstyle uses it. Here `<module name="...">` elements nest: `Checker` holds `TreeWalker`, which holds the checks, and `<property name="..." value="..."/>` configures one.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- Structured key=value (logfmt) lines on standard error; standard output carries only the report. -->
<configuration>
```

In Logback, `<configuration>` is the root, `<appender>` and `<root>` are its children.

## 6. Try it yourself

- `xmllint --noout pom.xml` (installed on macOS). Expected: no output if the XML is well-formed.
- `xmllint --xpath '//*[local-name()="artifactId"]/text()' pom.xml`. Expected: the artifact ids found in the file.
- `grep -c '<module' config/checkstyle.xml`. Expected: the number of rule modules.

## 7. Mistakes people make

- A missing closing tag or a stray `&`: the whole file is rejected ("not well-formed").
- Editing a pom by hand and breaking the order of plugin elements; run `./mvnw validate` after changes.
- Using tabs/spaces inconsistently: harmless to XML, but the `.editorconfig` keeps two spaces.

## 8. Check yourself

1. What is an element?
   **Answer:** A named piece of the tree written `<name>...</name>` or `<name/>`.
2. What does `xmlns` do?
   **Answer:** Names the vocabulary (namespace) the elements belong to.
3. Why must `&` be escaped?
   **Answer:** In XML it starts an entity such as `&amp;`.
4. Which tool reads `logback.xml`?
   **Answer:** Logback, at start-up.
5. What is the root of a pom?
   **Answer:** `<project>`.

## 9. Further reading

- XML 1.0 (Fifth Edition): https://www.w3.org/TR/xml/
- POM reference: https://maven.apache.org/pom.html
