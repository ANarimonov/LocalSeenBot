package com.anarimonov.localseenbot.entity.abs;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@MappedSuperclass
public abstract class AbsEntity {
    @Id
    @GeneratedValue
    private int id;

    @OrderBy
    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Timestamp updatedAt;

    public AbsEntity(int id) {
        this.id = id;
    }
}
