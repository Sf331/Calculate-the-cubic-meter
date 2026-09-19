package com.kelifang.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录态。存在 HttpSession 里，也放在 UserContext 里供 Service 层取用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser implements Serializable {

    private Long id;
    private String username;
    private String realName;

    /** PRINCIPAL / ACADEMIC / TEACHER / STUDENT / PARENT */
    private String role;
}
