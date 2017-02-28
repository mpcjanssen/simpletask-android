#!/bin/sh

./gradlew clean build
fossil uv add build/outputs/apk/*
fossil sync -u
