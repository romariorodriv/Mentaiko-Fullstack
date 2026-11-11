package com.mentaiko.backend.service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class ActivityPreferenceService {
    public static final List<String> TYPES = List.of(
       "RESPIRACION","MOVIMIENTO","RELAJACION","ESCRITURA","ATENCION_PLENA","GENERAL");
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    public record Preferences(List<String> selected, List<String> available) {}
    public ActivityPreferenceService(JdbcTemplate jdbc,UserRepository users){
        this.jdbc=jdbc;this.users=users;
    }
    private Long userId(String email,boolean lock){
        var user=lock ? users.findLockedByEmail(email):users.findByEmailIgnoreCase(email);
        return user.filter(u->u.isActive()).orElseThrow(
           ()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sesión no válida")).getId();
    }
    @Transactional(readOnly=true)
    public Preferences get(String email){
        var selected=jdbc.query(
          "SELECT activity_type FROM activity_preferences WHERE user_id=? ORDER BY activity_type",
          (rs,i)->rs.getString(1),userId(email,false));
        return new Preferences(selected,TYPES);
    }
    @Transactional
    public Preferences update(String email,List<String> selected){
        if(selected==null || selected.size()>TYPES.size()
           || !TYPES.containsAll(selected) || new HashSet<>(selected).size()!=selected.size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipos de actividad no válidos");
        Long user=userId(email,true);
        jdbc.update("DELETE FROM activity_preferences WHERE user_id=?",user);
        for(String type:selected)
            jdbc.update("INSERT INTO activity_preferences(user_id,activity_type) VALUES(?,?)",user,type);
        return get(email);
    }
}
