package com.sparkit.common.exception;

import com.sparkit.common.enums.ErrorCode;
import lombok.Getter;

import java.text.MessageFormat;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String msg;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(MessageFormat.format(errorCode.getMsg(), args));
        this.code = errorCode.getCode();
        this.msg = MessageFormat.format(errorCode.getMsg(), args);
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(String msg) {
        super(msg);
        this.code = 500;
        this.msg = msg;
    }
}