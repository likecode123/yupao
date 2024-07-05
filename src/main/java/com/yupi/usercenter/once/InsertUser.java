package com.yupi.usercenter.once;

import com.yupi.usercenter.mapper.UserMapper;
import com.yupi.usercenter.model.domain.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;


import javax.annotation.Resource;
@Component
public class InsertUser{

    @Resource
    private UserMapper userMapper;

    /**
     * 循环插入用户
     */
//    @Scheduled(initialDelay = 5000,fixedRate = Long.MAX_VALUE )
    public void doInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 3000;
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("fourthBatch");
            user.setUserAccount("第五批40000条");
            user.setAvatarUrl("https://s2.loli.net/2024/06/29/EVlUmaI7YAKbpBf.jpg");
            user.setProfile("五批");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("15166");
            user.setEmail("lakers@163.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("92123");
            user.setTags(" [\"lakers\",\"james\",\"男\",\"黄色\"]");
            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println( stopWatch.getLastTaskTimeMillis());

    }
}



