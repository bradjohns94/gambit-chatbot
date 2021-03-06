#!/bin/bash

SERVICE_NAME=$1
DEPLOY_VERSION=$2
CONVERTED_NAME=$(echo "${SERVICE_NAME}" | sed -e "s/-/_/g" | awk '{print toupper($0)}')
DEPLOY_SERVICE_NAME="${CONVERTED_NAME}_DEPLOY_VERSION"

DOCKER_TAG="${SERVICE_NAME}:${DEPLOY_VERSION}"

if [ -z "$(docker images -q ${DOCKER_TAG} 2> /dev/null)" ]; then
  echo "Tag not found: ${DOCKER_TAG}"
  exit 1
fi

echo "Deploying ${SERVICE_NAME}:${DEPLOY_VERSION}"
# Update .env
sed -e "s/${DEPLOY_SERVICE_NAME}=.*/${DEPLOY_SERVICE_NAME}=${DEPLOY_VERSION}/" .env >> .env.tmp
mv .env.tmp .env

echo "Tearing down old services..."
docker-compose down
echo "Starting production service..."
# TODO separate out tools and migration one-offs from services
docker-compose --file docker-compose-prod.yaml up -d slack_client_prod
echo "Successfully deployed ${SERVICE_NAME}:${DEPLOY_VERSION}"
