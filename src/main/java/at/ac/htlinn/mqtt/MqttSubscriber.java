package at.ac.htlinn.mqtt;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.charset.StandardCharsets;

public class MqttSubscriber {

    public static void main(String[] args) throws InterruptedException {
        Dotenv env = Dotenv.load();

        Mqtt3BlockingClient client = Mqtt3Client.builder()
                .serverHost(env.get("MQTT_HOST"))
                .serverPort(Integer.parseInt(env.get("MQTT_PORT" )))
                .sslWithDefaultConfig()
                .simpleAuth()
                    .username(env.get("MQTT_USERNAME"))
                    .password(env.get("MQTT_PASSWORD").getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth()
                .identifier(env.get("MQTT_CLIENT_ID_SUB", "htlinn-subscriber"))
                .buildBlocking();

        client.connect();
        System.out.println("Connected to " + env.get("MQTT_HOST"));

        String topic = env.get("MQTT_TOPIC");
        client.subscribeWith()
                .topicFilter(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .send();
        System.out.println("Subscribed to [" + topic + "] — waiting for messages...");

        try (Mqtt3BlockingClient.Mqtt3Publishes publishes = client.publishes(MqttGlobalPublishFilter.ALL)) {
            while (true) {
                Mqtt3Publish msg = publishes.receive();
                String payload = StandardCharsets.UTF_8.decode(msg.getPayload().orElseThrow()).toString();
                System.out.println("Received [" + msg.getTopic() + "]: " + payload);
            }
        }
    }
}
