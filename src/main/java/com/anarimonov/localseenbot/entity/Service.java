package com.anarimonov.localseenbot.entity;

import com.anarimonov.localseenbot.entity.abs.AbsEntity;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = "services")
public class Service {
	@Id
	private int id;
	private int min;
	private int max;
	private String name;
	private String category;
	private String url;
	private int cost;
	private String about;
}