FROM amazoncorretto:17 as build
WORKDIR /build
COPY ./ ./
RUN ./gradlew clean shadowJar --no-daemon

FROM amazoncorretto:17
WORKDIR /pencil

RUN yum -y install shadow-utils \
    && groupadd --system pencil \
    && useradd --system pencil --gid pencil \
    && chown -R pencil:pencil /pencil
USER pencil:pencil

VOLUME /data

ENV PENCIL_CONFIG_FILE="/data/config.yaml"

COPY --from=build /build/build/libs/pencil-all.jar pencil.jar
CMD ["java", "-jar", "/pencil/pencil.jar"]
