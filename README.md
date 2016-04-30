# tooring
Distributed Turing machine

##Requirenments
* Java 8
* Maven 3

##Running
First you have to build the application: `mvn clean package`

The output is `tooring-0.0.1-SNAPSHOT.jar` file located at the `target` folder.

This archive can be executed in two modes: submitter or worker.

###Worker
To start app as a worker, use command: `java -jar tooring-0.0.1-SNAPSHOT.jar --worker ID` (where ID is the identifier of the worker).

###Submitter
To submit new Turing machine to the cluster, place it's JSON-formatted description to the directory with the JAR-file (example file is "bubbleSort").

Then execute command `java -jar tooring-0.0.1-SNAPSHOT.jar --set --input FILENAME` (where filename is your input-file name, e.g. "bubbleSort").

In a while you gonna get the output with your key: ID representing submitted input stored in the system. Use it further for scheduling the execution.

With the key obtained, you can schedule the Turing machine to be computed. It can be done using command `java -jar tooring-0.0.1-SNAPSHOT.jar --id ID --schedule KEY` (where ID is your ID, and KEY is the key of the submitted Turing machine).

Then you can try to get a result using command `java -jar tooring-0.0.1-SNAPSHOT.jar --get KEY --output FILENAME` (where KEY is the task key and FILENAME is the name of the file to store the result to).

##Architecture and ideology
The developed application uses Hazelcast Framework under the hood (http://hazelcast.org/).

One of the core features of the Hazelcast is the possibility to create dynamically-scaled and fault-tolerant computing cluster.
The cluster can be deployed in three possible modes: to the local network (using broadcasting), to the Amazon Cloud (using AWS Application Key) and to the arbitrary network (using static IP-addressing). `tooring` application testing was performed using first option (over the LAN).

`tooring` application brings two key features to the user:
* __Parallelism__. If someone has N complex tasks to compute and each task takes M hours to be computed on the local machine, one doesn't need to wait MxN hours to perform computations.
The user provides the input data and the computation algorithm for each task in the form of classic Turing machine and submits it to the cluster.
Description-file should be JSON-formatted and should contain Turing machine *tape* (with input data), *state space* and the *transition space* (see the `bubbleSort` file example).
Finally, if the cluster has enough quantity of workers, user gets his results in ~M hours instead of NxM.
* __Fault-tolerance__. If someone has complex computational task which takes M hours to be computed locally, there might be a problem about such local computation: OS or PC can freeze or even crash and the results of such long computation may become lost.
`tooring` application guarantees that submitted and scheduled Turing machine will be computed eventually. User can submit and schedule task and forget about it for a while. Sooner or later he will be able to obtain the computation result.

Also `tooring` application has scoring system: the more tasks you compute as a worker, the higher priority of your own computations you'll have in the system.

##Testing
Application has unit- and integration-tests. One of them is "bubbleSort" test which can be used for performance measurements (com.uwc.tooring.turing.TuringMachineTests.testBubbleSort). It contains Bubble Sort algorithm description and it fills the Turing machine tape with sample data.
By changing the `MULTIPLIER` constant you can scale up or down the size of the array to sort. Then you can serialize the prepared Turing machine and save it to text file for further usage.
Sample generated file is located at the root directory of the project.