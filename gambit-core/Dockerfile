FROM mozilla/sbt

ARG VERSION
ENV GAMBIT_CORE_VERSION=${VERSION}
ENV SRCDIR=/home/gambit/gambit-core

RUN useradd -ms /bin/bash gambit
WORKDIR $SRCDIR

ADD ./gambit-core/target/scala-2.12/gambit-core.jar ${SRCDIR}/application.jar

USER gambit

CMD ["java", "-jar", "application.jar"]
