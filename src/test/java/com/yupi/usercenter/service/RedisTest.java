package com.yupi.usercenter.service;
import java.util.Date;

import com.yupi.usercenter.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;


    @Test
    void testRedis() {
        // 测试Redis连接？
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("kaiString","789");
//        valueOperations.set("kaiInt",1);
//        valueOperations.set("kaiDouble",2.0);
//        User user = new User();
//        user.setId(1L);
//        user.setUsername("niuBiTest");
//        valueOperations.set("kaiUser",user);

        //查
        Object rokie= valueOperations.get("kaiString");
        Assertions.assertTrue("789".equals((String)rokie));
//        rokie = valueOperations.get("kaiInt");
//        Assertions.assertTrue(1 == (Integer) rokie);
//        rokie = valueOperations.get("kaiDouble");
//        Assertions.assertTrue(2.0 == (Double) rokie);
//        System.out.println(valueOperations.get("kaiUser"));
//
        //改
//        valueOperations.set("kaiString","456");
//        Object rokie= valueOperations.get("kaiString");
//        Assertions.assertTrue("456".equals((String)rokie));

//        redisTemplate.delete("kaiString");
//        Object rokie= valueOperations.get("kaiString");
//        Assertions.assertTrue("456".equals((String)rokie));



    }
}
