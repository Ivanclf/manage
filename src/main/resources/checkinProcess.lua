local function checkinProcess(keys, args)
    -- 赋值
    local userKey = keys[1]
    local locationKey = keys[2]
    local activityId = args[1]
    local duration = tonumber(args[2])
    local latitude = tonumber(args[3])
    local longitude = tonumber(args[4])
    
    -- 验证参数
    if not activityId or not duration or not latitude or not longitude then
        return 0
    end
    
    -- 逐个读取电话列表的值
    local phoneList = {}
    for i = 5, #args do
        table.insert(phoneList, args[i])
    end
    
    -- 清理可能已存在的旧数据
    redis.call("del", userKey)
    
    -- 将电话加入到集合中
    if #phoneList > 0 then
        redis.call("sadd", userKey, unpack(phoneList))
        redis.call("expire", userKey, duration)
    end
    
    -- 将地理位置加入
    redis.call('geoadd', locationKey, longitude, latitude, activityId)
    redis.call("expire", locationKey, duration)
    
    -- 返回应签到人数
    return #phoneList
end

return checkinProcess(KEYS, ARGV)
