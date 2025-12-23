package com.aiqutepets.config;

import com.aiqutepets.service.DeviceMqttService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

/**
 * MQTT 配置类
 * 基于 Spring Integration MQTT 实现 MQTT 客户端连接和消息处理
 */
@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.default.topic}")
    private String defaultTopic;

    @Autowired(required = false)
    private DeviceMqttService deviceMqttService;

    /**
     * MQTT 客户端工厂
     * 配置连接参数：URL、用户名、密码、KeepAlive 等
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        // 设置 Broker URL
        options.setServerURIs(new String[] { brokerUrl });

        // 设置用户名和密码
        options.setUserName(username);
        options.setPassword(password.toCharArray());

        // 设置 KeepAlive 间隔（60秒）
        options.setKeepAliveInterval(60);

        // 设置连接超时时间（30秒）
        options.setConnectionTimeout(30);

        // 设置自动重连
        options.setAutomaticReconnect(true);

        // 设置清除会话（每次连接时不保留上次的会话信息）
        options.setCleanSession(true);

        factory.setConnectionOptions(options);

        log.info("MQTT 客户端工厂配置完成: brokerUrl={}, username={}", brokerUrl, username);

        return factory;
    }

    // ==================== 入站配置（接收消息）====================

    /**
     * 入站消息通道
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT 入站适配器
     * 监听指定 Topic 的消息
     */
    @Bean
    public MessageProducer inbound() {
        // 使用不同的 clientId 避免与出站冲突
        String inboundClientId = clientId + "_inbound";

        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(inboundClientId,
                mqttClientFactory(), defaultTopic);

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());

        log.info("MQTT 入站适配器配置完成: clientId={}, topic={}", inboundClientId, defaultTopic);

        return adapter;
    }

    /**
     * MQTT 消息处理器
     * 处理从 MQTT Broker 接收到的消息
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return new MessageHandler() {
            @Override
            public void handleMessage(Message<?> message) throws MessagingException {
                String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
                String payload = message.getPayload().toString();

                log.info("收到 MQTT 消息: topic={}, payload={}", topic, payload);

                // 解析 Topic，提取设备 UID
                // Topic 格式示例: ctl/{deviceUid}
                if (topic != null && topic.startsWith("ctl/")) {
                    String deviceUid = topic.substring(4);

                    // 调用 DeviceMqttService 处理消息
                    if (deviceMqttService != null) {
                        deviceMqttService.handleDeviceStatusMessage(deviceUid, payload);
                    }
                }
            }
        };
    }

    // ==================== 出站配置（发送消息）====================

    /**
     * 出站消息通道
     */
    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT 出站处理器
     * 用于向 MQTT Broker 发送消息
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutbound() {
        // 使用不同的 clientId 避免与入站冲突
        String outboundClientId = clientId + "_outbound";

        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(outboundClientId, mqttClientFactory());

        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(defaultTopic);
        messageHandler.setDefaultQos(1);

        log.info("MQTT 出站处理器配置完成: clientId={}", outboundClientId);

        return messageHandler;
    }
}
