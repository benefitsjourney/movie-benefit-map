package com.collection.benefitsjourney.config;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.domain.ConversationBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.jwt.JsonWebTokenUtility;
import com.collection.benefitsjourney.repository.ConversationRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JsonWebTokenUtility jsonWebTokenUtility;
    private final UsersRepository usersRepository;
    private final ConversationRepository conversationRepository;

    private static final Pattern CONVERSATION_TOPIC =
            Pattern.compile("^/topic/conversations/(\\d+)$");

    public WebSocketAuthInterceptor(
            JsonWebTokenUtility jsonWebTokenUtility,
            UsersRepository usersRepository,
            ConversationRepository conversationRepository) {

        this.jsonWebTokenUtility = jsonWebTokenUtility;
        this.usersRepository = usersRepository;
        this.conversationRepository = conversationRepository;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // 建立 STOMP 連線時驗證 JWT
        if (command == StompCommand.CONNECT) {

            String authorization =
                    accessor.getFirstNativeHeader("Authorization");

            if (authorization == null
                    || !authorization.startsWith("Bearer ")) {
                throw new AccessDeniedException(
                        "WebSocket 連線需要登入 Token");
            }

            String token = authorization.substring(7);
            String subject =
                    jsonWebTokenUtility.validateToken(token);

            if (subject == null || subject.isBlank()) {
                throw new AccessDeniedException(
                        "WebSocket Token 無效或已過期");
            }

            Integer usersId;

            try {
                usersId = Integer.valueOf(subject);
            } catch (NumberFormatException e) {
                throw new AccessDeniedException(
                        "Token 會員 ID 格式錯誤");
            }

            UsersBean user = usersRepository.findById(usersId)
                    .orElseThrow(() -> new AccessDeniedException(
                            "會員不存在"));

            // WebSocket 只允許目前狀態為 ACTIVE 的會員
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new AccessDeniedException(
                        "此帳號目前無法使用 WebSocket");
            }

            String role = user.getRole();

            if (!"USER".equals(role) && !"ADMIN".equals(role)) {
                throw new AccessDeniedException(
                        "會員角色無效");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            subject,
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + role)));

            // 將已驗證的會員身分綁定至 STOMP 連線
            accessor.setUser(authentication);

            return message;
        }

        // 訂閱聊天室時確認會員權限
        if (command == StompCommand.SUBSCRIBE) {

            Principal principal = accessor.getUser();

            if (principal == null) {
                throw new AccessDeniedException(
                        "尚未驗證 WebSocket 身分");
            }

            String destination = accessor.getDestination();

            Matcher matcher = destination == null
                    ? CONVERSATION_TOPIC.matcher("")
                    : CONVERSATION_TOPIC.matcher(destination);

            // 只允許訂閱指定格式的聊天室主題
            if (!matcher.matches()) {
                throw new AccessDeniedException(
                        "不允許訂閱此目的地");
            }

            Integer conversationId;
            Integer usersId;

            try {
                conversationId = Integer.valueOf(matcher.group(1));
                usersId = Integer.valueOf(principal.getName());
            } catch (NumberFormatException e) {
                throw new AccessDeniedException(
                        "聊天室或會員 ID 格式錯誤");
            }

            ConversationBean conversation =
                    conversationRepository.findById(conversationId)
                            .orElseThrow(() -> new AccessDeniedException(
                                    "聊天室不存在"));

            Integer userOneId =
                    conversation.getUserOne().getUsersId();

            Integer userTwoId =
                    conversation.getUserTwo().getUsersId();

            if (!userOneId.equals(usersId)
                    && !userTwoId.equals(usersId)) {
                throw new AccessDeniedException(
                        "你沒有權限訂閱此聊天室");
            }

            return message;
        }

        // 目前仍由既有 REST API 儲存訊息，
        // 不接受前端直接透過 STOMP 傳送訊息
        if (command == StompCommand.SEND) {
            throw new AccessDeniedException(
                    "請使用既有聊天 API 傳送訊息");
        }

        return message;
    }
}