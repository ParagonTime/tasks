package org.pt.test.tasks.mapper;

import org.pt.test.tasks.dto.NewUserRequest;
import org.pt.test.tasks.dto.UserDto;
import org.pt.test.tasks.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toUserEntity(NewUserRequest userRequest) {
        User newUser = new User();
        newUser.setName(userRequest.getName());
        newUser.setEmail(userRequest.getEmail());
        return newUser;
    }

    public UserDto toUserDto(User newUser) {
        UserDto userDto = new UserDto();
        userDto.setId(newUser.getId());
        userDto.setName(newUser.getName());
        userDto.setEmail(newUser.getEmail());
        return userDto;
    }
}
