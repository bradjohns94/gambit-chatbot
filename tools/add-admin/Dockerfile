FROM mozilla/sbt

ARG VERSION
ENV ADD_ADMIN_VERSION=${VERSION}
ENV SRCDIR=/home/gambit/add-admin

RUN useradd -ms /bin/bash gambit
WORKDIR $SRCDIR

ADD ./add-admin ${SRCDIR}
# This is gross, but I don't want to implement real CI
RUN sbt clean && sbt scalastyle
RUN sbt clean && sbt assembly

USER gambit

ENTRYPOINT ["java", "-jar", "target/scala-2.12/add-admin.jar"]
