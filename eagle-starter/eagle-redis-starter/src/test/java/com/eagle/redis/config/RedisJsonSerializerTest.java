package com.eagle.redis.config;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip 测试 {@link RedisCacheConfig#redisJsonSerializer(ObjectMapper)}。
 *
 * <p>覆盖 {@code Stream.toList()} / {@code List.of()} / {@code Map.of()} 等
 * final 不可变集合在 {@code DefaultTyping.NON_FINAL} 下的边界，
 * 以及 fix 前裸 {@code []} / {@code {}} 老数据的兼容反序列化。
 */
class RedisJsonSerializerTest {

    private final RedisCacheConfig config = new RedisCacheConfig();
    private RedisSerializer<Object> serializer;

    @BeforeEach
    void setUp() {
        serializer = config.redisJsonSerializer(config.redisObjectMapper());
    }

    @Test
    @DisplayName("null in → null out / 空字节也是 null")
    void nullRoundTrip() {
        assertNull(serializer.serialize(null));
        assertNull(serializer.deserialize(null));
        assertNull(serializer.deserialize(new byte[0]));
    }

    @Test
    @DisplayName("Stream.toList() 空列表能 round-trip 回非 null 空 List")
    void emptyImmutableListFromStreamToList() {
        List<String> empty = Stream.<String>empty().toList();
        // 真的是 final 不可变实现
        assertTrue(java.lang.reflect.Modifier.isFinal(empty.getClass().getModifiers()),
                "precondition: Stream.toList() returns a final class");

        byte[] bytes = serializer.serialize(empty);
        Object back = serializer.deserialize(bytes);

        assertInstanceOf(List.class, back);
        assertEquals(0, ((List<?>) back).size());
    }

    @Test
    @DisplayName("Stream.toList() 非空列表能 round-trip 还原元素")
    void nonEmptyImmutableListFromStreamToList() {
        List<String> immutable = Stream.of("a", "b", "c").toList();
        assertTrue(java.lang.reflect.Modifier.isFinal(immutable.getClass().getModifiers()),
                "precondition: Stream.toList() returns a final class");

        byte[] bytes = serializer.serialize(immutable);
        Object back = serializer.deserialize(bytes);

        assertInstanceOf(List.class, back);
        assertEquals(List.of("a", "b", "c"), back);
    }

    @Test
    @DisplayName("List.of(...) 也能 round-trip")
    void listOfRoundTrip() {
        Object back = roundTrip(List.of(1, 2, 3));
        assertEquals(List.of(1, 2, 3), back);
    }

    @Test
    @DisplayName("Map.of(...) 也能 round-trip")
    void mapOfRoundTrip() {
        Map<String, Integer> source = Map.of("a", 1, "b", 2);
        Object back = roundTrip(source);

        assertInstanceOf(Map.class, back);
        assertEquals(source, back);
    }

    @Test
    @DisplayName("Set.of(...) 也能 round-trip")
    void setOfRoundTrip() {
        Set<String> source = Set.of("x", "y");
        Object back = roundTrip(source);

        assertInstanceOf(Set.class, back);
        assertEquals(source, back);
    }

    @Test
    @DisplayName("Collections.emptyList() / singletonList() 兼容")
    void collectionsImmutableViews() {
        assertEquals(List.of(), roundTrip(Collections.emptyList()));
        assertEquals(List.of("only"), roundTrip(Collections.singletonList("only")));
        assertEquals(Map.of("k", "v"), roundTrip(Collections.singletonMap("k", "v")));
    }

    @Test
    @DisplayName("ArrayList / LinkedHashMap 等非 final 类原样保留运行时类型")
    void nonFinalClassesPreservedExactly() {
        ArrayList<String> list = new ArrayList<>(List.of("a", "b"));
        Object backList = roundTrip(list);
        assertInstanceOf(ArrayList.class, backList);
        assertEquals(list, backList);

        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Object backMap = roundTrip(map);
        assertInstanceOf(LinkedHashMap.class, backMap);
        assertEquals(map, backMap);
    }

    @Test
    @DisplayName("TreeMap 等非 final 但非 HashMap 类型不被 normalize 掉")
    void treeMapStaysTreeMap() {
        TreeMap<String, Integer> tree = new TreeMap<>();
        tree.put("a", 1);
        tree.put("b", 2);

        Object back = roundTrip(tree);
        assertInstanceOf(TreeMap.class, back, "TreeMap 不该被替换为 LinkedHashMap");
        assertEquals(tree, back);
    }

    @Test
    @DisplayName("POJO 单对象 round-trip 还原类型")
    void pojoRoundTrip() {
        SamplePojo source = new SamplePojo("eagle", 42);
        Object back = roundTrip(source);
        assertEquals(source, back);
    }

    @Test
    @DisplayName("record 根对象 round-trip 还原类型")
    void recordRoundTrip() {
        SampleRecord source = new SampleRecord("eagle", List.of("redis", "cache"));
        Object back = roundTrip(source);

        assertInstanceOf(SampleRecord.class, back);
        assertEquals(source, back);
    }

    @Test
    @DisplayName("老数据兼容:fix 前裸 [] 反序列化为空 List")
    void legacyBareEmptyArray() {
        byte[] legacy = "[]".getBytes(StandardCharsets.UTF_8);
        Object back = serializer.deserialize(legacy);
        assertInstanceOf(List.class, back);
        assertEquals(0, ((List<?>) back).size());
    }

    @Test
    @DisplayName("老数据兼容:fix 前裸 {} 反序列化为空 Map")
    void legacyBareEmptyObject() {
        byte[] legacy = "{}".getBytes(StandardCharsets.UTF_8);
        Object back = serializer.deserialize(legacy);
        assertInstanceOf(Map.class, back);
        assertEquals(0, ((Map<?, ?>) back).size());
    }

    private Object roundTrip(Object value) {
        byte[] bytes = serializer.serialize(value);
        return serializer.deserialize(bytes);
    }

    /**
     * 测试用 POJO,非 final,public 构造器 + setter。
     */
    public static class SamplePojo {
        private String name;
        private int age;

        public SamplePojo() {
        }

        public SamplePojo(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SamplePojo that)) return false;
            return age == that.age && java.util.Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, age);
        }
    }

    public record SampleRecord(String name, List<String> tags) {
    }
}
