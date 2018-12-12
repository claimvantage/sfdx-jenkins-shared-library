#!/usr/bin/env groovy

def call() {
    junit keepLongStdio: true, testResults: 'tests/**/*-junit.xml'
}
