Contributing
============

#Branching Model
For now this repository uses two branches:

* **master:** This is the production branch
* **stage:** This is the test branch. Merge in it triggers a docker build to be deployed on the test server.
* **develop:** This is the developing branch.

All pull-requests should be made on develop, once we consider a version stable, we shall merge develop into stage to test
on the test server. Finally when we are satisfied with the quality of the tests on the test server we will
merge stage into master to deploy in production.

The stage branch is used to deploy a develop version of the software on test servers.

#Committing

Please refer to the issue number in your commit. Commit messages should always start by the reference of the issue it solves and 
use imperative present tense for commit messages.

ex: `#105 - Add a quote to fix the issue on lambda computing` 