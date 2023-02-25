## **Project archived**

`nb-javac-android` has been merged with the [`AndroidIDE`](https://github.com/AndroidIDEOfficial/AndroidIDE) project.

# nb-javac-android

<a href="https://github.com/itsaky/AndroidIDE"><img src="https://androidide.com/github/img/androidide.php?part&for-the-badge"/></a><br>

[nb-javac](https://github.com/oracle/nb-javac) is a patched version of OpenJDK "javac", i.e., the Java compiler. This has long been part of NetBeans, providing a highly tuned Java compiler specifically for the Java editor i.e., parsing and lexing for features such as syntax coloring, code completion.

`nb-javac-android` is a patched version of `nb-javac`. It allows developers to use `javac` i.e. the Java Compiler in android applications.

**This version of `nb-javac` is meant to be used with [`AndroidIDE`](https://github.com/itsaky/AndroidIDE). If you want to use it in your project, you might need to do some modifications. See [`Issue #1`](https://github.com/itsaky/nb-javac-android/issues/1) for more information.**

# Building nb-javac jar files

## Prerequisite
  - Git
  - Ant 1.9.9 or above
  - JDK 8 (to build nb-javac). Other versions are not recommended.
  - Apache Maven

## Build the project
1. Obtain the code with the following command

```
$ git clone https://github.com/itsaky/nb-javac-android.git
```

2. To get a specific version use the following command

```bash
$ git checkout <release_tag_name> 
```

3. Run the below command to build nb-javac.

```bash
$ ant -f ./make/langtools/netbeans/nb-javac clean jar
```

A JAR named `nb-javac-<ver>-android.jar` will be generatead at location `./make/langtools/netbeans/nb-javac/dist/`

4. Run below command to zip the source code of nb-javac

```bash
$ ant -f ./make/langtools/netbeans/nb-javac zip-nb-javac-sources
```

# Publishing to maven central / OSSRH

1. Aquire an account for OSSRH from sonatype and get access to the target groupId
   See here: https://central.sonatype.org/pages/ossrh-guide.html

2. Configure the maven installation so that the credentials are made available
   for the server with the id oss.sonatype.org

3. Run
   ```
   ant -f ./make/langtools/netbeans/nb-javac publish-to-ossrh-snapshots -Dmaven.groupId=your.grp.id
   ```
   to publish snapshot artifacts (https://oss.sonatype.org/content/repositories/snapshots/)

4. Run
   ```
   ant -f ./make/langtools/netbeans/nb-javac publish-to-maven-central -Dmaven.groupId=your.grp.id
   ```
   to stage the release, which will get promoted to maven central, after it has
   been manually released.

# Documentation 

- https://cwiki.apache.org/confluence/display/NETBEANS/Overview%3A+nb-javac
- https://cwiki.apache.org/confluence/display/NETBEANS/Release+Schedule
- https://confluence.oraclecorp.com/confluence/display/NB/nb-javac+JDK14+uptake
- https://wiki.se.oracle.com/display/JPG/Behavior+without+NB-Javac

# nb-javac help
- Subscribe or mail the users@netbeans.apache.org list - Ask questions, find answers, and also help other users.
- Subscribe or mail the dev@netbeans.apache.org list - Join development discussions, propose new ideas and connect with contributors.

# Contributing
See the  [Contributing Policy](./CONTRIBUTING.md)

# Security
See the  [Security Policy](./SECURITY.md)
