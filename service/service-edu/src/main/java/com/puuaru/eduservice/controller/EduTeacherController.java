package com.puuaru.eduservice.controller;


import com.puuaru.eduservice.entity.EduTeacher;
import com.puuaru.eduservice.service.EduTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 讲师 前端控制器
 * </p>
 *
 * @author puuaru
 * @since 2022-11-17
 */
@RestController
@RequestMapping("/eduservice/teacher")
public class EduTeacherController {
    @Autowired
    EduTeacherService eduTeacherService;

    @GetMapping("/list")
    public List<EduTeacher> getTeacherList() {
        return eduTeacherService.list();
    }
}
