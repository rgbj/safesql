#!/bin/bash

mvn -B cobertura:cobertura coveralls:report

if [[ -n ${TRAVIS_TAG} ]]; then
    echo "Skipping deployment for tag \"${TRAVIS_TAG}\""
    exit
fi

if [[ ${TRAVIS_BRANCH} != 'master' ]]; then
    echo "Skipping deployment for branch \"${TRAVIS_BRANCH}\""
    exit
fi

if [[ "$TRAVIS_PULL_REQUEST" = "true" ]]; then
    echo "Skipping deployment for pull request"
    exit
fi

mvn -B deploy --settings maven_deploy_settings.xml -Dmaven.test.skip=true -Dfindbugs.skip=true