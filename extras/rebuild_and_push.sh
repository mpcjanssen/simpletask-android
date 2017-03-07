#!/bin/sh

./gradlew clean build
fossil uv add app/build/outputs/apk/*
fossil sync -u
