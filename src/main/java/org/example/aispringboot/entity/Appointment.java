package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("appointment")
@Builder
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("counselor_id")
    private Long counselorId;

    @TableField("appointment_date")
    private LocalDate appointmentDate;

    @TableField("time_slot")
    private String timeSlot;

    private Integer status;

    private String remark;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField(value = "counselorName", exist = false)
    private String counselorName;

    @TableField(value = "counselorTitle", exist = false)
    private String counselorTitle;

    @TableField(value = "userNickname", exist = false)
    private String userNickname;
}
