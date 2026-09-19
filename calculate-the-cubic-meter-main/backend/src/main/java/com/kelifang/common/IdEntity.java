package com.kelifang.common;

/**
 * 实体都要有主键。Lombok 的 @Data 会生成 getId/setId，所以实体只需 implements 这个接口，不用写任何代码。
 */
public interface IdEntity {

    Long getId();

    void setId(Long id);
}
