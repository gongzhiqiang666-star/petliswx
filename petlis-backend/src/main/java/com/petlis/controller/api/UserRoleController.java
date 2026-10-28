package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserRoleController {
    private final AccessService access;

    @GetMapping("/roles")
    public Result<List<String>> roles() {
        return Result.success(access.current().roles().stream().map(Enum::name).sorted().toList());
    }
}
