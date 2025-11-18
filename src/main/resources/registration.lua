local activityId = ARGV[1]
local phone = ARGV[2]

local activityKey = "registration:activity:" .. activityId
local registrationKey = "registration:registrator:" .. activityId

local remain = redis.call('get', activityKey)
-- 判断键是否存在
if(remain == false) then
    return 1
end
-- 判断是否还有名额
if(tonumber(remain) <= 0) then
    return 2
end

-- 判断用户是否已报名
if(redis.call('sismember', registrationKey, phone) == 1) then
    return 3
end

-- 扣名额
redis.call('incrby', activityKey, -1)
-- 报名
redis.call('sadd', registrationKey, phone)

return 0