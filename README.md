# Super Smash Character Data Streaming Application ⚔💥💪

This is a Kafka Streams application that takes input from a topic of game data and extracts the character data from each message to be sent downstream.

## Structure

To create the `Game` and `Character` classes, the `pom.xml` file has the Avro build plugin to build these classes using the `.avsc` files in the resources directory. This is done with `mvn clean install package`.

All of the processing in this application happens in the `CharacterDataExtractionProcessor` class.

## Operation

I used the [Quick Start for Confluent Platform](https://docs.confluent.io/platform/current/platform-quickstart.html) along with Podman (in order to use the docker compose file) to get an instance of Kafka running locally so that I could create topics. I also used the [Install Confluent Platform using ZIP and TAR](https://docs.confluent.io/platform/current/installation/installing_cp/zip-tar.html) guide to install the Confluent CLI for the `console-avro-producer` commands listed below.

To produce data to the `game.data` topic I use the following cURL commands in Ubuntu bash (WSL2):

```bash
bin/kafka-avro-console-producer \
--bootstrap-server localhost:9092 \
--topic game.data  \
--property schema.registry.url=http://localhost:8081 \
--property value.schema.id=1
```

Make sure your `schema.id` matches the schema that you just produced.

Once the producer is ready and the Streams application is running, I entered the following example data:

```json
{"match_length": 162, "player_one_character": "Mario", "player_two_character": "Link", "player_one_name": "Gaius", "player_two_name": "Antonius", "winner": "Gaius","stage": "Final Destination"}
```

This produces a new Avro message to the `game.data` topic which this application picks up and extracts character data to send downstream to `character.data` topic. The results can be checked in the Confluent Control Center by looking at the messages for the `character.data` topic.

## Further Improvements

Currently, this stream only creates downstream messages for the winner of the match. In the future I would like to expand upon this producer to create messages for both characters.
