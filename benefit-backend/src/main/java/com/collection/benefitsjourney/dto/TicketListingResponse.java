package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.TicketListingBean;
import com.collection.benefitsjourney.domain.UsersBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketListingResponse {
    private Integer ticketListingId;
    private Integer usersId;

    // 刊登者會員名稱
    private String usersName;

    private Integer cinemaId;
    // 影城名稱
    private String cinemaName;
    private Integer movieId;
    // 電影名稱
    private String movieName;
    private String title;
    private String description;
    private String status;
    private LocalDateTime ticketShowtime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketListingResponse from(TicketListingBean bean) {

        TicketListingResponse response = new TicketListingResponse();

        response.setTicketListingId(bean.getTicketListingId());

        // 取得刊登者會員資料
        UsersBean usersBean = bean.getUsersBean();

        if (usersBean != null) {
            response.setUsersId(usersBean.getUsersId());
            response.setUsersName(usersBean.getName());
        }

        // 取得影城 ID 與名稱
        if (bean.getCinemaBean() != null) {
            response.setCinemaId(bean.getCinemaBean().getCinemaId());
            response.setCinemaName(bean.getCinemaBean().getCinemaName());
        }

        // 取得電影 ID 與名稱
        if (bean.getMovieBean() != null) {
            response.setMovieId(bean.getMovieBean().getMovieId());
            response.setMovieName(bean.getMovieBean().getMovieName());
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
