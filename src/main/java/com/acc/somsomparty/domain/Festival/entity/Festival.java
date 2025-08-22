package com.acc.somsomparty.domain.Festival.entity;

import com.acc.somsomparty.domain.Ticket.entity.Ticket;
import com.acc.somsomparty.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "festival", indexes = {
        @Index(name = "idx_name_lower", columnList = "name_lower"),
        @Index(name = "idx_description_lower", columnList = "description_lower")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Festival extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "name_lower", nullable = false, length = 20)
    private String nameLower;

    @Column(name = "description_lower", nullable = false, length = 255)
    private String descriptionLower;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @OneToMany(mappedBy = "festival", cascade = CascadeType.ALL)
    private List<Ticket> ticketList = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void setLowercaseValues() {
        this.nameLower = this.name.toLowerCase();
        this.descriptionLower = this.description.toLowerCase();
    }

    public Festival(String name, String description, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
