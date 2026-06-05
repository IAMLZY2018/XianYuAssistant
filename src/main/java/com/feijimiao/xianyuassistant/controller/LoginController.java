package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.annotation.NoAuth;
import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.CheckUserExistsRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.LoginReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.LoginRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.RegisterReqDTO;
import com.feijimiao.xianyuassistant.service.AuthService;
import com.feijimiao.xianyuassistant.service.bo.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 登录控制器
 * @author IAMLZY
 * @date 2026/4/22
 */
@Slf4j
@RestController
@RequestMapping("/api/login")
@CrossOrigin(origins = "*")
@NoAuth
public class LoginController {

    @Autowired
    private AuthService authService;

    /**
     * 检查是否有用户（决定显示登录还是注册）
     */
    @PostMapping("/checkUserExists")
    public ResultObject<CheckUserExistsRespDTO> checkUserExists() {
        try {
            CheckUserExistsRespBO respBO = authService.checkUserExists();
            CheckUserExistsRespDTO respDTO = new CheckUserExistsRespDTO();
            respDTO.setExists(respBO.getExists());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("检查用户存在失败", e);
            return ResultObject.failed("检查用户存在失败: " + e.getMessage());
        }
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public ResultObject<LoginRespDTO> register(@RequestBody RegisterReqDTO reqDTO, HttpServletRequest request) {
        try {
            // 参数校验
            if (reqDTO.getUsername() == null || reqDTO.getUsername().trim().isEmpty()) {
                return ResultObject.validateFailed("用户名不能为空");
            }
            if (reqDTO.getPassword() == null || reqDTO.getPassword().trim().isEmpty()) {
                return ResultObject.validateFailed("密码不能为空");
            }
            if (reqDTO.getConfirmPassword() == null || !reqDTO.getPassword().equals(reqDTO.getConfirmPassword())) {
                return ResultObject.validateFailed("两次密码不一致");
            }
            if (reqDTO.getUsername().length() < 3 || reqDTO.getUsername().length() > 20) {
                return ResultObject.validateFailed("用户名长度需在3-20之间");
            }
            if (reqDTO.getPassword().length() < 6 || reqDTO.getPassword().length() > 50) {
                return ResultObject.validateFailed("密码长度需在6-50之间");
            }

            RegisterReqBO reqBO = new RegisterReqBO();
            reqBO.setUsername(reqDTO.getUsername().trim());
            reqBO.setPassword(reqDTO.getPassword());

            LoginRespBO respBO = authService.register(reqBO);

            LoginRespDTO respDTO = new LoginRespDTO();
            respDTO.setToken(respBO.getToken());
            respDTO.setUsername(respBO.getUsername());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("注册失败", e);
            return ResultObject.failed(e.getMessage());
        }
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public ResultObject<LoginRespDTO> login(@RequestBody LoginReqDTO reqDTO, HttpServletRequest request) {
        try {
            String ip = getClientIp(request);

            // 检查登录错误次数限制
            if (!authService.checkLoginAttempt(ip)) {
                return ResultObject.failed("登录错误次数过多，请10分钟后再试");
            }

            // 参数校验
            if (reqDTO.getUsername() == null || reqDTO.getUsername().trim().isEmpty()) {
                return ResultObject.validateFailed("用户名不能为空");
            }
            if (reqDTO.getPassword() == null || reqDTO.getPassword().trim().isEmpty()) {
                return ResultObject.validateFailed("密码不能为空");
            }

            String deviceId = request.getHeader("User-Agent");
            if (deviceId != null && deviceId.length() > 100) {
                deviceId = deviceId.substring(0, 100);
            }

            LoginReqBO reqBO = new LoginReqBO();
            reqBO.setUsername(reqDTO.getUsername().trim());
            reqBO.setPassword(reqDTO.getPassword());
            reqBO.setIp(ip);
            reqBO.setDeviceId(deviceId);

            LoginRespBO respBO = authService.login(reqBO);

            // 登录成功，清除失败记录
            authService.clearLoginFailure(ip);

            LoginRespDTO respDTO = new LoginRespDTO();
            respDTO.setToken(respBO.getToken());
            respDTO.setUsername(respBO.getUsername());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("登录失败", e);
            // 登录失败，记录错误次数
            String ip = getClientIp(request);
            authService.recordLoginFailure(ip);
            return ResultObject.failed(e.getMessage());
        }
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ResultObject<?> logout(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            authService.logout(token);
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("退出登录失败", e);
            return ResultObject.failed("退出登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
