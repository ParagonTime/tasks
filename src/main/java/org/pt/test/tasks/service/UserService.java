package org.pt.test.tasks.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pt.test.tasks.dto.NewUserRequest;
import org.pt.test.tasks.dto.UserDto;
import org.pt.test.tasks.entity.User;
import org.pt.test.tasks.exception.NoFoundException;
import org.pt.test.tasks.mapper.UserMapper;
import org.pt.test.tasks.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(NewUserRequest userRequest) {
        log.info("Creating user with name: {} email: {}", userRequest.getName(), userRequest.getEmail());
        User newUser = userRepository.save(userMapper.toUserEntity(userRequest));
        return userMapper.toUserDto(newUser);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        log.debug("Getting user by id: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> new NoFoundException("User no found id: " + id));
        return userMapper.toUserDto(user);
    }
}
