import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zfl
 * @Date: 2022-05-22-18:30
 * @Description:
 */
public class Test {
    public static void main(String[] args) {

        Map<String, Object> map = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("uid", Integer.parseInt("123"));
                put("expire_time", System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 15);
            }
        };

        String token = JWTUtil.createToken(map, "1234".getBytes());
        System.out.println(token);
        JWT jwt = JWTUtil.parseToken(token);
        System.out.println(jwt.toString());
    }
}
