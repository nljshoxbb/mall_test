package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import com.mmall.util.RedisPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;


    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户不存在");
        }

        //TODO 密码登录MD5
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username, md5Password);

        if (user == null) {
            return ServerResponse.createByErrorMessage("密码错误");
        }

        user.setPassword(StringUtils.EMPTY);


        return ServerResponse.createBySuccess("登录成功", user);
    }

    public ServerResponse<String> register(User user) {
        ServerResponse validResponse = this.checkValid(user.getUsername(), Const.USERNAME);

        if (!validResponse.isSuccess()) {
            return validResponse;
        }
        validResponse = this.checkValid(user.getUsername(), Const.EMAIL);

        if (!validResponse.isSuccess()) {
            return validResponse;
        }

        user.setRole(Const.Role.ROLE_CUSTOMER);
        // MD5 加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        int resultCount = userMapper.insert(user);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    @Override
    public ServerResponse<String> checkValid(String str, String type) {
        if (StringUtils.isNotBlank(type)) {
            if (Const.USERNAME.equals(type)) {
                int resulCount = userMapper.checkUsername(str);
                if (resulCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            }
            if (Const.EMAIL.equals(type)) {
                int resulCount = userMapper.checkEmail(str);
                if (resulCount > 0) {
                    return ServerResponse.createByErrorMessage("email已存在");
                }
            }
        } else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMessage("校验成功");
    }


    @Override
    public ServerResponse selectQuestion(String username) {
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (validResponse.isSuccess()) {
            // 用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if (StringUtils.isNotBlank(question)) {
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMessage("找回密码的问题是空的");
    }

    public ServerResponse<String> checkAnswer(String username, String qustion, String answer) {
        int resultCount = userMapper.checkAnswer(username, qustion, answer);
        if (resultCount > 0) {
            String forgetToken = UUID.randomUUID().toString();
            RedisPoolUtil.setEx(Const.TOKEN_PREFIX+username,forgetToken,60*60*12);
            return ServerResponse.createBySuccess(forgetToken);
        }

        return ServerResponse.createByErrorMessage("问题的答案错误");
    }

    public ServerResponse<String> forgetResetPassword(String username, String password, String forgetToken) {
        if (StringUtils.isBlank(forgetToken)) {
            return ServerResponse.createByErrorMessage("参数错误，token必传");
        }
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (validResponse.isSuccess()) {
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        String token = RedisPoolUtil.get(Const.TOKEN_PREFIX+username);
        if (StringUtils.isBlank(token)) {
            return ServerResponse.createByErrorMessage("token 无效或者过期");
        }

        if (StringUtils.equals(forgetToken, token)) {
            String md5Password = MD5Util.MD5EncodeUtf8(password);
            int rowCount = userMapper.updatePasswordByUsername(username, md5Password);

            if (rowCount > 0) {
                return ServerResponse.createBySuccessMessage("修改密码成功");
            }
        } else {
            return ServerResponse.createByErrorMessage("token 错误，请重新获取重置密码的token");
        }

        return ServerResponse.createByErrorMessage("修改密码失败");
    }


    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user) {
        if (StringUtils.isBlank(passwordNew)) {
            return ServerResponse.createByErrorMessage("新密码不能为空");
        }
        // 防止横向越权，校验用户旧密码，一定要指定用户，如果不指定id.
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld), user.getId());
        if (resultCount > 0) {
            return ServerResponse.createByErrorMessage("旧密码错误");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));

        int updateCount = userMapper.updateByPrimaryKeySelective(user);

        if (updateCount > 0) {
            return ServerResponse.createBySuccessMessage("密码更新成功");
        }
        return ServerResponse.createByErrorMessage("密码更新失败");
    }

    public ServerResponse<User> updateInformation(User user) {
        // username 不能被更新
        // email也要进行校验，校验新的email是不是已经存在，并且如果email相同，不能是当前这个用户的
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());

        if (resultCount > 0) {
            return ServerResponse.createByErrorMessage("EMAIL已经存在，请更换email再尝试");
        }
        User updateUser = new User();

        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if (updateCount > 0) {
            return ServerResponse.createBySuccess("更新个人信息成功", updateUser);

        }
        return ServerResponse.createByErrorMessage("更新个人信息失败");

    }

    public ServerResponse<User> getInformation(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            return ServerResponse.createByErrorMessage("找不到用户");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }

    // backend
    public ServerResponse checkAdminRole(User user) {
        if (user != null && user.getRole().intValue() == Const.Role.ROLE_ADMIN) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }


}
