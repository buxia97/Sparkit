package com.sparkit.framework.handler;

import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMsg());
        return R.fail(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public R<?> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", msg);
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<?> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingParamException(MissingServletRequestParameterException e) {
        log.warn("缺少参数: {}", e.getParameterName());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), "缺少参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), "请求方法不支持: " + e.getMethod());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public R<?> handleNotFound(NoHandlerFoundException e) {
        log.warn("接口不存在: {}", e.getRequestURL());
        return R.fail(ErrorCode.NOT_FOUND.getCode(), "接口不存在");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public R<?> handleDuplicateKey(DuplicateKeyException e) {
        log.error("数据重复: {}", e.getMessage());
        return R.fail(ErrorCode.CONFLICT.getCode(), "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public R<?> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("数据完整性异常: {}", e.getMessage());
        return R.fail(ErrorCode.INTERNAL_ERROR.getCode(), "数据操作异常");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<?> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("文件上传超出大小限制: {}", e.getMessage());
        return R.fail(ErrorCode.FILE_UPLOAD_FAIL.getCode(), "文件大小超出限制");
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(ErrorCode.INTERNAL_ERROR.getCode(), "系统异常");
    }
}