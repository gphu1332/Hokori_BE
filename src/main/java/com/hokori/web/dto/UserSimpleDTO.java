package com.hokori.web.dto;
import com.hokori.web.entity.User;
import lombok.Data;

@Data
public class UserSimpleDTO {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String roleName;  // chỉ trả tên role

    public static UserSimpleDTO from(User u) {
        UserSimpleDTO d = new UserSimpleDTO();
        d.setId(u.getId());
        d.setUsername(u.getUsername());
        d.setEmail(u.getEmail());
        d.setDisplayName(u.getDisplayName());
        d.setRoleName(u.getRole() != null ? u.getRole().getRoleName() : null);
        return d;
    }
}
