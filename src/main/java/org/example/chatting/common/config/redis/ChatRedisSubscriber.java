package org.example.chatting.common.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {

        RedisChatMessage redisMessage = objectMapper.readValue(
                message.getBody(),
                RedisChatMessage.class
            );

        log.info("메시지 받았음");
        log.info(redisMessage.getContent());

        messagingTemplate.convertAndSend(
            "/sub/chat/" + redisMessage.getRoomId(),
            redisMessage);
    }
}