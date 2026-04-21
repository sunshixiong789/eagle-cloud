package com.eagle.common.constant;

/**
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2026/4/2-22:34
 */
public class CacheConstants {
    /** 用户名 → 用户对象缓存（UserRepository.findByUsername） */
    public static final String CACHE_USER_BY_NAME = "USER_NAME";
    /** 字典类型缓存（DictApplicationService） */
    public static final String CACHE_DICT_TYPE = "DICT_TYPE";
}
