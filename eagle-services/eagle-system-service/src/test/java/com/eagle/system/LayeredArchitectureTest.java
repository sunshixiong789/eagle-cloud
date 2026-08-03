package com.eagle.system;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * DDD 分层约束验证(ArchUnit)。
 * <p>
 * 与 {@link ModulithArchitectureTest} 的分工:
 * <ul>
 *   <li>Modulith 验证模块 <b>之间</b> 的边界(base / file / message 谁能依赖谁)</li>
 *   <li>本测试验证模块 <b>内部</b> 的分层(interfaces / application / domain / infrastructure)
 *       —— 这是 Modulith 看不见的盲区</li>
 * </ul>
 * 依赖方向: {@code interfaces -> application -> domain <- infrastructure}
 *
 * <p>注解一律用 <b>全限定名字符串</b> 匹配: Lombok 是 {@code compileOnly},不在测试 classpath 上。
 *
 * <p><strong>运行</strong>
 * <pre>gradle :eagle-services:eagle-system-service:test --tests "*LayeredArchitectureTest"</pre>
 */
@DisplayName("DDD 分层约束验证")
class LayeredArchitectureTest {

    private static final String ENTITY = "jakarta.persistence.Entity";
    private static final String ENUMERATED = "jakarta.persistence.Enumerated";
    private static final String AGGREGATE_ROOT = "com.eagle.datajpa.base.BaseAggregateRoot";

    /** 只导入主代码,排除测试类与构建产物。 */
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.eagle.system");

    /**
     * 冻结存量违例:基线记录在 {@code archunit_store/},此后只有 <b>新增</b> 违例才会让测试失败。
     * <p>
     * 存量清理进度见 {@code agent-plugin/rules/07-checklist.md} 的违例台账;
     * 修好一处就从 store 里消失一处,不会倒退。
     */
    private static void freeze(ArchRule rule) {
        FreezingArchRule.freeze(rule).check(CLASSES);
    }

    @Nested
    @DisplayName("依赖方向")
    class DependencyDirection {

        @Test
        @DisplayName("domain 层不得依赖 application / interfaces / infrastructure")
        void domainShouldNotDependOnOuterLayers() {
            freeze(noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..interfaces..", "..infrastructure..")
                    .because("领域层必须保持稳定,不能反向依赖外层(见 rules/02-architecture.md)")
                    );
        }

        @Test
        @DisplayName("interfaces 层不得直接依赖 infrastructure")
        void interfacesShouldNotDependOnInfrastructure() {
            freeze(noClasses().that().resideInAPackage("..interfaces..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("Controller 应经 application 编排,不直接触碰基础设施实现")
                    );
        }

        @Test
        @DisplayName("domain 层不得依赖 Web / Servlet 框架")
        void domainShouldNotDependOnWebFramework() {
            freeze(noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.web..", "jakarta.servlet..")
                    .because("领域模型不应与传输层耦合")
                    );
        }
    }

    @Nested
    @DisplayName("聚合根与实体")
    class AggregateRules {

        @Test
        @DisplayName("JPA 实体不得标注 @Data 或 @Builder")
        void entitiesShouldNotUseDataOrBuilder() {
            freeze(noClasses().that().areAnnotatedWith(ENTITY)
                    .should().beAnnotatedWith("lombok.Data")
                    .orShould().beAnnotatedWith("lombok.Builder")
                    .because("实体需静态工厂 + 业务方法改状态,不暴露 setter(见 rules/00-core.md)")
                    .allowEmptyShould(true)
                    );
        }

        @Test
        @DisplayName("领域模型不得暴露 public setter")
        void domainModelsShouldNotExposeSetters() {
            freeze(classes().that().areAssignableTo(AGGREGATE_ROOT)
                    .should(new ArchCondition<JavaClass>("不含 public setXxx 方法") {
                        @Override
                        public void check(JavaClass item, ConditionEvents events) {
                            item.getMethods().stream()
                                    .filter(m -> m.getName().startsWith("set")
                                            && m.getModifiers().contains(JavaModifier.PUBLIC))
                                    .forEach(m -> events.add(SimpleConditionEvent
                                            .violated(m, m.getFullName() + " 是 public setter")));
                        }
                    })
                    .because("聚合根通过业务方法改状态;子实体按 rules/00-core.md 允许 @Setter")
                    .allowEmptyShould(true)
                    );
        }

        @Test
        @DisplayName("@Enumerated 字段必须用 EnumType.STRING")
        void enumFieldsShouldUseStringType() {
            freeze(fields().that().areAnnotatedWith(ENUMERATED)
                    .should(new ArchCondition<JavaField>("标注 EnumType.STRING") {
                        @Override
                        public void check(JavaField item, ConditionEvents events) {
                            // 未显式指定 value 时默认 ORDINAL,同样违规
                            String value = item.tryGetAnnotationOfType(ENUMERATED)
                                    .map(a -> String.valueOf(a.getProperties().get("value")))
                                    .orElse("ORDINAL");
                            if (!value.contains("STRING")) {
                                events.add(SimpleConditionEvent.violated(item,
                                        item.getFullName() + " 用了 " + value + ",应为 STRING"));
                            }
                        }
                    })
                    .because("默认 ORDINAL 会让枚举重排后历史数据语义错乱(见 rules/04-data.md)")
                    .allowEmptyShould(true)
                    );
        }
    }

    @Nested
    @DisplayName("组件职责")
    class ComponentResponsibility {

        @Test
        @DisplayName("Mapper 不得依赖 Repository")
        void mappersShouldNotDependOnRepositories() {
            freeze(noClasses().that().resideInAPackage("..application.mapper..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                    .because("Mapper 只做字段映射,不做跨聚合查询(见 rules/02-architecture.md)")
                    .allowEmptyShould(true)
                    );
        }

        @Test
        @DisplayName("禁止使用 BeanUtils 反射拷贝")
        void shouldNotUseBeanUtils() {
            freeze(noClasses().should().dependOnClassesThat()
                    .haveFullyQualifiedName("org.springframework.beans.BeanUtils")
                    .because("禁止反射式映射,用 record 静态工厂或 @Component Mapper(见 rules/02-architecture.md)")
                    );
        }

        @Test
        @DisplayName("应用服务命名为 XxxApplicationService")
        void applicationServicesShouldBeNamedCorrectly() {
            freeze(classes().that().resideInAPackage("..application.service..")
                    .and().areNotNestedClasses()
                    .should().haveSimpleNameEndingWith("ApplicationService")
                    .because("见 rules/00-core.md 的 DDD 命名约定")
                    .allowEmptyShould(true)
                    );
        }

        @Test
        @DisplayName("Controller 不得捕获异常")
        void controllersShouldNotCatchExceptions() {
            freeze(methods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
                    .should(new ArchCondition<JavaMethod>("不出现 try-catch") {
                        @Override
                        public void check(JavaMethod item, ConditionEvents events) {
                            if (!item.getTryCatchBlocks().isEmpty()) {
                                events.add(SimpleConditionEvent.violated(item,
                                        item.getFullName() + " 含 try-catch"));
                            }
                        }
                    })
                    .because("Controller 只做入参校验和响应封装,异常交给全局处理器(见 rules/03-api-error.md)")
                    .allowEmptyShould(true)
                    );
        }
    }
}
