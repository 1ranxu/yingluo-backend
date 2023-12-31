package com.luoying.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.luoying.common.ErrorCode;
import com.luoying.common.Result;
import com.luoying.exception.BusinessException;
import com.luoying.model.domain.User;
import com.luoying.model.vo.UserVO;
import com.luoying.model.request.UserLoginRequest;
import com.luoying.model.request.UserQueryRequest;
import com.luoying.model.request.UserRegisterRequest;
import com.luoying.model.vo.UserListVO;
import com.luoying.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户接口
 *
 * @author 落樱的悔恨
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;


    @PostMapping("/register")
    public Result userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户注册请求对象空值");
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String authCode = userRegisterRequest.getAuthCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, authCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户注册请求对象属性空值");
        }
        long userId = userService.userRegister(userAccount, userPassword, checkPassword, authCode);
        return Result.success(userId);
    }

    @PostMapping("/login")
    public Result userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户登录请求对象空值");
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户登录请求对象属性空值");
        }
        UserVO userVO = userService.userLogin(userAccount, userPassword, request);
        return Result.success(userVO);
    }


    @PostMapping("/loginout")
    public Result userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "HttpServletRequest请求空值");
        }
        int result = userService.userLogout(request);
        return Result.success(result);
    }

    @GetMapping("/current")
    public Result getCurrentUser(HttpServletRequest request) {
        //获取登录用户
        UserVO loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN, "用户未登录");
        }
        Long loginUserId = loginUser.getId();
        //查询最新的用户信息
        User user = userService.getById(loginUserId);
        //  用户信息（脱敏）
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        return Result.success(userVO);
    }

    @PostMapping("/query")
    public Result userListQuery(@RequestBody UserQueryRequest userQueryRequest, HttpServletRequest request) {
        UserListVO userListVO = userService.userListQuery(userQueryRequest, request);
        return Result.success(userListVO);
    }

    // todo 匹配多个
    @GetMapping("/recommend")
    public Result usersRecommend(long currentPage, long pageSize, HttpServletRequest request) {
        //返回数据
        return Result.success(userService.usersRecommend(currentPage, pageSize, request));
    }

    @PostMapping("/delete")
    public Result userDelete(@RequestBody User user, HttpServletRequest request) {
        // 1 鉴权，仅管理员可删除
        if (!userService.isAdmin(request)) throw new BusinessException(ErrorCode.NO_AUTH, "用户无权限");
        //2 删除
        if (user.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "删除用户的id不能为小于等于0");
        }
        boolean result = userService.removeById(user.getId());
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除用户失败");
        }
        return Result.success(result);
    }

    @PostMapping("/update")
    public Result userUpdate(@RequestBody User user, HttpServletRequest request) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改用户为空");
        }
        //todo如果用户没有传任何要更新的值，直接抛异常
        UserVO loginUser = userService.getLoginUser(request);
        //更新，前端传过来的数据有就更新，没有就保持默认
        int result = userService.updateUser(user, loginUser);
        return Result.success(result);
    }

    @GetMapping("/searchByTags")
    public Result searchByTags(@RequestParam(required = false) List<String> tags) {
        if (CollectionUtil.isEmpty(tags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签为空");
        }
        return Result.success(userService.queryUsersByTagsBySQL(tags));
    }

    /**
     * 获取与当前用户相似度最高的用户
     * @param num 需要匹配用户的个数
     * @param request
     * @return
     */
    @GetMapping("/match")
    public Result usersMatch(long num, HttpServletRequest request) {
        if (num<=0 || num >20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取登录用户
        UserVO loginUser = userService.getLoginUser(request);
        //封装返回匹配的用户
        return Result.success(userService.usersMatch(num,loginUser));
    }
}
