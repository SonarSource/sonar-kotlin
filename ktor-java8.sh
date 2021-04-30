mkdir .jdk
curl -L "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u292-b10/OpenJDK8U-jdk_x64_linux_hotspot_8u292b10.tar.gz" -o .jdk/jdk.tar.gz
tar -xzf .jdk/jdk.tar.gz -C .jdk --strip-components 1
.jdk/bin/java -version
export JAVA_HOME=$PWD/.jdk
cd its/sources/kotlin/ktor
./gradlew build -x test
cd ../../../../
