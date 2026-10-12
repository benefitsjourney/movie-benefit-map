package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.TicketListingBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketListingResponse {
    private Integer ticketListingId;
    private Integer usersId;
    private Integer cinemaId;
    private Integer movieId;
    private String title;
    private String description;
    private String status;
    private LocalDateTime ticketShowtime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketListingResponse from(TicketListingBean bean) {

        TicketListingResponse response = new TicketListingResponse();

        response.setTicketListingId(bean.getTicketListingId());

        if (bean.getUsersBean() != null) {
            response.setUsersId(bean.getUsersBean().getUsersId());
        }

        if (bean.getCinemaBean() != null) {
            response.setCinemaId(bean.getCinemaBean().getCinemaId());
        }

        if (bean.getMovieBean() != null) {
            response.setMovieId(bean.getMovieBean().getMovieId());
        }

        response.setTitle(bean.getTitle());
        response.setDescription(bean.getDescription());
        response.setStatus(bean.getStatus());
        response.setTicketShowtime(bean.getTicketShowtime());
        response.setCreatedAt(bean.getCreatedAt());
        response.setUpdatedAt(bean.getUpdatedAt());

        return response;
    }
}
