
package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.domain.TicketFavoriteBean;
import com.collection.benefitsjourney.domain.TicketListingBean;
import com.collection.benefitsjourney.domain.UsersBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 用於回傳票券收藏資料及對應的票券刊登詳細資料給前端。
public class TicketFavoriteResponse {

    // 收藏資料
    private Integer ticketFavoriteId;
    private Integer ticketListingId;
    private LocalDateTime createdAt;

    // 票券刊登資料
    private String title;
    private String description;
    private String status;
    // 刊登最後修改時間，對應 ticket_listing.updated_at
    private LocalDateTime updatedAt;

    // 新增：票券刊登者會員名稱
    private Integer usersId;
    private String usersName;

    // 影城、電影及放映時間
    private String cinemaName;
    private String movieName;
    private LocalDateTime ticketShowtime;

    public static TicketFavoriteResponse from(TicketFavoriteBean bean) {

        TicketFavoriteResponse response = new TicketFavoriteResponse();

        // 1. 收藏本身的資料
        response.setTicketFavoriteId(bean.getTicketFavoriteId());
        response.setCreatedAt(bean.getCreatedAt());

        // 2. 取得對應的票券刊登
        TicketListingBean listing = bean.getTicketListingBean();

        if (listing != null) {

            response.setTicketListingId(listing.getTicketListingId());
            response.setTitle(listing.getTitle());
            response.setDescription(listing.getDescription());
            response.setStatus(listing.getStatus());

            // 使用刊登最後修改時間，不是收藏建立時間
            response.setUpdatedAt(listing.getUpdatedAt());

            // 保留完整放映日期及時間
            response.setTicketShowtime(listing.getTicketShowtime());

            // 新增：取得票券刊登者名稱
            UsersBean usersBean = listing.getUsersBean();

            if (usersBean != null) {
                response.setUsersId(usersBean.getUsersId());
                response.setUsersName(usersBean.getName());
            }

            // 3. 取得影城名稱
            CinemaBean cinema = listing.getCinemaBean();

            if (cinema != null) {
                response.setCinemaName(cinema.getCinemaName());
            }

            // 4. 取得電影名稱
            MovieBean movie = listing.getMovieBean();

            if (movie != null) {
                response.setMovieName(movie.getMovieName());
            }
        }
        return response;
    }
}