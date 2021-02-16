# CLI app converting a Dynalist list of English words into Anki cards with definitions from the Oxford dictionary

### Compile, run tests and build an executable JAR:
```
sbt coverage test coverageReport coverageOff scalastyle test:scalastyle assembly
```

### Run
```
sbt -Dconfig.file=path/to/config-file "run [max-num-words-to-convert]"
```
or
```
java -Dconfig.file=path/to/config-file -jar <projectDir>target/scala-2.13/dynalist-to-anki-converter-assembly-0.1.jar [max-num-words-to-convert]
```
Default ```max-num-words-to-convert``` is 1.

### Required additional configuration
```
include "application"

words {
    list-name = <name-of-the-dynalist-list-with-words>
    api-key = <dynalist-api-key>
}

dictionary {
    app-id = <oxford-dictionary-app-id>
    app-key = <oxford-dictionary-app-key>
}
```
