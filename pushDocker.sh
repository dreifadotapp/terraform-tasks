#!/bin/bash

docker tag terraform-tasks-agent:latest ianmorgan/terraform-tasks-agent:latest
docker push ianmorgan/terraform-tasks-agent:latest
