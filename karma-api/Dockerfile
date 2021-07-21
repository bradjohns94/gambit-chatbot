FROM openjdk:18

ARG VERSION
ENV GAMBIT_CORE_VERSION=${VERSION}
ENV SRCDIR=/home/gambit/karma-api

RUN useradd -ms /bin/bash gambit
WORKDIR $SRCDIR

ADD ./karma-api/target/scala-2.12/karma-api.jar ${SRCDIR}/application.jar

USER gambit

CMD ["java", "-jar", "application.jar"]
