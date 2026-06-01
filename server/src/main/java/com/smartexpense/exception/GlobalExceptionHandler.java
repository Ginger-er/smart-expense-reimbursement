package com.smartexpense.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.smartexpense.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.error(400, message);
    }

    /** 参数绑定异常 */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.error(400, message);
    }

    /** 缺少请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getMessage());
        return Result.error(400, "缺少必要参数: " + e.getParameterName());
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return Result.error(405, "请求方法不支持: " + e.getMethod());
    }

    /** 404 */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("接口不存在: {}", e.getRequestURL());
        return Result.error(404, "接口不存在");
    }

    /** 文件上传大小超限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超限: {}", e.getMessage());
        return Result.error(413, "上传文件大小不能超过10MB");
    }

    /** JSON解析异常 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error(400, "请求参数格式错误");
    }

    /** Sa-Token 未登录 */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleNotLoginException(NotLoginException e) {
        log.warn("未登录访问: {}", e.getMessage());
        return Result.error(401, "请先登录");
    }

    /** Sa-Token 权限不足 */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleNotPermissionException(NotPermissionException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error(403, "权限不足");
    }

    /** Sa-Token 角色不足 */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleNotRoleException(NotRoleException e) {
        log.warn("角色不足: {}", e.getMessage());
        return Result.error(403, "角色权限不足");
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(500, "服务器内部错误，请联系管理员");
    }
}
