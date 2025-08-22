local active = redis.call('get', KEYS[1]) or '0'
local max = redis.call('get', KEYS[2]) or '0'

if tonumber(active) < tonumber(max) then
    redis.call('incr', KEYS[1])
    return 1
else
    return 0
end