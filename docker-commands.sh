#!/bin/bash

# docker-commands.sh

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Docker Hub details
DOCKER_HUB_USERNAME="fivaz"
REPOSITORY_NAME="routine-backend"

# Function to build the Docker image for a specific platform
build_docker() {
    local platform=$1
    local tag=$2

    echo -e "${BLUE}Building Docker image for platform: ${platform}...${NC}"
    docker build --platform ${platform} -t ${tag} .

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Docker image built successfully for ${platform}!${NC}"
    else
        echo "Error building Docker image for ${platform}"
        exit 1
    fi
}

# Function to build for development (ARM)
build_dev() {
    build_docker "linux/arm64" "routine-backend:dev"
}

# Function to build for production (AMD64)
build_prod() {
    build_docker "linux/amd64" "routine-backend:latest"
}

# Function to push the production image to Docker Hub
push_prod() {
    echo -e "${BLUE}Tagging production image for Docker Hub...${NC}"
    docker tag routine-backend:latest ${DOCKER_HUB_USERNAME}/${REPOSITORY_NAME}:latest

    echo -e "${BLUE}Pushing production image to Docker Hub...${NC}"
    docker push ${DOCKER_HUB_USERNAME}/${REPOSITORY_NAME}:latest

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Production image pushed successfully as ${DOCKER_HUB_USERNAME}/${REPOSITORY_NAME}:latest!${NC}"
    else
        echo "Error pushing Docker image"
        exit 1
    fi
}

# Function to run the Docker container
run_docker() {
    local tag=$1

    echo -e "${BLUE}Running Docker container...${NC}"
    docker run -p 8080:8080 \
        -e GCP_PROJECT_ID=${GCP_PROJECT_ID:-default-project-id} \
        ${tag}
}

# Function to stop and remove all related containers
cleanup_docker() {
    echo -e "${BLUE}Cleaning up Docker containers...${NC}"
    docker stop $(docker ps -a | grep routine-backend | awk '{print $1}') 2>/dev/null
    docker rm $(docker ps -a | grep routine-backend | awk '{print $1}') 2>/dev/null
    echo -e "${GREEN}Cleanup complete!${NC}"
}

# Parse command line arguments
case "$1" in
    "build-dev")
        build_dev
        ;;
    "build-prod")
        build_prod
        ;;
    "push-prod")
        build_prod
        push_prod
        ;;
    "run-dev")
        run_docker "routine-backend:dev"
        ;;
    "run-prod")
        run_docker "routine-backend:latest"
        ;;
    "cleanup")
        cleanup_docker
        ;;
    "all-dev")
        cleanup_docker
        build_dev
        run_docker "routine-backend:dev"
        ;;
    "all-prod")
        cleanup_docker
        build_prod
        run_docker "routine-backend:latest"
        ;;
    *)
        echo "Usage: $0 {build-dev|build-prod|push-prod|run-dev|run-prod|cleanup|all-dev|all-prod}"
        echo "  build-dev   - Build the Docker image for development (ARM)"
        echo "  build-prod  - Build the Docker image for production (AMD64)"
        echo "  push-prod   - Build and push the production image to Docker Hub (as latest)"
        echo "  run-dev     - Run the Docker container for development (ARM)"
        echo "  run-prod    - Run the Docker container for production (AMD64)"
        echo "  cleanup     - Stop and remove existing containers"
        echo "  all-dev     - Cleanup, build for development, and run"
        echo "  all-prod    - Cleanup, build for production, and run"
        exit 1
        ;;
esac