# GraalVM + Micronaut + Profile Guided Optimization

A simple application built on

- [Micronaut](https://micronaut.io/)
- [Flyway DB](https://flywaydb.org/)
- [H2](https://www.h2database.com/html/main.html)

The application has one endpoint which when invoked it fetches the quotes from
the database and return a random quote.

```json
{
  "author": "Eleanor Roosevelt",
  "quote": "If life were predictable it would cease to be life, and be without flavor."
}
```

The application works well with JIT and native-image without Profile Guided
Optimization (PGO)).  When I run the instrumented version of the native-image
this fails with a `21637 segmentation fault`.  I am using

- GraalVM EE 22.3 and Java 17
- Micronaut 3.7.4
- MacOS M1-chip/AArch64

## Prerequisites

- GraalVM EE 22.3 and Java 17, or newer, is required to run this example

  ```shell
  $ java --version
  java 17.0.5 2022-10-18 LTS
  Java(TM) SE Runtime Environment GraalVM EE 22.3.0 (build 17.0.5+9-LTS-jvmci-22.3-b07)
  Java HotSpot(TM) 64-Bit Server VM GraalVM EE 22.3.0 (build 17.0.5+9-LTS-jvmci-22.3-b07, mixed mode, sharing)
  ```

  Install [SDKMAN](https://sdkman.io/)

  ```shell
  $ curl -s 'https://get.sdkman.io' | bash
  ```

  Install GraalVM CE Java 17 using SDKMAN

  ```shell
  $ sdk list java
  $ sdk install java 22.3.r17-grl
  ```

  Install GraalVM EE Java 17 using SDKMAN.  The GraalVM EE is not available for
  download from SDKMAN and needs to be downloaded manually and added as an
  [SDKMAN local version](https://sdkman.io/usage#localversion).

  ```shell
  $ bash <(curl -sL https://get.graalvm.org/ee-token)
  $ bash <(curl -sL https://get.graalvm.org/jdk)
  $ sdk install java graalee-22.3-17 ./graalvm-ee-java17-22.3.0/Contents/Home
  ```

- `native-image` 22.3, or newer, is required to run this example

  ```shell
  $ gu list
  ComponentId              Version             Component name                Stability                     Origin
  ---------------------------------------------------------------------------------------------------------------------------------
  graalvm                  22.3.0              GraalVM Core                  Experimental
  native-image             22.3.0              Native Image                  Experimental                  gds.oracle.com
  ```

  or

  ```shell
  $ native-image --version
  GraalVM 22.3.0 Java 17 EE (Java Version 17.0.5+9-LTS-jvmci-22.3-b07)
  ```

  Install `native-image` using the
  [GraalVM Updater](https://www.graalvm.org/22.3/reference-manual/graalvm-updater/)

  ```shell
  $ gu install native-image
  ```

## Reproducing the error

- (_Optional_) Clean the caches

  ```shell
  $ rm -rf .gradle
  $ rm -rf build
  ```

- (_Optional_) Run the tests

  ```shell
  $ ./gradlew check
  ```

- (_Optional_) Run the application using Gradle

  ```shell
  $ ./gradlew run
  ```

  Make a request to the application

  ```shell
  $ curl http://localhost:8080/quote/random | jq
  ```

  This will return a random quote

- Create the JAR file

  ```shell
  $ ./gradlew assemble
  ```

  List the generated JAR files

  ```shell
  $ tree ./build/libs
  ./build/libs
  ├── experiment-graalvm-micronaut-pgo-1.0.0-all.jar
  ├── experiment-graalvm-micronaut-pgo-1.0.0-runner.jar
  └── experiment-graalvm-micronaut-pgo-1.0.0.jar
  ```

- (_Optional_) Run the application using Java JIT

  ```shell
  $ java -jar ./build/libs/experiment-graalvm-micronaut-pgo-1.0.0-all.jar
  ```

  Make a request to the application

  ```shell
  $ curl http://localhost:8080/quote/random | jq
  ```

  This will return a random quote

- (_Optional_) Create the native image without PGO

  ```shell
  $ ./gradlew nativeCompile
  ```

- (Optional) Run the application using the native image

  ```shell
  $ ./build/native/nativeCompile/experiment-graalvm-micronaut-pgo
  ```

  Make a request to the application

  ```shell
  $ curl http://localhost:8080/quote/random | jq
  ```

  This will return a random quote

- Create the native image **with PGO**

  1. Create the instrumented native image (`--pgo-instrument`)

     ```shell
     $ native-image \
       --class-path ./build/libs/experiment-graalvm-micronaut-pgo-1.0.0-all.jar \
       -H:+BuildOutputColorful \
       -H:Path=./build/native/nativeCompile \
       -H:Name=experiment-graalvm-micronaut-pgoi \
       -H:ConfigurationFileDirectories=./build/native/generated/generateResourcesConfigFile \
       -H:Class=demo.fst.Application \
       -J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED \
       -J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.configure=ALL-UNNAMED \
       -J--add-exports=org.graalvm.sdk/org.graalvm.nativeimage.impl=ALL-UNNAMED \
       --no-fallback \
       --pgo-instrument
     ```

  2. Run the instrumented native image and collect the profiling data
     (`./build/native/nativeCompile/experiment-graalvm-micronaut-pgo.iprof`)

     ```shell
     $ ./build/native/nativeCompile/experiment-graalvm-micronaut-pgoi \
       -XX:ProfilesDumpFile=./build/native/nativeCompile/experiment-graalvm-micronaut-pgo.iprof
     [1]    21637 segmentation fault  ./build/native/nativeCompile/experiment-graalvm-micronaut-pgoi
     ```
