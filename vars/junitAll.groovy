def call() {
    
    echo "Processingtest results"
    
    junit keepLongStdio: true, testResults: 'tests/**/*-junit.xml'
}
