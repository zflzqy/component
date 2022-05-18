package cn.zflzqy.shiroclient.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：zfl
 * @description：
 * @date ：2022/3/5 13:26
 */
@RequestMapping
@RestController
public class AuthorizationController {
    @RequestMapping(value = {"/unauthorized"})
    public String index(){
        return  "<span>没有权限，<a href=\"/logout/cas\">重新登录</a></span>";
    }
}
