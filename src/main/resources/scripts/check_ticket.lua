local stock = redis.call('GET', KEYS[1])
if tonumber(stock) -1 < 0 then
    return 0
else
    return 1
end