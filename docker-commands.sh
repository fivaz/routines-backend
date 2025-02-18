#!/bin/bash

# docker-commands.sh

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to build the Docker image
build_docker() {
    echo -e "${BLUE}Building Docker image...${NC}"
    docker build -t myapp .

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Docker image built successfully!${NC}"
    else
        echo "Error building Docker image"
        exit 1
    fi
}

# Function to run the Docker container
run_docker() {
    echo -e "${BLUE}Running Docker container...${NC}"
    docker run -p 8080:8080 \
        -e GCP_PROJECT_ID=${GCP_PROJECT_ID:-default-project-id} \
        myapp
}

# Function to stop and remove all related containers
cleanup_docker() {
    echo -e "${BLUE}Cleaning up Docker containers...${NC}"
    docker stop $(docker ps -a | grep myapp | awk '{print $1}') 2>/dev/null
    docker rm $(docker ps -a | grep myapp | awk '{print $1}') 2>/dev/null
    echo -e "${GREEN}Cleanup complete!${NC}"
}

# Parse command line arguments
case "$1" in
    "build")
        build_docker
        ;;
    "run")
        run_docker
        ;;
    "cleanup")
        cleanup_docker
        ;;
    "all")
        cleanup_docker
        build_docker
        run_docker
        ;;
    *)
        echo "Usage: $0 {build|run|cleanup|all}"
        echo "  build   - Build the Docker image"
        echo "  run     - Run the Docker container"
        echo "  cleanup - Stop and remove existing containers"
        echo "  all     - Cleanup, build, and run"
        exit 1
        ;;
esac