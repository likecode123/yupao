package com.yupi.usercenter.service;

import com.yupi.usercenter.utils.AlgorithmUtils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 算法工具类测试
 */
public class AlgorithmUtilsTest {


    @Test
    void test() {
        String str1 = "[\"Java\",\"大一\",\"男\",\"乒乓球\"]";
        String str2 = "[\"Java\",\"大三\",\"女\",\"乒乓球\"]";
        String str3 = "[\"Java\",\"大二\",\"男\",\"乒乓球\"]";
        String str4 = "[\"Python\",\"大四\",\"女\",\"编程\"]";
        String str5 = "[\"Python\",\"大四\",\"女\",\"编程\",\"乒乓球\"]";
        String str6 = " [\"warriors\",\"curry\"]";

        // 1
        int score1 = AlgorithmUtils.minDistance(str1, str2);
        // 3
        int score2 = AlgorithmUtils.minDistance(str1, str3);
        int score3 = AlgorithmUtils.minDistance(str1, str4);
        int score4 = AlgorithmUtils.minDistance(str1, str5);
        int score5 = AlgorithmUtils.minDistance(str1, str6);
        System.out.println(score1);
        System.out.println(score2);
        System.out.println(score3);
        System.out.println(score4);
        System.out.println(score5);
    }

    @Test
    void testCompareTags() {
        List<String> tagList1 = Arrays.asList("Java", "大一", "男");
        List<String> tagList2 = Arrays.asList("Java", "大一", "女");
        List<String> tagList3 = Arrays.asList("Python", "大二", "女");
        // 1
        int score1 = AlgorithmUtils.minDistance(tagList1, tagList2);
        // 3
        int score2 = AlgorithmUtils.minDistance(tagList1, tagList3);
        System.out.println(score1);
        System.out.println(score2);
    }

}