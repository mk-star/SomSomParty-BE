local stock = redis.call('GET', KEYS[1])
if tonumber(stock) <= 0 then
    return 0
else
    redis.call('DECR', KEYS[1])
    return 1
end