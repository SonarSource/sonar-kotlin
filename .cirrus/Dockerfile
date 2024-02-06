ARG CIRRUS_AWS_ACCOUNT
FROM ${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-latest

ARG GITHUB_TOKEN
ARG GRADLE_USER_HOME

USER root

RUN mkdir "$GRADLE_USER_HOME/init.d"

# Download https://github.com/SonarSource/re-ci-images/blob/master/docker/gradle/repoxAuth.init.gradle.kts
# and copy/move into GRADLE_USER_HOME
RUN curl  -H 'Authorization: token $GITHUB_TOKEN' \
          -H 'Accept: application/vnd.github.v3.raw' \
          -L 'https://github.com/SonarSource/re-ci-images/blob/master/docker/gradle/repoxAuth.init.gradle.kts' \
          -o '$GRADLE_USER_HOME/init.d/repoxAuth.init.gradle.kts'

USER sonarsource
