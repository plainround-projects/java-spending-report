# XML

## 1. What it is

**XML** (eXtensible Markup Language) is a way of writing structured data as text. Data is wrapped in named **tags**, and tags nest like boxes in boxes, so a program can read the structure as a tree. It looks like HTML but you choose the tag names, and the rules are stricter. Several tools in this repository (Maven, Checkstyle, Logback) use XML for their configuration.

## 2. Why this repository uses it

The tools choose the format, not the repository: Maven only reads `pom.xml`; Checkstyle and Logback read XML configuration files. Knowing the few rules of XML makes all four files readable.

## 3. Where it appears

| File | What it is | Lines |
| --- | --- | --- |
| `pom.xml` | the Maven build description | 296 |
| `config/checkstyle.xml` | the Checkstyle rules | 104 |
| `src/main/resources/logback.xml` | the logging configuration | 14 |
| `src/test/resources/logback-test.xml` | the quieter logging configuration for tests | 13 |

## 4. How it works underneath

1. A tool reads the text and builds a **tree**: one root element, children inside it, text and attributes on the nodes. This is called parsing.
2. A file is **well-formed** if every tag is closed in the right order, there is exactly one root, attribute values are quoted and special characters are escaped. A tool refuses a file that is not.
3. Tools additionally check that the *meaning* is right (for the pom: that only known tags appear in known places). That is the job of a schema or of the tool itself.
4. Whitespace between tags is irrelevant; inside text it is kept (Maven trims it).

## 5. Walkthrough of this repository's use

Every XML feature that appears in this repository, with a real example:

### The declaration

```xml
<?xml version="1.0" encoding="UTF-8"?>
```

The first line says the file is XML, version 1.0, stored as UTF-8. It must be the very first thing in the file. Checkstyle's file has the shorter `<?xml version="1.0"?>`.

### Elements and nesting

```xml
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>${slf4j.version}</version>
    </dependency>
```

An **element** is written `<name>content</name>`. Content can be text or more elements; here `<dependency>` contains `<groupId>`, `<artifactId>` and `<version>`, each containing text. Every opening tag must be closed, in the reverse order of opening. The whole file is one element, the root.

### Attributes

```xml
  <appender name="STDERR" class="ch.qos.logback.core.ConsoleAppender">
```

An **attribute** is a `name="value"` pair inside the opening tag. Values are always in quotes. Here `name` and `class` configure the appender.

### Empty elements

```xml
                <requireUpperBoundDeps/>
```

An element with no content is written as one self-closing tag. `<a/>` means exactly the same as `<a></a>`.

### Comments

```xml
        <!-- Lifecycle plugins pinned so a Maven upgrade cannot change the build silently. -->
```

Text between `<!--` and `-->` is ignored by programs. A comment may not contain two hyphens in a row.

### Namespaces

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
```

`xmlns="..."` declares the namespace of the element and everything inside it; `xmlns:xsi="..."` declares a prefix, so `xsi:schemaLocation` is an attribute from that vocabulary. The addresses are names; nothing is downloaded when a tool reads the file.

### Document type declaration

```xml
<!DOCTYPE module PUBLIC "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
```

A DTD (document type definition) names the grammar the file follows. Only Checkstyle's file has one; the others rely on their own tool's built-in knowledge.

### Special characters

```xml
&lt;  &gt;  &amp;  &quot;  &apos;
```

Inside text, `<` and `&` cannot be written as they are, because they start tags and entities. They are written `&lt;` and `&amp;`. (`&gt;`, `&quot;` and `&apos;` are optional.) The XML files here need none of them; the same escaping exists in HTML, where `&amp;` appears in links (see `html.md`).

Every one of the four files is explained line by line in its own technology note: `pom.xml` in `maven.md`, `config/checkstyle.xml` in `checkstyle.md`, and the logback files in `logback.md`.

## 6. Try it yourself

- `xmllint --noout pom.xml config/checkstyle.xml src/main/resources/logback.xml` (xmllint ships with macOS). Expected: no output, which means the files are well-formed.
- In a scratch copy delete one closing tag and rerun. Expected: an error naming the line, such as `Opening and ending tag mismatch`.
- `./mvnw -q -B help:effective-pom -Doutput=target/effective-pom.xml && head -12 target/effective-pom.xml`. Expected: a file that begins with the XML declaration and a comment box "Effective POM for project ...": the pom as Maven sees it, with every default filled in.

## 7. Mistakes people make

- Forgetting to close a tag or closing in the wrong order.
- A bare `&` or `<` in text.
- Unquoted attribute values.
- Two root elements.
- Hand-editing a generated XML file.

## 8. Check yourself

1. What is an element?
   **Answer:** A named piece of data written as an opening tag, content and a closing tag.
2. How is an attribute written?
   **Answer:** As `name="value"` inside the opening tag.
3. What is `<a/>`?
   **Answer:** An empty element; the same as `<a></a>`.
4. What does well-formed mean?
   **Answer:** The text follows XML's syntax rules, so any XML parser can read it.
5. How do you write a `<` inside text?
   **Answer:** `&lt;`.

## 9. Further reading

- XML 1.0 specification: https://www.w3.org/TR/xml/
- Maven POM reference: https://maven.apache.org/pom.html
