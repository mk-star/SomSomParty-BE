package com.acc.somsomparty.domain.Queue.dto;

import lombok.Builder;

@Builder
public record QueueMessage(String userId, long currentTime) {
}
