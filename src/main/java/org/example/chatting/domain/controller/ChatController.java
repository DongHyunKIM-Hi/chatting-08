package org.example.chatting.domain.controller;

import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.example.chatting.common.config.redis.ChatRedisPublisher;
import org.example.chatting.common.config.redis.RedisChatMessage;
import org.example.chatting.common.entity.ChatMessage;
import org.example.chatting.common.entity.ChatRoom;
import org.example.chatting.common.entity.User;
import org.example.chatting.common.interceptor.AuthenticatedUser;
import org.example.chatting.domain.model.ChatMessageDto;
import org.example.chatting.domain.model.TypingIndicatorDto;
import org.example.chatting.domain.repository.ChatMessageRepository;
import org.example.chatting.domain.repository.ChatRoomRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRedisPublisher chatRedisPublisher;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void send(ChatMessageDto dto, Principal principal) {

        User sender = AuthenticatedUser.fromPrincipal(principal);

        ChatRoom room = chatRoomRepository
            .findById(dto.getRoomId())
            .orElseThrow();

        ChatMessage message = new ChatMessage(sender, room, dto.getContent());
        chatMessageRepository.save(message);

        RedisChatMessage redisMessage = new RedisChatMessage(
            message.getChatRoom().getId(),
            message.getSender().getId(),
            message.getSender().getName(),
            message.getContent()
        );

        chatRedisPublisher.publish(room.getId(), redisMessage);

    }

    @MessageMapping("/chat.typing")
    public void typing(TypingIndicatorDto dto, Principal principal) {
        User user = AuthenticatedUser.fromPrincipal(principal);

        dto.setUserId(user.getId());
        dto.setUserName(user.getName());

        // 타이핑 상태를 해당 채팅방의 다른 사용자들에게 브로드캐스트
        messagingTemplate.convertAndSend("/sub/chat/" + dto.getRoomId() + "/typing", dto);
    }
}