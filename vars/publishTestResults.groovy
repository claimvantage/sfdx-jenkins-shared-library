def call() {
    
    echo "Publish test results"
    
    junit keepLongStdio: true, testResults: 'tests/**/*-junit.xml'
}
