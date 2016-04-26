#!/bin/bash

if [[ -n ${TRAVIS_TAG} ]]; then
    echo "on a tag -> set pom.xml <version> to $TRAVIS_TAG"
    mvn --settings maven_deploy_settings.xml org.codehaus.mojo:versions-maven-plugin:2.2:set -DnewVersion=${TRAVIS_TAG} 1>/dev/null 2>/dev/null
fi

#if [[ ${TRAVIS_BRANCH} != 'master' ]]; then
#    echo "Skipping deployment for branch \"${TRAVIS_BRANCH}\""
#    exit
#fi

if [[ "$TRAVIS_PULL_REQUEST" = "true" ]]; then
    echo "Skipping deployment for pull request"
    exit
fi

mvn -B deploy --settings maven_deploy_settings.xml -Dmaven.test.skip=true -Dfindbugs.skip=true