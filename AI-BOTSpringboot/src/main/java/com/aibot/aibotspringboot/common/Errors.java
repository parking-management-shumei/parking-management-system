package com.aibot.aibotspringboot.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class Errors {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Result> api(ApiException e) {
    return ResponseEntity.status(e.status).body(error(e.status, e.getMessage()));
  }

  @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
  public ResponseEntity<Result> invalid(Exception e) {
    return ResponseEntity.badRequest().body(error(400, "输入参数无效，请检查后重试"));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Result> conflict(Exception e) {
    return ResponseEntity.status(409).body(error(409, "数据冲突：账号、手机号、车牌或车位已存在/被占用"));
  }

  public static Result error(int code, String message) {
    Result r = Result.fail();
    r.setCode(code);
    r.setMsg(message);
    return r;
  }
}
