package org.example.aispringboot.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.DTO.command.UserLoginCommandDTO;
import org.example.aispringboot.DTO.command.UserRegisterCommandDTO;
import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.enumClass.UserType;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.UserMapper;
import org.example.aispringboot.service.convert.UserConvert;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
	@Resource
	private UserMapper userMapper;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public UserLoginResponseDTO login(UserLoginCommandDTO commandDTO) {
		//构建查询条件
		LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(User::getUsername, commandDTO.getUsername())
				.or()
				.eq(User::getEmail, commandDTO.getUsername());
		User user = userMapper.selectOne(queryWrapper);
		System.out.println(user);

		//判断用户是否存在
		if (user == null) {
			throw new BusinessException("用户不存在");
		}

		String inputPassword = commandDTO.getPassword().trim();
		//验证密码
		if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
			throw new BusinessException("密码错误");
		}

		//检查用户的状态
		if (!user.isActive()) {
			throw new BusinessException("用户已被禁用,请联系管理员");
		}

		//生成jwt token
		String token = JwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
		System.out.println(token);
		//构建响应DTO
		UserLoginResponseDTO.UserDetailResponseDTO userInfo = UserConvert.entityToDetailResponse(user);
		return UserConvert.entityToLoginResponse(token, userInfo);

	}

	public UserLoginResponseDTO.UserDetailResponseDTO register(UserRegisterCommandDTO commandDTO) {
		System.out.println(JSONUtil.parseObj(commandDTO));
		//验证密码是否一致
		if (!commandDTO.getPassword().equals(commandDTO.getConfirmPassword())) {
			throw new BusinessException("两次输入密码不一致");
		}

		//验证用户名是否存在
		LambdaQueryWrapper<User> userNameQuery = new LambdaQueryWrapper<>();
		userNameQuery.eq(User::getUsername, commandDTO.getUsername());
		if (userMapper.selectCount(userNameQuery) > 0) {
			throw new BusinessException("用户名已存在");
		}

		//检查邮箱是否存在
		LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
		emailQuery.eq(User::getEmail, commandDTO.getEmail());
		if (userMapper.selectCount(emailQuery) > 0) {
			throw new BusinessException("邮箱已存在");
		}

		// 公开注册只能创建普通用户，不能信任客户端提交的角色。
		commandDTO.setUserType(UserType.USER.getCode());

		//创建用户
		String password = commandDTO.getPassword();
		String encryptedPassword = passwordEncoder.encode(password);
		User user = UserConvert.registerCommandToEntity(commandDTO,encryptedPassword);
		userMapper.insert(user);

		return UserConvert.entityToDetailResponse(user);
	}

	public UserLoginResponseDTO.UserDetailResponseDTO getUserById(Long userId){
		User user = userMapper.selectById(userId);
		if(user == null){
			throw new BusinessException("用户不存在");
		}
		return UserConvert.entityToDetailResponse(user);
	}
}
