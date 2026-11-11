package com.mentaiko.backend.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.service.ActivityPreferenceService;

@RestController
@RequestMapping("/api/admin/micro-activities")
public class AdminActivityTypeController {
    private final MicroActivityRepository activities;
    public record Update(String activityType) {}
    public record Result(Long id,String activityType) {}
    public AdminActivityTypeController(MicroActivityRepository activities){this.activities=activities;}

    @PatchMapping("/{id}/type")
    @Transactional
    public Result update(@PathVariable Long id,@RequestBody Update request){
        if(request==null || !ActivityPreferenceService.TYPES.contains(request.activityType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo de actividad inválido");
        MicroActivity activity=activities.findById(id).orElseThrow(
          ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Microactividad no encontrada"));
        activity.setActivityType(request.activityType());
        activities.save(activity);
        return new Result(activity.getId(),activity.getActivityType());
    }
}
