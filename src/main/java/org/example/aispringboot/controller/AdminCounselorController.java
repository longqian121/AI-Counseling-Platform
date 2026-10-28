package org.example.aispringboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.DTO.command.CounselorCreateCommandDTO;
import org.example.aispringboot.entity.Counselor;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.mapper.CounselorMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/counselor")
public class AdminCounselorController {

    @Resource
    private CounselorMapper counselorMapper;

    @Resource
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/page")
    public Result<IPage<Counselor>> page(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<Counselor> page = new Page<>(currentPage, size);
        LambdaQueryWrapper<Counselor> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Counselor::getName, name);
        }
        if (status != null) {
            wrapper.eq(Counselor::getStatus, status);
        }
        wrapper.orderByDesc(Counselor::getCreatedAt);
        IPage<Counselor> result = counselorMapper.selectPage(page, wrapper);
        return Result.ok(result);
    }

    @PostMapping
    public Result<Counselor> create(@RequestBody CounselorCreateCommandDTO dto) {
        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            return Result.error("400", "用户名和密码不能为空", null);
        }
        // check username uniqueness
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            return Result.error("400", "用户名已存在", null);
        }
        // create user
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getName());
        user.setUserType(3);
        user.setStatus(1);
        userMapper.insert(user);
        // create counselor linked to user
        Counselor counselor = new Counselor();
        counselor.setUserId(user.getId());
        counselor.setName(dto.getName());
        counselor.setTitle(dto.getTitle());
        counselor.setSpecialty(dto.getSpecialty());
        counselor.setAvatar(dto.getAvatar());
        counselor.setDescription(dto.getDescription());
        counselor.setStatus(1);
        counselorMapper.insert(counselor);
        return Result.ok(counselor);
    }

    @PutMapping("/{id}")
    public Result<Counselor> update(@PathVariable Long id, @RequestBody Counselor counselor) {
        counselor.setId(id);
        counselorMapper.updateById(counselor);
        return Result.ok(counselor);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        counselorMapper.deleteById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Counselor counselor = counselorMapper.selectById(id);
        if (counselor != null) {
            counselor.setStatus(status);
            counselorMapper.updateById(counselor);
        }
        return Result.ok();
    }
}
