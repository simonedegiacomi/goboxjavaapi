# GoBoxAPI
This folder contains the code of the API for manage the files of your GoBox Storage.

## Import
### Maven
To import this library you can use [Jitpack](https://jitpack.io/).
Just add Jitpack as repository
```xml
<repositories>
    <repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
    </repository>
</repositories>
```
And then add the dependence:
```xml
<dependency>
    <groupId>com.github.simonedegiacomi</groupId>
    <artifactId>goboxjavaapi</artifactId>
    <version>master-00a5235817-1</version>
</dependency>
```
### Gradle
If you need to import this library with Gradle you can add the Jitpack repository with
```
repositories {
    allprojects {
        repositories {
            maven { url "https://jitpack.io" }
        }
    }
}
```
And the library with:
```
compile 'com.github.simonedegiacomi:goboxjavaapi:master-SNAPSHOT'
```