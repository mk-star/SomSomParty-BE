package com.acc.somsomparty.global.exception.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    // Festival
    FESTIVAL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 축제입니다."),
    //Chatting
    FAILED_MESSAGE_SAVE(HttpStatus.BAD_REQUEST,"메세지 저장에 실패했습니다."),
    FAILED_MESSAGE_GET(HttpStatus.BAD_REQUEST, "메세지 조회에 실패했습니다."),
    CHATROOM_NOT_FOUND(HttpStatus.BAD_REQUEST,"존재하지 않는 채팅방입니다."),
    ALREADY_JOINED_CHATROOM(HttpStatus.CONFLICT,"이미 참여 중인 채팅방입니다."),
    USER_CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND,"참여 중인 채팅방이 아닙니다."),
    // Queue
    QUEUE_ALREADY_REGISTERED_USER(HttpStatus.CONFLICT, "이미 큐에 등록된 유저입니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.BAD_REQUEST, "락을 획득할 수 없습니다."),
    NO_SLOT(HttpStatus.BAD_REQUEST, "슬롯이 부족합니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
    USER_NOT_CONFIRMED(HttpStatus.FORBIDDEN, "이메일 인증이 필요합니다. 이메일을 확인해주세요."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 Refresh Token입니다."),
    TOKEN_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "토큰 검증에 실패했습니다."),
    // Ticket
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 티켓입니다."),
    TICKET_SOLD_OUT(HttpStatus.BAD_REQUEST, "남아있는 티켓이 없습니다.");

    private final HttpStatus httpStatus;    // HttpStatus
    private final String message;       // 설명
}
