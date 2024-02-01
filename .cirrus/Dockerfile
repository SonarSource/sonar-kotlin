ARG CIRRUS_AWS_ACCOUNT
FROM ${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-g7-latest

# Copy the Gradle init-script for Repox Authorization
ENV GRADLE_HOME /usr/local/share/gradle/
COPY ${PWD}/repoxAuth.init.gradle.kts ${GRADLE_HOME}/init.d/

USER sonarsource