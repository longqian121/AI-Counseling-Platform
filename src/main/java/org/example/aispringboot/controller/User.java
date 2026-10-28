package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispringboot.DTO.command.UserLoginCommandDTO;
import org.example.aispringboot.DTO.command.UserRegisterCommandDTO;
import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.service.UserService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class User {
	@Resource
	private UserService userService;

	//用户登录接口
	@PostMapping("/login")
	public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO commandDTO) {

		UserLoginResponseDTO result = userService.login(commandDTO);
		System.out.println(result);
		return Result.ok(result);
	}

	//用户注册接口
	@PostMapping("/add")
	public Result<UserLoginResponseDTO.UserDetailResponseDTO> register(@Valid @RequestBody UserRegisterCommandDTO commandDTO) {
		UserLoginResponseDTO.UserDetailResponseDTO result = userService.register(commandDTO);
		return Result.ok(result);
	}

	//获取当前用户
	@GetMapping("/current")
	public Result<UserLoginResponseDTO.UserDetailResponseDTO> getCurrentUser() {
		//如何从token中解析出用户的id
		String token = JwtTokenUtil.getCurrentToken();
		DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
		Long userId = jwt.getClaim("userId").asLong();
	//调用service层获取用户详情
		UserLoginResponseDTO.UserDetailResponseDTO result = userService.getUserById(userId);
		return Result.ok(result);
	}
	// 用户登出
	@PostMapping("/logout")
	public Result<Void> logout() {
		// JWT 无状态，服务端无需额外操作；前端清除 token 后即完成登出
		return Result.ok();
	}
}
