Gitrank-web
===========

[![Circle CI](https://img.shields.io/circleci/project/gitlinks/gitrank-web.svg)](https://circleci.com/gh/gitlinks/gitrank-web)
[![Codacy Badge](https://www.codacy.com/project/badge/e3f15c6b2d194f5a989708663ff718dc)](https://www.codacy.com/app/nikel092_2742/gitrank-web)

Web front-end application for the gitrank project.

## Requirements

* Scala 2.11
* Neo4j 2.2.2
* sbt

## Install

Set environment variables:

* `NEO4J_USER` (default is neo4j)
* `NEO4J_PASSWORD` (default is neo4j)
* `GITHUB_CLIENT_ID`
* `GITHUB_CLIENT_SECRET`

## Run

Run with an instance of neo4j running

`$ activator run`
