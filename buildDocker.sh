#!/bin/bash

./gradlew clean jar -x test
docker build -t terraform-tasks-agent .
