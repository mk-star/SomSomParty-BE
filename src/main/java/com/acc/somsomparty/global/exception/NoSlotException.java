package com.acc.somsomparty.global.exception;

import com.acc.somsomparty.global.exception.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoSlotException extends RuntimeException {
  ErrorCode errorCode;

  @Override
  public String getMessage() {
    return errorCode.getMessage();  // ErrorCode의 메시지를 반환
  }
}