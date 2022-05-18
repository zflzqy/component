package cn.zflzqy.shiroclient.exception;

import cn.hutool.core.util.StrUtil;
import org.apache.shiro.authz.AuthorizationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class AuthorizationControllerAdvice {
    public AuthorizationControllerAdvice() {
    }

    @ExceptionHandler({AuthorizationException.class})
    public ModelAndView resolveAuthorizationException(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        if (StrUtil.contains(request.getContentType(), "application/json")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            return new ModelAndView();
        } else {
            return new ModelAndView("redirect:/unauthorized");
        }
    }
}