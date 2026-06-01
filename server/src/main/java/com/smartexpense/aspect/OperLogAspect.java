package com.smartexpense.aspect;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.smartexpense.annotation.OperLog;
import com.smartexpense.entity.SysOperLog;
import com.smartexpense.entity.SysUser;
import com.smartexpense.mapper.SysOperLogMapper;
import com.smartexpense.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志切面：拦截标注了 {@link OperLog} 的 Controller 方法，记录操作人、请求、结果与耗时。
 * 日志写入用 try-catch 包裹，日志失败不影响业务。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysOperLogMapper operLogMapper;
    private final SysUserMapper userMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint pjp, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperLog sysLog = new SysOperLog();
        sysLog.setTitle(operLog.value());
        sysLog.setMethod(pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName());
        sysLog.setCreateTime(LocalDateTime.now());

        // 请求信息（非 Web 上下文则跳过）
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            sysLog.setRequestMethod(request.getMethod());
            sysLog.setRequestUrl(request.getRequestURI());
            sysLog.setIp(clientIp(request));
        }
        sysLog.setRequestParams(serializeArgs(pjp.getArgs()));

        Object result;
        try {
            result = pjp.proceed();
            sysLog.setStatus(1);
        } catch (Throwable e) {
            sysLog.setStatus(0);
            sysLog.setErrorMsg(truncate(e.getMessage(), 500));
            throw e;
        } finally {
            fillOperator(sysLog);
            sysLog.setCostMs(System.currentTimeMillis() - start);
            try {
                operLogMapper.insert(sysLog);
            } catch (Exception ex) {
                log.error("操作日志写入失败: {}", ex.getMessage());
            }
        }
        return result;
    }

    /** 记录操作人（登录接口此时已登录成功，能取到 userId） */
    private void fillOperator(SysOperLog sysLog) {
        try {
            Object loginId = StpUtil.getLoginId();
            if (loginId != null) {
                Long userId = Long.valueOf(loginId.toString());
                sysLog.setUserId(userId);
                SysUser user = userMapper.selectById(userId);
                if (user != null) {
                    sysLog.setUsername(user.getUsername());
                }
            }
        } catch (Exception ignored) {
            // 未登录场景（登录失败等）不记录操作人
        }
    }

    /** 序列化入参：过滤文件/请求对象，并对 password 类字段脱敏 */
    private String serializeArgs(Object[] args) {
        List<String> parts = new ArrayList<>();
        for (Object a : args) {
            if (a == null || a instanceof MultipartFile || a instanceof HttpServletRequest
                    || a instanceof HttpServletResponse || a instanceof BindingResult) {
                continue;
            }
            String json = JSONUtil.toJsonStr(a);
            // 脱敏：password / oldPassword / newPassword 等字段值替换为 ***
            json = json.replaceAll("(?i)(\"[^\"]*password\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
            parts.add(json);
        }
        String s = String.join(", ", parts);
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
