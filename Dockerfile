FROM openjdk:11.0.9.1-slim AS build
WORKDIR /measurement-service/
COPY . .
RUN ./gradlew build -x :check

FROM ubuntu:20.04
RUN mkdir -p /ci/workdir /sdk
ENV PATH="/sdk/tools/bin:/sdk/platform-tools:${PATH}"
ENV DEBIAN_FRONTEND=noninteractive

RUN apt update -y --fix-missing
RUN apt install -y --fix-missing wget unzip openjdk-11-jdk
RUN cd /sdk && wget https://dl.google.com/android/repository/commandlinetools-linux-6200805_latest.zip \
            && unzip commandlinetools-linux-6200805_latest.zip

ENV ANDROID_HOME=/sdk
ENV ANDROID_NDK_HOME=/sdk/ndk/21.3.6528147

RUN sdkmanager --update --sdk_root=${ANDROID_HOME}
RUN yes | sdkmanager --sdk_root=${ANDROID_HOME} --licenses
RUN sdkmanager --sdk_root=${ANDROID_HOME} "tools" "platform-tools" "platforms;android-30" "build-tools;30.0.3" "cmake;3.10.2.4988404" "ndk;21.3.6528147"

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8

WORKDIR /app/
COPY --from=build /measurement-service/build/libs/*.jar app.jar
COPY --from=build /measurement-service/test-apps test-apps
ENTRYPOINT ["java", "-jar", "app.jar"]
