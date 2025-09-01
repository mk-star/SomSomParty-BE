
-- KEYS[1] = 대기열 ZSet
-- KEYS[2] = activeSlot (string)
-- KEYS[3] = maxSlot (string)

local active = tonumber(redis.call('GET', KEYS[2]) or '0')
local max = tonumber(redis.call('GET', KEYS[3]) or '0')

-- 대기열 비었는지 확인
if redis.call('ZCARD', KEYS[1]) == 0 then
    return "EMPTY"
end

-- 슬롯이 있으면 pop + 점유
if active < max then
    local popped = redis.call('ZPOPMIN', KEYS[1], 1)
    if #popped > 0 then
        -- 슬롯 점유
        redis.call('INCR', KEYS[2])
        -- 여기서 rank 전송 대신 바로 슬롯 해제
        redis.call('DECR', KEYS[2])
        return popped[1]  -- 사용자 ID 반환
    end
end

return "NOSLOT"
