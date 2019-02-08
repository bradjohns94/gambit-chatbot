# Add Administrator

A tool script which, from the CLI of the server hosting the chatbot, can create
an admin user given a set of arguments. This is useful to allow the initial
admin to be created without opening API calls for arbitrary users to establish
themselves as an administrator, but isn't the recommended way to create new
users/promote existing users after that.

## Environment

In order to use this script, you'll need the following environment variables
configured:

- *PG_URL* - A base URL to connect to the postgres database, (e.g. localhost)
- *PG_DB* - The database in postgres that the gambit user data is stored
- *PG_USER* - A username to connect to postgres with
- *PG_PASSWORD* - The associated password for the given username

## Building

This script can be built via SBT, docker, or docker compose using the following:

*SBT*
```
$ cd ./add-admin  # From the root add-admin directory
$ sbt assembly
```

*Docker*
```
$ docker build -t <image name>:<image tag> .
```

*Docker-Compose*
```
$ cd ../../  # Back to the gambit-chatbot root directory
$ docker-compose build add_admin
```


## Running

If you're running this script from Docker or SBT, make sure first that the
postgres database is up and running and has been migrated to the latest version.

From there, run the following commands (leaving off from the build step):

*SBT*
```
$ java -jar target/scala-2.12/add-admin.jar --username <nickname> \
  --client <client> \
  --client-id <client-specific identifier>
```

*Docker*
```
$ docker run --rm -it <image name>:<image tag> --username <nickname> \
  --client <client> \
  --client-id <client-specific identifier>
```

*Docker-Compose*
```
$ docker-compose run add_admin --username <nickname> \
  --client <client>
  --client-id <client-specific identifier>
```

Where the arguments are the following:

- *--username* - The desired gambit nickname of the admin user (any string)
- *--client* - A client identifier associated with a client configured with the bot
- *--client-id* - The unique ID associated with the client


## Testing

This project is a simple tool that currently has no unit tests. Shame on me.
