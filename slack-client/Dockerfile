FROM mozilla/sbt

ARG VERSION
ENV SLACK_CLIENT_VERSION=${VERSION}
ENV SRCDIR=/home/gambit/slack-client

RUN useradd -ms /bin/bash gambit
WORKDIR $SRCDIR

ADD ./slack-client/target/scala-2.12/slack-client.jar ${SRCDIR}/application.jar

USER gambit

CMD ["java", "-jar", "application.jar"]
