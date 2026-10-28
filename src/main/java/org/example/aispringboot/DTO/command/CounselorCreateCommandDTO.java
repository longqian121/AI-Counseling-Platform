package org.example.aispringboot.DTO.command;

import lombok.Data;

@Data
public class CounselorCreateCommandDTO {
    private String username;
    private String password;
    private String name;
    private String title;
    private String specialty;
    private String avatar;
    private String description;
}
