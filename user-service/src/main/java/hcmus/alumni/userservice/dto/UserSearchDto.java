package hcmus.alumni.userservice.dto;

import java.util.Set;

public interface UserSearchDto {

    String getId();
    String getFullName();
    String getEmail();
    String getAvatarUrl();
    Set<IRoleDto> getRoles();
    
}