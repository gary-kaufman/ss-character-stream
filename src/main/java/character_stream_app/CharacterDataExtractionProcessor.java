package character_stream_app;

import character_stream_app.model.Game;
import character_stream_app.model.SSCharacter;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class CharacterDataExtractionProcessor {

	private static final Logger log = LoggerFactory.getLogger(CharacterDataExtractionProcessor.class);

    public static void main(String[] args) throws InterruptedException {
    	log.info("This is a Kafka Stream Processor...");
    	
    	Properties props = new Properties();
    	props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "http://localhost:9092");
    	props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "character-data-stream");
    	props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    	props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    	props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
    	props.put("schema.registry.url", "http://localhost:8081");
    	props.put("use.latest.version", true);
    	
    	String inputTopic = "game.data";
    	String outputTopic = "character.data";
    	
    	// When you want to override serdes explicitly/selectively
    	final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", "http://localhost:8081");
    	final Serde<Game> valueSpecificAvroSerde = new SpecificAvroSerde<>();
    	valueSpecificAvroSerde.configure(serdeConfig, false); // `false` for record values

    	StreamsBuilder builder = new StreamsBuilder();
    	KStream<String, Game> topicInputStream = builder.stream(inputTopic, 
    			Consumed.with(Serdes.String(), 
    					valueSpecificAvroSerde));
    	    	
    	KStream<String, SSCharacter> outputStream = topicInputStream.map((key, game) ->
        new KeyValue<>((game.getWinner() + Long.toString(System.currentTimeMillis())),
        		convertGameToWinnerCharacter(game)));
    	
    	outputStream.to(outputTopic);
    	
    	Topology topology = builder.build();
    	System.out.println(topology.describe());
    	KafkaStreams streams = new KafkaStreams(topology, props);
    	
    	final CountDownLatch latch = new CountDownLatch(1);
    	// attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
        	e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
        
    }

	private static SSCharacter convertGameToWinnerCharacter(Game game) {
		log.info("Processing character data for: " + game.getWinner());
		String winnerPlayerCharacter = new String();
		
		if (game.getWinner().equals(game.getPlayerOneName())) winnerPlayerCharacter = game.getPlayerOneCharacter();
		else winnerPlayerCharacter = game.getPlayerTwoCharacter();
		
		SSCharacter ssCharacter = new SSCharacter(game.getMatchLength(), winnerPlayerCharacter, game.getWinner(), true, game.getStage());
		
		return ssCharacter;
	}

}
