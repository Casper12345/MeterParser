# Meter Parser

A library for parsing NEM12 meter reading files.

## Discussion
This project is build using Scala3 Fs2 streams combined with Cats effect for concurrency.


#### Concurrency


## Limitations

If I had more time I would have written more tests. I could also have expanded the parser to
support other NEM formats plus adding better error handling. Full error reporting which reports which
line is malformed for all records could also have been supported. In this current version the parser 
only reports the first error it encounters for each record. 

## Installation 

Make sure to have a jdk version 21 or above installed:
    
    https://sdkman.io

And Docker installed:

    https://www.docker.com

And run the project from the root folder.

#### Running it without installing scala:
The project contains uber jars with all dependencies including scala3. In case the 
user doesn't want to install scala and sbt.

To run the project:

    $ Make up
    $ ./bin/run.sh

#### Running it with sbt
The project can also be run with sbt:
Install sbt:
    
    https://sdkman.io

Then run sbt:
    
    $ Make docker-up
    $ sbt flyway/run
    $ ./bin/env.sh
    $ sbt meterParser/run

## Tests
In order to run the tests sbt has to be installed:

    $ Make docker-up
    $ sbt flyway/run
    $ sbt meterParser/test


