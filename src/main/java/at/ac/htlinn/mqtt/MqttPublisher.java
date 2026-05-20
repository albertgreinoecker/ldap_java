package at.ac.htlinn.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.charset.StandardCharsets;

public class MqttPublisher {

    public static void main(String[] args) {
        Dotenv env = Dotenv.load();

        Mqtt3BlockingClient client = Mqtt3Client.builder()
                .serverHost(env.get("MQTT_HOST"))
                .serverPort(Integer.parseInt(env.get("MQTT_PORT")))
                .sslWithDefaultConfig()
                .simpleAuth()
                    .username(env.get("MQTT_USERNAME"))
                    .password(env.get("MQTT_PASSWORD").getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth()
                .identifier(env.get("MQTT_CLIENT_ID_PUB", "htlinn-publisher"))
                .buildBlocking();

        client.connect();
        System.out.println("Connected to " + env.get("MQTT_HOST"));

        String topic = env.get("MQTT_TOPIC");
        for (int i = 1; i <= 5; i++) {
            String message = "Message #" + i;
            client.publishWith()
                    .topic(topic)
                    .payload(message.getBytes(StandardCharsets.UTF_8))
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();
            System.out.println("Published [" + topic + "]: " + message);
        }

        client.disconnect();
        System.out.println("Disconnected.");
    }
}
