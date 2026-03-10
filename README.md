# Bitops: A Concolic-Based Test Input Generation Method for Units with Bit Operators of Java Projects

A concolic testing tool that automatically generates test inputs for Java methods, with dedicated support for bitwise and shift operators (`&`, `|`, `^`, `~`, `<<`, `>>`, `>>>`).

## Abstract

Existing concolic testing tools often lack proper support for bitwise and shift operations, leading to incomplete path exploration and inaccurate constraint solving. This tool addresses that gap by encoding bitwise semantics directly into Z3 BitVector constraints, enabling correct and complete test input generation for Java units that contain bit operators.

---

## Prerequisites

- **Java 17** – [Download here](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html). Make sure `JAVA_HOME` points to JDK 17. ([Setup guide](https://www.youtube.com/watch?v=YONvtseO574))
- **Maven 3.x** – bundled with most IDEs (IntelliJ IDEA recommended) or [install manually](https://maven.apache.org/install.html)
- **IntelliJ IDEA** (or any Maven-compatible Java IDE)

---

## Installation & Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/LeHoaiNam756/Bitops.git
cd Bitops
```

### Step 2: Configure the Project Root Path

Open `src/main/java/core/utils/FilePath.java` and update the `JCIA_PROJECT_ROOT_PATH` constant to the absolute path where you cloned the project on your machine:

```java
public static final String JCIA_PROJECT_ROOT_PATH = "C:\\path\\to\\Bitops";
```

### Step 3: Add the Z3 Library

The Z3 version available on Maven Central is outdated, so the required JAR (`com.microsoft.z3.jar` v4.14.0) is bundled locally under `src/main/java/core/lib/`.

Install it into your local Maven repository by running:

```bash
mvn install:install-file \
  -Dfile="<project-path>/src/main/java/core/lib/com.microsoft.z3.jar" \
  -DgroupId="com.microsoft" \
  -DartifactId="z3" \
  -Dversion="4.14.0" \
  -Dpackaging=jar
```

> **If the command fails**, locate your `.m2` folder (typically `C:\Users\<YourName>\.m2\repository`) and manually create the directory `com\microsoft\z3\4.14.0`, then copy the JAR file into it. Reload the Maven project afterwards.

### Step 4: Build the Project

```bash
mvn clean install -DskipTests
```

---

## Running the Tool

Open `src/main/java/Main.java` in your IDE and run the `main` method. The JavaFX GUI will launch.

**Using the GUI:**

1. Load your Java project by selecting the source file or project folder via the file browser.
2. Select the class and method you want to test from the project tree.
3. Choose a coverage criterion: **Statement**, **Branch**, or **MC/DC**.
4. Click **Run** to start concolic test generation.
5. View the generated test inputs, coverage results, and execution output in the report table.

---

## Running Tests

```bash
mvn test
```

Test suites are located under `src/test/java/` and cover CFG construction, symbolic execution, and test generation modules.
