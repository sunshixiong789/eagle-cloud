package com.eagle.system.base.domain.model.valueobject;

import com.eagle.system.base.domain.model.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户资料（值对象）
 * <p>
 * 值对象特征：
 * <ul>
 *   <li>不可变：通过创建新对象来修改</li>
 *   <li>无标识：通过属性值判断相等性</li>
 *   <li>可替换：整体替换而非修改属性</li>
 * </ul>
 *
 * @author 孙士雄
 */
@Embeddable
@Getter
@NoArgsConstructor  // JPA 需要
@AllArgsConstructor
public class UserProfile {

    @Column(length = 500, comment = "头像 URL")
    private String avatar;

    @Column(length = 64, comment = "昵称")
    private String nickname;

    @Column(length = 64, comment = "真实姓名")
    private String name;

    @Column(length = 20, comment = "性别")
    @Enumerated
    private Gender gender;

    @Column(comment = "个人简介")
    private String bio;

    /**
     * 更新用户资料（返回新对象，保持不可变性）
     *
     * @param name     真实姓名
     * @param nickname 昵称
     * @param avatar   头像 URL
     * @return 新的 UserProfile 对象
     */
    public UserProfile update(String name, String nickname, String avatar) {
        return new UserProfile(
                avatar != null ? avatar : this.avatar,
                nickname != null ? nickname : this.nickname,
                name != null ? name : this.name,
                this.gender,
                this.bio
        );
    }

    /**
     * 更新性别和简介
     *
     * @param gender 性别
     * @param bio    个人简介
     * @return 新的 UserProfile 对象
     */
    public UserProfile updateGenderAndBio(Gender gender, String bio) {
        return new UserProfile(
                this.avatar,
                this.nickname,
                this.name,
                gender != null ? gender : this.gender,
                bio != null ? bio : this.bio
        );
    }
}
