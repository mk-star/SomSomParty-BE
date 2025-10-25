
-- KEYS[1] = 축제 ID
-- ARGV[1] = TTL (초 단위)
-- ARGV[2] = 허용 요청 수

local current = tonumber(redis.call('GET', KEYS[1]) or "0")

if tonumber(current) + 1 > tonumber(ARGV[2]) then
    return 0
end

current = redis.call('INCR', KEYS[1])

-- TTL은 첫 요청일 때만 설정
if current == 1 then
    redis.call('EXPIRE', KEYS[1], ttl)
end

-- 현재 카운트 반환
return current;
