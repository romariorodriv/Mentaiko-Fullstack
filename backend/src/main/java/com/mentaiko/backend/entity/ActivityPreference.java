package com.mentaiko.backend.entity;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.*;

@Entity
@Table(name = "activity_preferences")
@IdClass(ActivityPreference.Key.class)
public class ActivityPreference {
    @Id @Column(name="user_id",nullable=false)
    private Long userId;

    @Id @Column(name="activity_type",length=30,nullable=false)
    private String activityType;

    protected ActivityPreference() {}
    public ActivityPreference(Long userId,String activityType) {
        this.userId=userId;this.activityType=activityType;
    }
    public Long getUserId(){return userId;}
    public String getActivityType(){return activityType;}

    public static class Key implements Serializable {
        private Long userId;
        private String activityType;
        public Key(){}
        public Key(Long userId,String activityType){this.userId=userId;this.activityType=activityType;}
        @Override public boolean equals(Object object){
            if(this==object)return true;
            if(!(object instanceof Key other))return false;
            return Objects.equals(userId,other.userId) && Objects.equals(activityType,other.activityType);
        }
        @Override public int hashCode(){return Objects.hash(userId,activityType);}
    }
}
