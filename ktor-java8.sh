mkdir .jdk
curl -L "http://download.oracle.com/otn-pub/java/jdk/8u161-b12/2f38c3b165be4555a1fa6e98c45e0808/jdk-8u161-linux-x64.tar.gz" -o .jdk/jdk.tar.gz
tar -xzf .jdk/jdk.tar.gz -C .jdk --strip-components 1
.jdk/bin/java -version
export JAVA_HOME=$PWD/.jdk
cd its/sources/kotlin/ktor
./gradlew build -x test
cd ../../../../
