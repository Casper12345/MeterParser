# Meter Parser

A library for parsing AEMO NEM12 standard meter reading files.
See: https://aemo.com.au/-/media/files/electricity/nem/retail_and_metering/metering-procedures/2017/mdff_specification_nem12_nem13_final_v102.pdf


## Discussion
This project is built with Scala 3 and leverages fs2 streams combined with Cats Effect to enable efficient concurrency. 
The design prioritizes minimal usage of OS threads by employing Cats Effect fibers for the core reader logic 
and JDK virtual threads for database access, ensuring high performance and scalability.

## Parallel Processing Configuration
The application supports parallel processing, controlled by the ```PARALLELISM``` environment variable.

Each file is processed in parallel until the specified parallelism threshold is reached. 
Each file is assigned its own stream, which runs sequentially. 
Once a stream completes, the next stream is picked up by a fiber from the parallelism pool. 
Note: the "pool" here refers to Cats Effect lightweight fibers, not OS threads.

#### Key Considerations for ```PARALLELISM```

- Memory Usage: Each stream maintains an internal buffer of size CHUNK_SIZE (configurable).
The JVM memory will hold up to ```CHUNK_SIZE * PARALLELISM``` record objects at a time.


- CPU Bound Tasks: Streams primarily perform CPU-bound tasks when aggregating results.
They are IO-bound only when inserting ```CHUNK_SIZE``` records.
Therefore, set PARALLELISM to a value close to the number of logical CPU cores for optimal performance. 
Avoid setting it significantly higher than the core count to prevent resource contention.

#### Considerations for ```CHUNK_SIZE```

- ```CHUNK_SIZE``` controls size of batches that are inserted by JDBC. Check the batch limit size for your database.

## Limitations

- The run scripts have been made for posix based system and is not targeted for windows. 
  However, the jar file can be run on windows as well using the `java -jar` command.

- The jdk only supports jdk 21+ due to its usage of virtual threads.

- The parser currently only supports the NEM12 format and only parses data relevant for the persisted data model.


## Installation 

Make sure to have a jdk version 21 or above installed:
    
    https://sdkman.io

And Docker installed:

    https://www.docker.com

And run the project from the root folder.

### Running the project without installing scala:
The project contains uber jars with all dependencies including scala3. 
The run script defines set of environment variable that match
the docker compose setup. Destination files are added to the ```./csv``` folder - the path can 
be set with the ```FILE_LOCATION``` variable. 

To run the project:

    $ Make up
    $ ./bin/run.sh

### Running the project with sbt:
The project can also be run with sbt. Install sbt:
    
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
