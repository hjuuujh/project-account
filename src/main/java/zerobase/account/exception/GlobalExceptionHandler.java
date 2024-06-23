package zerobase.account.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import zerobase.account.dto.ErrorResponse;
import zerobase.account.type.ErrorCode;

import static zerobase.account.type.ErrorCode.INTERNAL_SERVER_ERROR;
import static zerobase.account.type.ErrorCode.INVALID_REQUEST;

@Slf4j
@RestControllerAdvice // 전역적으로 예외처리, @ResponseBody가 붙어 있어 응답이 Json
public class GlobalExceptionHandler {

    // 만든 에러
    @ExceptionHandler(AccountException.class)
    public ErrorResponse handleAccountException(AccountException e) {
        log.error("{} is occurred", e.getErrorCode());

        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    // db unique key 중복
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("DataIntegrityViolationException is occurred", e);
        return new ErrorResponse(INVALID_REQUEST, INVALID_REQUEST.getDescription());

    }

    // 그외 에러
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception e) {
        log.error("Exception is occurred", e);

        return new ErrorResponse(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getDescription());
    }
}
