FROM mozilla/sbt

ARG VERSION
ENV GAMBIT_CORE_VERSION=${VERSION}
ENV SRCDIR=/home/gambit/user-api

RUN useradd -ms /bin/bash gambit
WORKDIR $SRCDIR

ADD ./user-api ${SRCDIR}
# This is gross, but I don't want to implement real CI
RUN sbt clean && sbt scalastyle
RUN sbt clean && sbt assembly

USER gambit

CMD ["java", "-jar", "target/scala-2.12/user-api.jar"]
