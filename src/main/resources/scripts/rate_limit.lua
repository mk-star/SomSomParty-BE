
-- KEYS[1] = 축제 ID
-- ARGV[1] = TTL (초 단위)
-- ARGV[2] = 허용 요청 수

-- 키 값을 1 증가시킴
local current = redis.call('INCR', KEYS[1])

-- 첫 요청이면 TTL 설정
if tonumber(current) == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end

-- 제한 초과 시 0 반환
if tonumber(current) > tonumber(ARGV[2]) then
    return 0
end

-- 현재 카운트 반환
return current
