Gitrank-web
===========
[![Give Feedback](https://gitrank.io/assets/images/giveFeedbackBadge.svg)](https://gitrank.io/github/gitlinks/gitrank-web)
[![Stories in Ready](https://badge.waffle.io/gitlinks/gitrank-web.svg?label=ready&title=Ready)](http://waffle.io/gitlinks/gitrank-web)
[![Build Status](https://semaphoreci.com/api/v1/projects/496f40bb-a35b-437b-b3dd-818334b9aebf/566862/shields_badge.svg)](https://semaphoreci.com/gitlinks/gitrank-web)
[![Codacy Badge](https://www.codacy.com/project/badge/e3f15c6b2d194f5a989708663ff718dc)](https://www.codacy.com/app/nikel092_2742/gitrank-web)

Web front-end application for the gitrank project.
Project extensive description can be found [here](https://github.com/gitlinks/github-rank-project)

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
