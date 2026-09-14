package com.kelifang.auth;

import com.kelifang.auth.dto.LoginRequest;
import com.kelifang.common.CurrentUser;
import com.kelifang.common.Result;
import com.kelifang.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<CurrentUser> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        CurrentUser user = authService.login(req);
        request.getSession(true).setAttribute(UserContext.SESSION_KEY, user);
        return Result.ok(user);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<CurrentUser> me() {
        return Result.ok(UserContext.get());
    }
}
