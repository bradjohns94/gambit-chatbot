FROM mozilla/sbt

ARG VERSION
ENV SLACK_CLIENT_VERSION=${VERSION}
ENV SRCDIR=/home/gambit/slack-client

RUN useradd -ms /bin/bash gambit
WORKDIR $SRCDIR

ADD ./slack-client $SRCDIR
# This is gross, but I don't want to implement real CI
RUN sbt clean && sbt scalastyle
RUN sbt clean && sbt assembly

USER gambit

CMD ["java", "-jar", "target/scala-2.12/slack-client.jar"]
