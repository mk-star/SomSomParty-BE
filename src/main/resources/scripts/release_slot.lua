local active = redis.call('get', KEYS[1]) or '0'
if tonumber(active) > 0 then
    redis.call('decr', KEYS[1])
    return 1
else
    return 0
end