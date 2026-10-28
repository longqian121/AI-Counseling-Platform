package org.example.aispringboot.common;

import org.example.aispringboot.exception.BusinessException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobarExceptionHandler {
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining(", "));
		return Result.error(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMsg(), message);
	}

	@ExceptionHandler(BusinessException.class)
	public Result<?> handleBusinessException(BusinessException e) {
		if (e.getData() != null) {
			return Result.error(e.getCode(), e.getMessage(), e.getData());
		}
		return Result.error(e.getCode(), e.getMessage(), null);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
		return Result.error(ResultCode.FILE_SIZE_EXCEEDED.getCode(), "文件大小超过限制，最大允�?0MB", null);
	}
}