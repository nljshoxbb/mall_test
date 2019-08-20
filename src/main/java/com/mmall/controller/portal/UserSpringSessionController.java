package com.mmall.controller.portal;


import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/springseesion")
public class UserSpringSessionController {

    @Autowired
    private IUserService iUserService;

    @RequestMapping(value = "login.do", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<User> login(
            String username, String password,
            HttpSession session,
            HttpServletResponse httpServletResponse,
            HttpServletRequest httpServletRequest) {

        //
        int i = 0;
        int j = 666 / i;

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return ServerResponse.createByErrorMessage("用户名或密码为空");
        }

        ServerResponse<User> response = iUserService.login(username, password);

        if (response.isSuccess()) {
//            CookieUtil.writeLoginToken(httpServletResponse, session.getId());
//            RedisShardedPoolUtil.setEx(session.getId(), JsonUtil.obj2String(response.getData()), Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
             session.setAttribute(Const.CURRENT_USER,response.getData());
        }

        return response;
    }


    @RequestMapping(value = "logout.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {
//        String loginToken = CookieUtil.readLoginToken(request);
//        CookieUtil.delLoginToken(request,response);
//        RedisShardedPoolUtil.del(loginToken);

        session.removeAttribute(Const.CURRENT_USER);

        return ServerResponse.createBySuccess();
    }


    @RequestMapping(value = "get_user_info.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {
//        String loginToken = CookieUtil.readLoginToken(request);
//        if (StringUtils.isEmpty(loginToken)) {
//            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
//        }
//        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
//        User user = JsonUtil.string2Obj(userJsonStr, User.class);
        User user =(User) session.getAttribute(Const.CURRENT_USER);

        if (user != null) {
            return ServerResponse.createBySuccess(user);
        }
        return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
    }



}
