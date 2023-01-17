package com.redis.rest.controller;

import com.redis.rest.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author: yaodong zhang
 * @create 2023/1/16
 */
@RestController
@RequestMapping("/script")
public class ScriptController {

    @Autowired
    private RedisTemplate redisTemplate;


    @DeleteMapping("/{key}")
    public ApiResponse<Boolean> delKey(@PathVariable(value = "key") @NotNull String key) {

        String UUID = java.util.UUID.randomUUID().toString();
        boolean success = redisTemplate.opsForValue().setIfAbsent(key, UUID, 3, TimeUnit.MINUTES);
        if (!success) {
            System.err.println("锁已存在");
        }
        // 执行 lua 脚本
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
        // 指定 lua 脚本
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/del_key.lua")));
        // 指定返回类型
        redisScript.setResultType(Boolean.class);
        System.err.println(redisScript.getScriptAsString());
        // 参数一：redisScript，参数二：key列表，参数三：arg（可多个）
        Boolean result = (Boolean) redisTemplate.execute(redisScript, Collections.singletonList(key), UUID);
        System.err.println(result);

        return ApiResponse.success(result);
    }

    @GetMapping("/limit")
    public ApiResponse<Long> limit() {
        String key = "123";
        // 执行 lua 脚本
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        // 指定 lua 脚本
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/counter_limit.lua")));
        System.err.println(redisScript.getScriptAsString());
        // 指定返回类型
        redisScript.setResultType(Long.class);
        // 参数一：redisScript，参数二：key列表，参数三：arg（可多个）
        Long result = (Long) redisTemplate.execute(redisScript, Collections.singletonList(key), "10", "60");
        System.err.println(result);
        if (result == -1) {
            System.err.println("访问过于频繁，请稍候再试");
        }
        return ApiResponse.success(result);
    }

}
