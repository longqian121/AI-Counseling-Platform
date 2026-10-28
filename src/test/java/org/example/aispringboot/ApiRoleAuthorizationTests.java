package org.example.aispringboot;

import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.mapper.consultationSessionMapper;
import org.example.aispringboot.service.UserService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiRoleAuthorizationTests {
    private static final long USER_ID = 991L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private consultationSessionMapper sessionMapper;

    @BeforeEach
    void currentUserIsNormalUser() {
        when(userService.getUserById(USER_ID)).thenReturn(
                UserLoginResponseDTO.UserDetailResponseDTO.builder()
                        .id(USER_ID)
                        .username("test-user")
                        .userType(1)
                        .status(1)
                        .build());
    }

    @Test
    void currentDatabaseRoleOverridesOldTokenRole() throws Exception {
        String oldAdminToken = JwtTokenUtil.generateToken(USER_ID, "test-user", 2);

        mockMvc.perform(get("/api/admin/counselor/page").header("token", oldAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotCallDoctorOrKnowledgeWriteApi() throws Exception {
        String token = JwtTokenUtil.generateToken(USER_ID, "test-user", 1);

        mockMvc.perform(get("/api/doctor/appointments").header("token", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/knowledge/article").header("token", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void articleWriteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/knowledge/article"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotReadAnotherUsersMessages() throws Exception {
        when(sessionMapper.selectById(44L)).thenReturn(
                ConsultationSession.builder().id(44L).userId(992L).build());
        String token = JwtTokenUtil.generateToken(USER_ID, "test-user", 1);

        mockMvc.perform(get("/api/psychological-chat/sessions/44/messages").header("token", token))
                .andExpect(status().isForbidden());
    }
}
