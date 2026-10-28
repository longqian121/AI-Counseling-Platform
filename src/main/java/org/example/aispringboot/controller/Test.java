package org.example.aispringboot.controller;

import org.example.aispringboot.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class Test {
	@GetMapping("/test")
	public Result<String> test(){
//		Result<T> result = new Result<>();
//		result.setCode(200);
//		result.setMsg("success");
		return Result.ok("hello world");
	}
}
