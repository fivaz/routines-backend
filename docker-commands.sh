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

    # Ensure BuildKit is enabled
    export DOCKER_BUILDKIT=1

    # Check if the secret file exists
    if [ ! -f .sentry-auth-token ]; then
        echo -e "${RED}Error: .sentry-auth-token file not found!${NC}"
        exit 1
    fi

    echo -e "${BLUE}Building Docker image for platform: ${platform}...${NC}"
    docker build --build-arg SENTRY_AUTH_TOKEN="$(< .sentry-auth-token)" --platform ${platform} -t ${tag} .

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Docker image built successfully for ${platform}!${NC}"
    else
        echo -e "${RED}Error building Docker image for ${platform}.${NC}"
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

# Function to stop, remove all related containers, and remove associated images
cleanup_docker() {
    echo -e "${BLUE}Cleaning up Docker containers...${NC}"

    # Stop and remove containers related to routine-backend
    CONTAINERS=$(docker ps -a | grep routine-backend | awk '{print $1}')
    if [ -n "$CONTAINERS" ]; then
        docker stop $CONTAINERS 2>/dev/null
        docker rm $CONTAINERS 2>/dev/null
    else
        echo "No matching containers found."
    fi

    echo -e "${BLUE}Cleaning up Docker images...${NC}"

    # Find and remove images related to routine-backend
    IMAGES=$(docker images | grep routine-backend | awk '{print $3}')
    if [ -n "$IMAGES" ]; then
        docker rmi $IMAGES 2>/dev/null
    else
        echo "No matching images found."
    fi

    echo -e "${GREEN}Cleanup complete!${NC}"
}

# Function to restart (rebuild + rerun) without removing images
restart_docker() {
    local platform=$1
    local tag=$2
    local container_name=$3

    echo -e "${BLUE}Restarting Docker container: ${container_name}...${NC}"

    # Stop & remove container if it exists
    if docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
        docker stop ${container_name} 2>/dev/null
        docker rm ${container_name} 2>/dev/null
    fi

    # Rebuild image (same name & tag)
    build_docker "${platform}" "${tag}"

    # Run container again
    docker run \
        --name ${container_name} \
        -p 8080:8080 \
        -e GCP_PROJECT_ID=${GCP_PROJECT_ID:-default-project-id} \
        ${tag}

    echo -e "${GREEN}Restart complete for ${container_name}!${NC}"
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
    "restart-dev")
        restart_docker "linux/arm64" "routine-backend:dev" "routine-backend-dev"
        ;;
    "restart-prod")
        restart_docker "linux/amd64" "routine-backend:latest" "routine-backend-prod"
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
        echo "  restart-dev - Rebuild and restart dev container (no image cleanup)"
        echo "  restart-prod - Rebuild and restart prod container (no image cleanup)"
        exit 1
        ;;
esac