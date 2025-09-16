package com.acc.somsomparty.domain.Queue.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class QueueStatus {
    private final long availableSlots;
    private final long queueSize;
}