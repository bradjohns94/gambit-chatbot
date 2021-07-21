FROM mozilla/sbt

ARG VERSION
ENV GAMBIT_CORE_VERSION=${VERSION}
ENV SRCDIR=/home/gambit/user-api

RUN useradd -ms /bin/bash gambit
WORKDIR $SRCDIR

ADD ./user-api/target/scala-2.12/user-api.jar ${SRCDIR}/application.jar

USER gambit

CMD ["java", "-jar", "application.jar"]
