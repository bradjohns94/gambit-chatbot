#!/bin/bash

SERVICE_NAME=$1
BUMP_TYPE=$2

# Derive the version from the service name
if ! [ -d "$(pwd)/${SERVICE_NAME}" ]; then
  echo "Service directory not found, ${SERVICE_NAME}"
  echo "Usage: ./deploy.sh <service name> [minor|major|patch]"
  exit 1
fi

CONVERTED_NAME=$(echo "${SERVICE_NAME}" | sed -e "s/-/_/g" | awk '{print toupper($0)}')
SERVICE_VERSION_NAME="${CONVERTED_NAME}_VERSION"
SERVICE_VERSION=$(sed -n -e "s/${SERVICE_VERSION_NAME}\=*//p" .env)
SERVICE_VERSION=$(echo $SERVICE_VERSION | sed -e "s/\n//g")

if [ -z ${SERVICE_VERSION} ]; then
  echo "Failed to find variable ${SERVICE_VERSION_NAME} in .env"
  exit 1
fi

# Derive the new version from the old version
if [ ${BUMP_TYPE} != "patch" ] && \
   [ ${BUMP_TYPE} != "minor" ] && \
   [ ${BUMP_TYPE} != "major" ]; then
  echo "Invalid bump type argument: ${BUMP_TYPE}"
  echo "Usage: ./deploy.sh <service name> [minor|major|patch]"
  exit 1
fi

OLD_PATCH=$(echo ${SERVICE_VERSION} | sed -e "s/.*\..*\.\(.*\)/\1/g")
OLD_MINOR=$(echo ${SERVICE_VERSION} | sed -e "s/.*\.\(.*\)\..*/\1/g")
OLD_MAJOR=$(echo ${SERVICE_VERSION} | sed -e "s/\(.*\)\..*\..*/\1/g")

if [ ${BUMP_TYPE} == "patch" ]; then
  NEW_VERSION="${OLD_MAJOR}.${OLD_MINOR}.$((${OLD_PATCH} + 1))"
elif [ ${BUMP_TYPE} == "minor" ]; then
  NEW_VERSION="${OLD_MAJOR}.$((${OLD_MINOR} + 1)).0"
elif [ ${BUMP_TYPE} == "major" ]; then
  NEW_VERSION="$((${OLD_MAJOR} + 1)).0.0"
fi

if [ -z "${NEW_VERSION}" ]; then
  echo "Failed to determine new version"
  exit 1
fi

# Tag the docker snapshot as a new release
echo "Upgrading ${SERVICE_NAME} from version ${SERVICE_VERSION} to ${NEW_VERSION}"

docker tag "${SERVICE_NAME}:${SERVICE_VERSION}-SNAPSHOT" "${SERVICE_NAME}:${NEW_VERSION}"

if [ $? -ne 0 ]; then
  echo "Failed to tag new docker image"
  exit 1
fi

echo "Tagged docker image: ${SERVICE_NAME}:${NEW_VERSION}"

# Upgrade the max version in .env
sed -e "s/${SERVICE_VERSION_NAME}=${SERVICE_VERSION}/${SERVICE_VERSION_NAME}=${NEW_VERSION}/g" .env >> .env.tmp
mv .env.tmp .env

echo "Successfully created tag: ${SERVICE_NAME}:${NEW_VERSION}"
