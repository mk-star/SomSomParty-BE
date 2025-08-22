-- KEYS[1] = 대기열 key (ZSet)
-- KEYS[2] = activeSlot key (string)
-- KEYS[3] = maxSlot key (string)

local active = tonumber(redis.call('GET', KEYS[2]) or '0')
local max = tonumber(redis.call('GET', KEYS[3]) or '0')

if active < max then
    local popped = redis.call('ZPOPMIN', KEYS[1], 1)
    if #popped > 0 then
        redis.call('INCR', KEYS[2])
        return popped[1] -- pop한 사용자 ID 반환
    end
end
return nil
