package com.collection.benefitsjourney.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "cinema")
public class CinemaBean {
    @Id
    @Column(name = "cinema_id")
    private Integer cinemaId;
}
