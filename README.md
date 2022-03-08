# Terraform Tasks

[![Circle CI](https://circleci.com/gh/dreifadotapp/terraform-tasks.svg?style=shield)](https://circleci.com/gh/dreifadotapp/terraform-tasks)
[![Licence Status](https://img.shields.io/github/license/dreifadotapp/terraform-tasks)](https://github.com/dreifadotapp/terraform-tasks/blob/master/licence.txt)

A set of [tasks](https://github.com/dreifadotapp/tasks#readme) dedicated to running
the [Terraform CLI](https://www.terraform.io/)

## Useful links

* [download](https://www.terraform.io/downloads) the Terraform CLI.
* [custom CircleCI images](https://circleci.com/docs/2.0/custom-images).

## Building with CircleCI

This project needs a custom image with Terraform installed. This is in the `.circleci/images/primary` folder. There is a
public image

```bash
docker build -t ianmorgan/cci-terraform-tasks-primary:0.0.1 .circleci/images/primary
docker login
docker push ianmorgan/cci-terraform-tasks-primary:0.0.1

```