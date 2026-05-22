package com.al.aichat.config;

import com.al.aichat.common.BusinessException;
import com.al.aichat.common.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 作用：
 *   1. 统一所有 API 异常返回 Result<T> 格式，前端不再需要处理多种错误结构
 *   2. 将异常信息标准化（生产环境可隐藏堆栈细节）
 *   3. 日志记录异常详情，方便排查问题
 *
 * 处理流程：
 *   请求 → Controller → 抛出异常 → 匹配 @ExceptionHandler → 返回 Result.error()
 *
 * 覆盖的异常类型：
 *   - 业务异常：BusinessException（Service 层抛出，携带对用户友好的提示）
 *   - 参数校验失败：@Valid + @NotBlank 等注解触发的 MethodArgumentNotValidException
 *   - 请求体缺失或格式错误：HttpMessageNotReadableException
 *   - 查询参数缺失：MissingServletRequestParameterException
 *   - 路径参数类型不匹配：MethodArgumentTypeMismatchException
 *   - 请求方法不允许：HttpRequestMethodNotSupportedException
 *   - 方法参数校验失败：ConstraintViolationException
 *   - 其他未预期异常：Exception（兜底，只记录日志不暴露细节）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 @Valid 参数校验失败
     *
     * 触发场景：DTO 上加了 @NotBlank / @Email 等注解，请求参数不合法时抛出
     *
     * 返回格式：
     * {
     *   "code": 400,
     *   "msg": "用户名不能为空; 邮箱格式不正确",
     *   "data": null
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理请求体缺失或格式错误
     *
     * 触发场景：
     *   - POST 请求没有传 body
     *   - JSON 格式错误（如缺少逗号、花括号不匹配）
     *   - 类型转换失败（如期望数字却传了字符串）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return Result.error(400, "请求参数格式错误");
    }

    /**
     * 处理缺少必填的查询参数
     *
     * 触发场景：
     *   GET /api/xxx?page=1  但缺少了必填的 size 参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getParameterName());
        return Result.error(400, "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 处理路径参数类型不匹配
     *
     * 触发场景：
     *   GET /api/session/abc  但路径参数期望是 Long 类型
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {} 应为 {}", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        return Result.error(400, "参数类型错误: " + e.getName());
    }

    /**
     * 处理业务异常（Service 层主动抛出）
     *
     * 触发场景：
     *   throw new BusinessException("用户名已存在");
     *   throw new BusinessException(404, "会话不存在");
     *
     * 这是"预期内的异常"——信息可以直接展示给用户。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求方法不允许（如 GET 访问 POST 接口）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不允许: {}", e.getMessage());
        return Result.error(405, "请求方法不允许");
    }

    /**
     * 处理 @RequestParam 或 @PathVariable 上的校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数约束校验失败: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理所有未预期的异常（兜底）
     *
     * 这里会打印完整堆栈，方便排查问题。
     * 生产环境中建议将堆栈替换为 "服务器内部错误" 等模糊提示。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器内部异常", e);
        return Result.error(500, "服务器内部错误");
    }
}