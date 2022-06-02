# Terraform Tasks

[![Circle CI](https://circleci.com/gh/dreifadotapp/terraform-tasks.svg?style=shield)](https://circleci.com/gh/dreifadotapp/terraform-tasks)
[![Licence Status](https://img.shields.io/github/license/dreifadotapp/terraform-tasks)](https://github.com/dreifadotapp/terraform-tasks/blob/master/licence.txt)

A set of [tasks](https://github.com/dreifadotapp/tasks#readme) dedicated to running
the [Terraform CLI](https://www.terraform.io/)

## Quick Start

See the example in [LifeCycleTest](impl/src/test/kotlin/dreifa/app/terraform/tasks/LifeCycleTest.kt)

_TODO - write some better docs and example_

## Useful links

* [download](https://www.terraform.io/downloads) the Terraform CLI.
* [custom CircleCI images](https://circleci.com/docs/2.0/custom-images).

## Running from the command line

```bash
./gradlew :agent:run 
```

The agent is started on the default port of 11601. To test is running

```bash
curl http://localhost:11601/ping
```


## Running under Docker

To build and push an image

```bash
./buildDocker.sh 
./pushImage.sh
```

To run locally 

```bash
docker run -p11601:11601 terraform-tasks-agent -d
```

and test with 
```bash
curl http://localhost:11601/ping
```


## Building with CircleCI

This project needs a custom image with Terraform installed. This is in the `.circleci/images/primary` folder. There is a
public image published using the details below.

```bash
docker build -t ianmorgan/cci-terraform-tasks-primary:0.0.2 .circleci/images/primary
docker login
docker push ianmorgan/cci-terraform-tasks-primary:0.0.2
```

## Adding as a dependency

Maven jars are deployed using JitPack. See releases for version details.

```groovy
//add jitpack repo
maven { url "https://jitpack.io" }

// add dependency
implementation "com.github.dreifadotapp:terraform-tasks:<release>"
```

JitPack build status is at https://jitpack.io/com/github/dreifadotapp/terraform-tasks/$releaseTag/build.log
