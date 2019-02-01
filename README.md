# Gambit

A functional, multi-platform, microservice based chatbot for Scala developers

## Overview

Gambit is a customizable, fully-asynchronous chatbot for those who want to
make a chatbot out of an obscure library using functional practices. Gambit
is currently composed of the following services:

- *Gambit Core* - The core API service of the bot
- *Slack Client* - An interface between the core API and slack

But will hopefully have more services on the way!

## Environment

Before you deploy Gambit, you first have to set up and environment file for
docker compose to use. All variables needed for the environment should be
specified in the env-template file given. Simply populate it with your own
personal config and you're ready to build and deploy.

## Building

The Dockerfile specified in each service should be ready to build the latest
version of the code for your development environment. For more responsible
production deployments you'll need to configure the environment with the
proper release version (which I don't publish, sorry) before proceeding to the
deploy step of this README

To run a development build simply run:

```
$ docker-compose build
```

There's no special way to build a production build because the deployment
process is primitive enough to convert dev builds into prod builds
.
## Deploying

To deploy in development, everything is already setup for you! Simply run:

```
$ docker-compose up
```

And all services will automatically spin up based on the config specified

To release a development build as a new production release, simply tag the
existing snapshot as the next version using and update your .env to indicate
that a new version exists using the tag script:

```
$ ./tag.sh <service-name> [patch|minor|major]
```

Where `[patch|minor|major]` refers to what part of the version number you'd
like to bump (e.g. patch 1.2.3 -> 1.2.4, major 1.2.3 -> 2.0.0)

Then, when you want to release your change (or rollback to another version)
use the deploy script:

```
$ ./deploy.sh <service-name> <version>
```

## Making a New Service

In order to make a new service, you'll need to:

1. Create a Docker build-able subdirectory under the root of this repo
2. Populate your .env file with a version number with the same name as your
 service in all caps with dashes changed to underscores and ending with VERSION
 (e.g. "gambit-core" -> "GAMBIT_CORE_VERSION") and any environment variables
 your service may need
3. Update docker-compose.yaml and docker-compose-prod.yaml with a new service
definition of your service


## Testing

Full application integration tests aren't available as of yet. Unit tests are
available, so please see the README for each individual service as for how
to run it's unit tests
